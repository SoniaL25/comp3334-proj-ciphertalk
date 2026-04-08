from crypto_module import crypto_module as crypto
from client import api
import base64
import os
import time
import re
from cryptography.hazmat.primitives.asymmetric import dh

KEY_DIR = os.path.join(os.path.dirname(__file__), ".keys")
current_user_email = None
current_private_key = None
current_public_key = None
current_public_pem = None
shared_key = None
shared_keys = {}
active_peer_email = None
token = None
username = None

local_messages = {}  # message_id -> (msg, expiry)

def init_mock_data(user):
    if user not in api.mock_friend_requests:
        api.mock_friend_requests[user] = [
            {"id": "1", "fromUser": "bob"},
            {"id": "2", "fromUser": "charlie"}
        ]

    if user not in api.mock_friends:
        api.mock_friends[user] = []

    print(f"[MOCK INIT] Data loaded for {user}")


def encode_bytes(b): # Utils
    return base64.b64encode(b).decode()


def decode_bytes(s):
    return base64.b64decode(s.encode())


def ensure_key_dir():
    os.makedirs(KEY_DIR, exist_ok=True)


def sanitize_key_name(email):
    return re.sub(r"[^A-Za-z0-9._-]", "_", email.strip().lower())


def get_key_paths(email):
    safe_name = sanitize_key_name(email)
    private_path = os.path.join(KEY_DIR, f"{safe_name}_private.pem")
    public_path = os.path.join(KEY_DIR, f"{safe_name}_public.pem")
    return private_path, public_path


def set_current_keypair(email, private_key, public_key):
    global current_user_email, current_private_key, current_public_key, current_public_pem
    current_user_email = email
    current_private_key = private_key
    current_public_key = public_key
    current_public_pem = crypto.serialize_public_key(public_key)


def load_or_create_user_keypair(email):
    ensure_key_dir()
    private_path, public_path = get_key_paths(email)

    if os.path.exists(private_path):
        with open(private_path, "r", encoding="utf-8") as f:
            private_pem = f.read()
        try:
            private_key = crypto.load_private_key(private_pem)
        except Exception:
            private_key = None

        # Old RSA key files cannot derive shared secrets with .exchange(); regenerate.
        if private_key is not None and not hasattr(private_key, "exchange"):
            private_key = None

        if private_key is not None:
            public_key = private_key.public_key()

            if not os.path.exists(public_path):
                with open(public_path, "w", encoding="utf-8") as f:
                    f.write(crypto.serialize_public_key(public_key))

            set_current_keypair(email, private_key, public_key)
            print(f"[Key] Loaded persistent keypair for {email}")
            return private_key, public_key

        print(f"[Key] Existing keypair for {email} is incompatible; regenerating")

    session_parameters = crypto.generate_dh_parameters()
    private_key, public_key = crypto.generate_dh_keypair(session_parameters)
    with open(private_path, "w", encoding="utf-8") as f:
        f.write(crypto.serialize_private_key(private_key))
    with open(public_path, "w", encoding="utf-8") as f:
        f.write(crypto.serialize_public_key(public_key))

    set_current_keypair(email, private_key, public_key)
    print(f"[Key] Generated and saved new persistent keypair for {email}")
    return private_key, public_key


def show_fingerprint(): # Identity
    if current_public_key is None:
        print("Please login first")
        return

    fp = crypto.generate_fingerprint(current_public_key)
    print(f"[Key] Your fingerprint:\n{fp}")


def upload_my_public_key():
    if not token:
        return False

    if current_public_pem is None:
        print("[WARN] No local public key loaded")
        return False

    res = api.upload_public_key(current_public_pem)
    if res is None:
        print("[WARN] Failed to upload public key")
        return False

    print("[Key] Public key uploaded to server")
    return True


def ensure_my_public_key_synced():
    if not token:
        return False

    if current_public_pem is None:
        print("[WARN] No local public key loaded")
        return False

    current = api.get_my_public_key()
    server_pem = None
    if isinstance(current, dict):
        server_pem = current.get("publicKey")
    elif isinstance(current, str):
        server_pem = current

    if server_pem and server_pem.strip() == current_public_pem.strip():
        return True

    print("[Key] Syncing local public key to server")
    return upload_my_public_key()


