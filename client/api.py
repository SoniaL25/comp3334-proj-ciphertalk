# NOTE:
# This client uses a mock fallback mechanism when the backend server is unavailable.
# This allows testing of E2EE functionality independently of server status.

import requests
import time
from urllib.parse import quote
from urllib.parse import urlparse

BASE_URL = "https://yoshie-kathartic-turbidimetrically.ngrok-free.dev" #"http://localhost:8080"
SESSION_COOKIE_NAME = "CTIM_SESSION"

_session = requests.Session()
_session_host = urlparse(BASE_URL).hostname

# Mock storage (for fallback)
mock_db = {}
mock_friend_requests = {
    "test_user": [
        {"id": "1", "fromUser": "bob"},
        {"id": "2", "fromUser": "charlie"}
    ]
}
mock_friends = {
    "test_user": ["bob"]
}


def register(username, password): #auth 
    url = f"{BASE_URL}/api/auth/register"
    try:
        res = _session.post(url, json={
            "email": username,
            "password": password
        })
        return res.json()
    except Exception as e:
        print("Register server error:", e)

    # fallback mock
    print("[MOCK] Register success")
    return {"status": "mock_registered"}


def login(username, password):
    url = f"{BASE_URL}/api/auth/login"

    try:
        res = _session.post(url, json={
            "email": username,
            "password": password
        })
        # sonia for testing
        print("Login response status:", res.status_code)

        if res.status_code == 200:
            print("[SERVER] OTP sent")
            session_id = get_session_id()
            return {"status": "otp_required", "session_id": session_id}

    except Exception as e:
        print("Login server error:", e)

    # fallback
    print("[MOCK] Login success (skip OTP)")
    return {"status": "mock_success", "token": "mock-token"}

    #Original with server
    #else:
        #print("Login failed:", res.text)
        #return None


def verify_otp(email, otp):
    url = f"{BASE_URL}/api/auth/verify-otp"

    try:
        res = _session.post(url, json={
            "email": email,
            "otp": otp
        })

        if res.status_code == 200:
            print("[SERVER] OTP verified")
            session_id = get_session_id()
            return session_id or True

    except Exception as e:
        print("Verify error:", e)

    # fallback
    print("[MOCK] OTP bypass")
    return "mock-token"


def get_session_id():
    return _session.cookies.get(SESSION_COOKIE_NAME)


def upload_public_key(public_key_pem):
    url = f"{BASE_URL}/api/profile/public-key"

    try:
        print("Upload cookie:", get_session_id())
        res = _session.put(url, json={
            "publicKey": public_key_pem
        })

        if res.status_code == 200:
            return res.json()

        print("Upload public key failed (server):", res.status_code, res.text)

    except Exception as e:
        print("Upload public key error:", repr(e))

    return None


def get_public_key_by_email(email):
    encoded_email = quote(email, safe="")
    url = f"{BASE_URL}/api/users/email/{encoded_email}/public-key"

    try:
        res = _session.get(url)

        if res.status_code == 200:
            payload = res.json()
            return payload.get("data", payload)

        print("Get public key failed (server):", res.text)

    except Exception as e:
        print("Get public key error:", e)

    return None


def send_friend_request(token, to_user):
    url = f"{BASE_URL}/api/requests/send"

    try:
        res = _session.post(url, json={
            "receiverEmail": to_user
        })

        if res.status_code == 200:
            print("[Friend Request Sent - SERVER]")
            return res.json()

        else:
            print("Send request failed (server):", res.text)

    except Exception as e:
        print("Server error:", e)

    # FALLBACK
    print("[MOCK] Friend request sent locally")

    new_request = {
    "id": str(len(mock_friend_requests.get(to_user, [])) + 1),
    "fromUser": "mock_user"
    }

    mock_friend_requests.setdefault(to_user, []).append(new_request)

    return {"status": "mock_request_sent"}


def accept_friend_request(token, request_id, current_user):
    url = f"{BASE_URL}/api/requests/{request_id}/respond"

    try:
        res = _session.put(url, json={
            "action": "ACCEPT"
        })

        if res.status_code == 200:
            print("[Friend Accepted - SERVER]")
            return res.json()

    except Exception as e:
        print("Server error:", e)

    # FALLBACK
    print("[MOCK] Accept locally")

    user = current_user

    req_list = mock_friend_requests.get(user, [])
    from_user = None

    # find first
    for r in req_list:
        if str(r["id"]) == str(request_id):
            from_user = r["fromUser"]
            break

    # remove after found
    mock_friend_requests[user] = [
        r for r in req_list if str(r["id"]) != str(request_id)
    ]

    # update friend list
    if from_user:
        mock_friends.setdefault(user, []).append(from_user)
        mock_friends.setdefault(from_user, []).append(user)
        print(f"[Accepted] {from_user} is now your friend")
    else:
        print("Request not found")

    return {"status": "mock_accepted"}

def get_incoming_requests(token, current_user):
    url = f"{BASE_URL}/api/requests/incoming"

    try:
        res = _session.get(url)

        if res.status_code == 200:
            print("[SERVER] Incoming requests fetched")
            payload = res.json()
            print("Payload:", payload)
            return payload.get("data", payload)

    except Exception as e:
        print("Server error:", e)

    # fallback
    print("[MOCK] Using local incoming requests")

    # mock structure
    return mock_friend_requests.get(current_user, [])


def are_friends(user1, user2): # check if friends
    return user2 in mock_friends.get(user1, [])
    

def send_message(token, chat_id, payload): #chat
    url = f"{BASE_URL}/api/chats/{chat_id}"

    try:
        res = _session.post(url, json=payload)

        if res.status_code == 200:
            return res.json()
        else:
            print("Send failed (server):", res.text)

    except Exception as e:
        print("Send server error:", e)

    # fallback mock send
    print("[MOCK] Using local send fallback")

    if chat_id not in mock_db:
        mock_db[chat_id] = []

    # store message locally
    mock_db[chat_id].append({
        "content": payload.get("content"),
        "nonce": payload.get("nonce"),
        "timestamp": int(time.time())
    })

    return {
        "status": "mock_sent",
        "chat_id": chat_id,
        "content": payload.get("content")
    }


def get_messages(token, chat_id):
    url = f"{BASE_URL}/api/chats/{chat_id}"

    try:
        res = _session.get(url)

        if res.status_code == 200:
            payload = res.json()
            return payload.get("data", payload)
        else:
            print("Get messages failed (server):", res.text)

    except Exception as e:
        print("Get server error:", e)

    # fallback mock receive
    print("[MOCK] Using local inbox")

    return mock_db.get(chat_id, [])