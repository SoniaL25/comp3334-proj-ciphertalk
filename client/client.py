from crypto_module import crypto_module as crypto
from client import api
import base64
import time

identity_private, identity_public = crypto.generate_identity_keypair()
session_parameters = crypto.generate_dh_parameters()
session_private, session_public = crypto.generate_dh_keypair(session_parameters)
session_public_pem = crypto.serialize_public_key(session_public)
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


def show_fingerprint(): # Identity
    fp = crypto.generate_fingerprint(identity_public)
    print(f"[Key] Your fingerprint:\n{fp}")


def upload_my_public_key():
    if not token:
        return False

    res = api.upload_public_key(session_public_pem)
    if res is None:
        print("[WARN] Failed to upload public key")
        return False

    print("[Key] Public key uploaded to server")
    return True


def establish_shared_key(peer_email):
    global shared_key, active_peer_email

    peer_info = api.get_public_key_by_email(peer_email)
    if not peer_info:
        print(f"[Key] Could not fetch public key for {peer_email}")
        return None

    peer_public_pem = peer_info.get("publicKey") if isinstance(peer_info, dict) else peer_info
    if not peer_public_pem:
        print(f"[Key] Invalid peer public key for {peer_email}")
        return None

    peer_public_key = crypto.load_public_key(peer_public_pem)
    salt = crypto.derive_pair_salt(session_public, peer_public_key)
    derived_key = crypto.compute_shared_secret(session_private, peer_public_key, salt=salt)

    shared_keys[peer_email] = derived_key
    shared_key = derived_key
    active_peer_email = peer_email
    print(f"[Key] Shared key established with {peer_email}")
    return derived_key


def build_associated_data(sender_email, receiver_email, chat_id, client_message_id, tag):
    return f"{sender_email}|{receiver_email}|{chat_id}|{client_message_id}|{tag}".encode()


def do_register(): # Auth
    u = input("Email: ")
    p = input("Password: ")
    res = api.register(u, p)
    print(res)


def do_login():
    global token, username

    u = input("Email: ")
    p = input("Password: ")

    res = api.login(u, p)

    if res.get("status") == "mock_success":
        token = res.get("token")
        username = u

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

            init_mock_data(username) 

            upload_my_public_key()

            print("Login success")
        else:
            print("Login failed")

    else:
        print("Login failed")


def setup_shared_key(): # Key Exchange
    peer_email = input("Peer email: ").strip()

    if not token:
        print("Please login first")
        return

    if not upload_my_public_key():
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
    global username

    friends = api.mock_friends.get(username, [])

    print("\n[Friends]:")
    if not friends:
        print("(No friends)")
    else:
        for f in friends:
            print(f"- {f}")

def send_message(): # Send Message 
    global shared_key

    if not token:
        print("Please login first")
        return
    
    receiver = input("Send to (username/email): ").strip()
    
    if not api.are_friends(username, receiver):
        print("You must be friends before sending messages")
        return

    if receiver not in shared_keys:
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

    if peer_email not in shared_keys:
        if not establish_shared_key(peer_email):
            return

    shared_key = shared_keys.get(peer_email)

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
            associated_data = build_associated_data(peer_email, username, chat_id, message_id, tag)

            plaintext = crypto.decrypt_message(
                shared_key,
                nonce,
                ciphertext,
                associated_data
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
    global local_messages

    messages = list(local_messages.values())

    if not messages:
        print("(No messages)")
        return

    page_size = 3
    page = 0

    while True:
        start = page * page_size
        end = start + page_size
        chunk = messages[start:end]

        if not chunk:
            print("(No more messages)")
            break

        print(f"\n[Inbox Page {page+1}]:")
        for msg, _ in chunk:
            print(f"[Message] {msg}")

        cmd = input("Press n for next page, q to quit: ")

        if cmd == "n":
            page += 1
        else:
            break


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