def establish_shared_key(peer_email):
    global shared_key, active_peer_email

    if current_private_key is None or current_public_key is None:
        print("Please login first")
        return None

    peer_info = api.get_public_key_by_email(peer_email)
    if not peer_info:
        print(f"[Key] Could not fetch public key for {peer_email}")
        return None

    peer_public_pem = peer_info.get("publicKey") if isinstance(peer_info, dict) else peer_info
    if not peer_public_pem:
        print(f"[Key] Invalid peer public key for {peer_email}")
        return None

    try:
        peer_public_key = crypto.load_public_key(peer_public_pem)
        if not isinstance(peer_public_key, dh.DHPublicKey):
            print(
                f"[Key] {peer_email} has a non-DH public key on the server. "
                "They need to log in with the updated client once so their DH public key is uploaded."
            )
            return None
        salt = crypto.derive_pair_salt(current_public_key, peer_public_key)
        derived_key = crypto.compute_shared_secret(current_private_key, peer_public_key, salt=salt)
    except Exception as ex:
        print(f"[Key] Shared key exchange failed with {peer_email}: {ex}")
        return None

    shared_keys[peer_email] = derived_key
    shared_key = derived_key
    active_peer_email = peer_email
    print(f"[Key] Shared key established with {peer_email}")
    return derived_key


def build_associated_data(sender_email, receiver_email, chat_id, client_message_id, tag):
    return f"{sender_email}|{receiver_email}|{chat_id}|{client_message_id}|{tag}".encode()


def decrypt_with_peer_retry(peer_email, sender_email, receiver_email, chat_id, message_id, tag, nonce, ciphertext):
    key = shared_keys.get(peer_email)
    if key:
        try:
            associated_data = build_associated_data(sender_email, receiver_email, chat_id, message_id, tag)
            return crypto.decrypt_message(key, nonce, ciphertext, associated_data)
        except Exception:
            pass

    # One refresh attempt in case peer rotated/re-uploaded key.
    refreshed_key = establish_shared_key(peer_email)
    if not refreshed_key:
        raise ValueError("Could not refresh shared key")

    associated_data = build_associated_data(sender_email, receiver_email, chat_id, message_id, tag)
    return crypto.decrypt_message(refreshed_key, nonce, ciphertext, associated_data)


def do_register(): # Auth
    u = input("Email: ")
    p = input("Password: ")
    res = api.register(u, p)
    if isinstance(res, dict) and res.get("status") != "mock_registered":
        load_or_create_user_keypair(u)
    print(res)


def do_login():
    global token, username

    u = input("Email: ")
    p = input("Password: ")

    res = api.login(u, p)

    if res.get("status") == "mock_success":
        token = res.get("token")
        username = u

        load_or_create_user_keypair(username)

        init_mock_data(username) 

        print("Login success (mock)")
        return

    if res.get("status") == "otp_required":
        print("OTP sent to your email")

        otp = input("Enter OTP: ")

        token_res = api.verify_otp(u, otp)

        if token_res:
            token = token_res
            username = u

            load_or_create_user_keypair(username)

            init_mock_data(username) 

            ensure_my_public_key_synced()

            print("Login success")
        else:
            print("Login failed")
            return

    else:
        print("Login failed")
        return


def setup_shared_key(): # Key Exchange
    peer_email = input("Peer email: ").strip()

    if not token:
        print("Please login first")
        return

    if not ensure_my_public_key_synced():
        return

    if establish_shared_key(peer_email):
        print("Secure session established")


def send_friend_request():
    global token

    if not token:
        print("Please login first")
        return

    to_user = input("Enter username to add: ")
    api.send_friend_request(token, to_user)


