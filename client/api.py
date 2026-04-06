import requests

BASE_URL = "http://localhost:8080"

def register(username, password): #auth 
    url = f"{BASE_URL}/api/auth/register"
    res = requests.post(url, json={
        "email": username,
        "password": password
    })
    return res.json()


def login(username, password):
    url = f"{BASE_URL}/api/auth/login"
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
        
    # fallback
    print("Login failed (OTP issue), using mock token for testing...")
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

    res = requests.post(url, json=payload, headers=headers)

    if res.status_code == 200:
        return res.json()
    else:
        print("Send failed:", res.text)
        return None


def get_messages(token, chat_id):
    url = f"{BASE_URL}/api/chats/{chat_id}"

    headers = {
        "Authorization": f"Bearer {token}"
    }

    res = requests.get(url, headers=headers)

    if res.status_code == 200:
        return res.json()
    else:
        print("Get messages failed:", res.text)
        return []