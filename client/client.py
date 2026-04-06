from crypto_module import crypto_module as crypto
from client import api
import base64
import time

identity_private, identity_public = crypto.generate_identity_keypair()
shared_key = None
token = None
username = None

local_messages = {}  # message_id -> (msg, expiry)

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

    token = api.login(u, p)
    if token:
        username = u
        print("Login success")
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


def send_message(): # Send Message 
    global shared_key

    if not shared_key:
        print("No shared key")
        return

    if not token:
        print("Please login first")
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


def show_inbox(): # Inbox
    print("\n[Inbox]:")

    if not local_messages:
        print("(No messages)")
        return

    for mid, (msg, expiry) in local_messages.items():
        print(f"[Message] {msg}")


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
        elif choice == "0":
            break


if __name__ == "__main__":
    main()