def accept_friend_request():
    global token, username

    incoming_requests = api.get_incoming_requests(token, username)

    if not incoming_requests:
        print("No incoming requests")
        return

    print("\n[Incoming Requests]:")
    for r in incoming_requests:
        if r['status'] == 'PENDING':
            print(f"ID: {r['id']} from {r['senderEmail']}")

    req_id = input("Enter request ID to accept: ").strip()

    res = api.accept_friend_request(token, req_id, username)

    if res:
        print("[Accepted] Friend request accepted")
    else:
        print("[Error] Accept failed")
        return

    # refresh
    updated_requests = api.get_incoming_requests(token, username)

    if not updated_requests:
        print("\n[Updated] No more incoming requests")
    else:
        print("\n[Updated Incoming Requests]:")
        for r in updated_requests:
            if r['status'] == 'PENDING':
                print(f"ID: {r['id']} from {r['senderEmail']}")


def show_friends():
    if not token:
        print("Please login first")
        return

    chats = api.get_friend_chats(token)

    print("\n[Friend Chats]:")
    if not chats:
        print("(No friend chats)")
        return

    chat_lookup = {}
    for chat in chats:
        chat_id = str(chat.get("chatId") or "")
        friend_email = chat.get("friendEmail") or "(unknown)"
        unread_count = chat.get("numOfUnreadMessage") or 0
        last_time = chat.get("lastMessageDateTime") or "-"

        if not chat_id:
            continue

        chat_lookup[chat_id] = chat
        print(f"- Chat ID: {chat_id} | Friend: {friend_email} | Unread: {unread_count} | Last: {last_time}")

    if not chat_lookup:
        print("(No valid chat IDs)")
        return

    selected_chat_id = input("Enter friend chat ID to fetch unread messages (blank to cancel): ").strip()
    if not selected_chat_id:
        return

    if selected_chat_id not in chat_lookup:
        print("Invalid chat ID")
        return

    peer_email = chat_lookup[selected_chat_id].get("friendEmail")
    if peer_email:
        establish_shared_key(peer_email)

    chat_messages = api.get_messages(token, selected_chat_id)

    if not chat_messages:
        print("(No unread messages in this chat)")
        return

    print(f"\n[Unread Messages in Chat {selected_chat_id}]:")
    for m in chat_messages:
        content = m.get("content")
        nonce_value = m.get("nonce")
        message_id = m.get("clientMessageId") or str(hash(str(m)))
        tag = m.get("tag") or ""

        if not content or not nonce_value:
            continue

        try:
            if peer_email and peer_email in shared_keys:
                ciphertext = decode_bytes(content)
                nonce = decode_bytes(nonce_value)
                plaintext = decrypt_with_peer_retry(
                    peer_email,
                    peer_email,
                    username,
                    selected_chat_id,
                    message_id,
                    tag,
                    nonce,
                    ciphertext,
                )
                print(f"- [{m.get('sentAt')}] {peer_email}: {plaintext}")
            else:
                print(f"- [{m.get('sentAt')}] (encrypted) content={content}")
        except Exception:
            print(f"- [{m.get('sentAt')}] (failed to decrypt) content={content}")

