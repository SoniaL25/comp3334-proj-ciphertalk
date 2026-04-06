import requests

BASE_URL = "http://localhost:8080"

def register(username, password):
    return requests.post(f"{BASE_URL}/register", json={
        "username": username,
        "password": password
    }).json()

def login(username, password):
    return requests.post(f"{BASE_URL}/login", json={
        "username": username,
        "password": password
    }).json()

def send_message(token, payload):
    return requests.post(f"{BASE_URL}/send_message",
        headers={"Authorization": token},
        json=payload
    ).json()

def get_messages(token):
    return requests.get(f"{BASE_URL}/messages",
        headers={"Authorization": token}
    ).json()