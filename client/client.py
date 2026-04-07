from crypto_module import crypto_module as crypto
from client import api
import base64
import time

identity_private, identity_public = crypto.generate_identity_keypair()
shared_key = None
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

            print("Login success")
        else:
            print("Login failed")

    else:
        print("Login failed")


def setup_shared_key(): # Key Exchange
    global shared_key

    print("Setting up secure session...")

    params = crypto.generate_dh_parameters()
    priv, pub = crypto.generate_dh_keypair(params)

    # demo shared key
    shared_key = crypto.compute_shared_secret(priv, pub)

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
        print(f"ID: {r['id']} from {r['fromUser']}")

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
            print(f"ID: {r['id']} from {r['fromUser']}")


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

    if not shared_key:
        print("No shared key")
        return

    if not token:
        print("Please login first")
        return
    
    receiver = input("Send to (username): ")
    
    if not api.are_friends(username, receiver):
        print("You must be friends before sending messages")
        return

    chat_id = input("Chat ID: ")
    msg = input("Message: ")

    if not msg.strip():
        print("Empty message not allowed")
        return

    ttl = int(input("TTL seconds (0 = no self-destruct): "))

    message_id = crypto.generate_message_id()
    timestamp = int(time.time())

    sender_name = username if username else "You"

    # demo mode
    associated_data = b""

    # original ver.
    #associated_data = f"{message_id}|{sender_name}|{chat_id}|{timestamp}".encode()

    """
    b'0' *12 part is only for demo, which let the system can run
    nonce = b'0' * 12
    _, ciphertext = crypto.encrypt_message(
        shared_key,
        msg,
        associated_data
    )

    # encode encrypted message into content
    encrypted_content = encode_bytes(ciphertext)

    payload = {
        "content": encrypted_content
    }
    """

    nonce, ciphertext = crypto.encrypt_message(
    shared_key,
    msg,
    associated_data
    )

    # encode encrypted message into content
    encrypted_content = encode_bytes(ciphertext)

    payload = {
        "content": encrypted_content,
        "nonce": encode_bytes(nonce)   # 🔥 加呢行
    }

    res = api.send_message(token, chat_id, payload)

    if res:
        print("[Sent]")
    else:
        print("Send failed")


def receive_messages(): # Receive Message
    global shared_key

    if not token:
        print("Please login first")
        return

    chat_id = input("Chat ID: ")

    res = api.get_messages(token, chat_id)

    for m in res:
        try:
            content = m.get("content")
            if not content:
                continue

            ciphertext = decode_bytes(content)

            # server don't have nonce → use dummy（demo）
            #nonce = b'0' * 12
            nonce = decode_bytes(m.get("nonce"))

            message_id = str(hash(content))

            if crypto.is_replay(message_id):
                print("Replay attack detected!")
                continue

            associated_data = b""  # server don't have metadata → simplified

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