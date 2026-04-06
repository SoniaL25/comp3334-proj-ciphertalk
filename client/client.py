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

def show_fingerprint(): # Identity / Fingerprint
    fp = crypto.generate_fingerprint(identity_public)
    print(f"[Key] Your fingerprint:\n{fp}")

def do_register(): # Auth
    u = input("Username: ")
    p = input("Password: ")
    res = api.register(u, p)
    print(res)

def do_login():
    global token, username
    u = input("Username: ")
    p = input("Password: ")
    res = api.login(u, p)
    token = res.get("token")
    username = u
    print("Logged in Completed")

def setup_shared_key(): # Key Exchange
    global shared_key

    print("Setting up secure session...")

    params = crypto.generate_dh_parameters()
    priv, pub = crypto.generate_dh_keypair(params)

    # TEMP demo（can change server exchange）
    shared_key = crypto.compute_shared_secret(priv, pub)

    print("Secure session established Completly")

def send_message(): # Send Message
    global shared_key

    if not shared_key:
        print(" No shared key")
        return

    receiver = input("To: ")
    msg = input("Message: ")

    if not msg.strip():
        print("Empty message not allowed")
        return
    
    ttl = int(input("TTL seconds (0 = no self-destruct): "))

    message_id = crypto.generate_message_id()
    timestamp = int(time.time())

    sender_name = username if username else "You"
    associated_data = f"{message_id}|{sender_name}|{receiver}|{timestamp}".encode()

    nonce, ciphertext = crypto.encrypt_message(
        shared_key,
        msg,
        associated_data
    )

    payload = {
        "message_id": message_id,
        "sender": username if username else "You",
        "receiver": receiver,
        "timestamp": timestamp,
        "ttl": ttl,
        "nonce": encode_bytes(nonce),
        "ciphertext": encode_bytes(ciphertext)
    }

    #res = api.send_message(token, payload)
    #print(" Sent:", res)

    # Test for fake server response
    print("\n[Sent] (simulated)")
    global last_sent_payload
    last_sent_payload = payload

def receive_messages(): # Receive Message
    global shared_key

    #res = api.get_messages(token)

    # Test for fake server response
    global last_sent_payload
    res = [last_sent_payload] if 'last_sent_payload' in globals() else []

    for m in res:
        message_id = m["message_id"]

        # replay protection
        if crypto.is_replay(message_id):
            print("Replay attack detected!")
            continue

        nonce = decode_bytes(m["nonce"])
        ciphertext = decode_bytes(m["ciphertext"])

        associated_data = f"{m['message_id']}|{m['sender']}|{m['receiver']}|{m['timestamp']}".encode()

        try:
            plaintext = crypto.decrypt_message(
                shared_key,
                nonce,
                ciphertext,
                associated_data
            )
        except:
            print("Decryption failed (tampering?)")
            continue

        now = int(time.time())

        expiry = m["timestamp"] + m["ttl"] if m["ttl"] > 0 else None

        if m["ttl"] > 0 and now > m["timestamp"] + m["ttl"]:
            print("[Expired] Message expired")
            continue

        local_messages[message_id] = (plaintext, expiry)

        sender_name = m['sender'] if m['sender'] else "You"
        print(f"\n[Delivered] Message received")
        print(f"\n[Message] From {sender_name}: {plaintext}")

def cleanup_messages(): # Self-destruct cleaner
    now = int(time.time())
    to_delete = []

    for mid, (msg, expiry) in local_messages.items():
        if expiry and now > expiry:
            to_delete.append(mid)

    for mid in to_delete:
        del local_messages[mid]
        print(f"[Expired] Message {mid} expired")


def show_inbox(): # Inbox UI
    print("\n[Inbox]:")

    if not local_messages:
        print("(No messages)")
        return
    
    for mid, (msg, expiry) in local_messages.items():
        print(f"From message: {msg}")
    
    
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