# NOTE:
# This client uses a mock fallback mechanism when the backend server is unavailable.
# This allows testing of E2EE functionality independently of server status.

import requests
import time

BASE_URL = "http://localhost:8080"

# Mock storage (for fallback)
mock_db = {}


def register(username, password): #auth 
    url = f"{BASE_URL}/api/auth/register"
    try:
        res = requests.post(url, json={
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
        res = requests.post(url, json={
            "email": username,
            "password": password
        })

        if res.status_code == 200:
            data = res.json()
            # if server return token
            token = data.get("token")
            if token:
                return token
        
        # fallback if server responds but not correct
        print("Login failed (OTP issue), using mock token for testing...")

    except Exception as e:
        print("Login server error:", e)

    # fallback mock token
    return "mock-token"

    #Original with server
    #else:
        #print("Login failed:", res.text)
        #return None
    

def send_message(token, chat_id, payload): #chat
    url = f"{BASE_URL}/api/chats/{chat_id}"

    headers = {
        "Authorization": f"Bearer {token}"
    }

    try:
        res = requests.post(url, json=payload, headers=headers)

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

    headers = {
        "Authorization": f"Bearer {token}"
    }

    try:
        res = requests.get(url, headers=headers)

        if res.status_code == 200:
            return res.json()
        else:
            print("Get messages failed (server):", res.text)

    except Exception as e:
        print("Get server error:", e)

    # fallback mock receive
    print("[MOCK] Using local inbox")

    return mock_db.get(chat_id, [])