def send_message(): # Send Message 
    global shared_key

    if not token:
        print("Please login first")
        return

    if not ensure_my_public_key_synced():
        print("Key sync failed; cannot send securely")
        return
    
    receiver = input("Send to (username/email): ").strip()
    
    if not api.are_friends(username, receiver):
        print("You must be friends before sending messages")
        return

    if not establish_shared_key(receiver):
        return

    shared_key = shared_keys.get(receiver)

    if not shared_key:
        print("No shared key")
        return

    chat_id = input("Chat ID: ")
    msg = input("Message: ")

    if not msg.strip():
        print("Empty message not allowed")
        return

    ttl_seconds = int(input("TTL seconds (0 = no self-destruct): "))
    ttl_minutes = max(0, (ttl_seconds + 59) // 60)

    message_id = crypto.generate_message_id()
    tag = crypto.generate_message_id()

    associated_data = build_associated_data(username, receiver, chat_id, message_id, tag)

    nonce, ciphertext = crypto.encrypt_message(shared_key, msg, associated_data)

    # encode encrypted message into content
    encrypted_content = encode_bytes(ciphertext)

    payload = {
        "content": encrypted_content,
        "nonce": encode_bytes(nonce),
        "clientMessageId": message_id,
        "tag": tag,
        "ttlMinutes": ttl_minutes
    }

    res = api.send_message(token, chat_id, payload)

    if res:
        print("[Sent]")
    else:
        print("Send failed")


def receive_messages(): # Receive Message
    if not token:
        print("Please login first")
        return

    peer_email = input("Peer email: ").strip()
    chat_id = input("Chat ID: ")

    if not establish_shared_key(peer_email):
        return

    res = api.get_messages(token, chat_id)

    for m in res:
        try:
            content = m.get("content")
            if not content:
                continue

            ciphertext = decode_bytes(content)

            nonce_value = m.get("nonce")
            if not nonce_value:
                continue

            nonce = decode_bytes(nonce_value)

            message_id = m.get("clientMessageId") or str(hash(content))

            if crypto.is_replay(message_id):
                print("Replay attack detected!")
                continue

            tag = m.get("tag") or ""
            plaintext = decrypt_with_peer_retry(
                peer_email,
                peer_email,
                username,
                chat_id,
                message_id,
                tag,
                nonce,
                ciphertext,
            )

            expiry = None  # server do not suport TTL

            local_messages[message_id] = (plaintext, expiry)

            print(f"\n[Delivered]")
            print(f"[Message] {plaintext}")

        except:
            print("Decryption failed")


def cleanup_messages(): # Cleanup
    now = int(time.time())
    to_delete = []

    for mid, (msg, expiry) in local_messages.items():
        if expiry and now > expiry:
            to_delete.append(mid)

    for mid in to_delete:
        del local_messages[mid]
        print(f"[Expired] Message {mid} expired")


def show_inbox():
    if not token:
        print("Please login first")
        return

    messages = api.get_undelivered_messages(token)

    if not messages:
        print("(No undelivered messages)")
        return

    print("\n[Undelivered Inbox Messages]:")
    for m in messages:
        chat_id = str(m.get("chatId") or "")
        sender_id = str(m.get("senderId") or "")
        sender_email = "(unknown)"

        if sender_id:
            sender_info = api.get_user_by_id(sender_id)
            if sender_info and sender_info.get("email"):
                sender_email = sender_info.get("email")

        content = m.get("content")
        nonce_value = m.get("nonce")
        message_id = m.get("clientMessageId") or str(hash(str(m)))
        tag = m.get("tag") or ""

        rendered = content
        if sender_email != "(unknown)" and content and nonce_value and chat_id:
            try:
                ciphertext = decode_bytes(content)
                nonce = decode_bytes(nonce_value)
                rendered = decrypt_with_peer_retry(
                    sender_email,
                    sender_email,
                    username,
                    chat_id,
                    message_id,
                    tag,
                    nonce,
                    ciphertext,
                )
            except Exception:
                rendered = "(failed to decrypt)"

        print(f"- Chat: {chat_id} | From: {sender_email} | Sent: {m.get('sentAt')} | Message: {rendered}")


def main(): # CLI

    while True:
        cleanup_messages()

        print("\n==== SECURE CHAT ====")
        print("1. Register")
        print("2. Login")
        print("3. Show Fingerprint")
        print("4. Setup Secure Session")
        print("5. Send Message")
        print("6. Receive Messages")
        print("7. Show Inbox")
        print("8. Send Friend Request")    
        print("9. Accept Friend Request")    
        print("10. Show Friends")           
        print("0. Exit")
        print("=====================")

        choice = input("Choose: ")
        print("=====================")

        if choice == "1":
            do_register()
        elif choice == "2":
            do_login()
        elif choice == "3":
            show_fingerprint()
        elif choice == "4":
            setup_shared_key()
        elif choice == "5":
            send_message()
        elif choice == "6":
            receive_messages()
        elif choice == "7":
            show_inbox()
        elif choice == "8":
            send_friend_request()     
        elif choice == "9":
            accept_friend_request()   
        elif choice == "10":
            show_friends()              
        elif choice == "0":
            break
        else:
            print("Invalid choice")


if __name__ == "__main__":
    main()