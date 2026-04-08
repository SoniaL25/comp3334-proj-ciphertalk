import os
import hashlib
import uuid
import bcrypt
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.asymmetric import dh
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat, load_pem_public_key


### Password hashing --- 

def hash_password(password):
    password_bytes = password.encode('utf-8') #string to bytes 
    salt = bcrypt.gensalt() # generate salt 
    hashed = bcrypt.hashpw(password_bytes, salt) 
    return hashed

def verify_password(password, hashed_password):
    password_bytes = password.encode('utf-8') # string to bytes 
    rehashed = bcrypt.hashpw(password_bytes, hashed_password)
    # Check if result matches stored hased password 
    if rehashed == hashed_password:
        return True
    else:
        return False
    

### Key generation ---

def generate_identity_keypair():
    # using RSA for key generation 
    # library handles the RSA steps internally (n = p*q, choosing e and d)
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048
    )

    public_key = private_key.public_key()
    
    return private_key, public_key



### Session establishment: Diffie-Hellman key exchange ---

def generate_dh_parameters():
    # p is a 2048-bit prime number
    # g is the generator, g^a mod p
    parameters = dh.generate_parameters(generator=2, key_size=2048)
    return parameters


def generate_dh_keypair(parameters):
    private_key = parameters.generate_private_key()
    public_key = private_key.public_key()  # public value is g^a mod p 
    return private_key, public_key


def compute_shared_secret(private_key, other_public_key, salt=None, info=b'handshake'):
    # Alice: B^a mod p = g^(ab) mod p
    # Bob: A^b mod p = g^(ab) mod p
    shared_secret = private_key.exchange(other_public_key)
    
    # HKDF turns the raw shared secret into proper symmetric key
    derived_key = HKDF(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        info=info
    ).derive(shared_secret)
    
    return derived_key


def serialize_public_key(public_key):
    public_key_bytes = public_key.public_bytes(
        encoding=Encoding.PEM,
        format=PublicFormat.SubjectPublicKeyInfo
    )
    return public_key_bytes.decode("utf-8")


def load_public_key(pem_public_key):
    if isinstance(pem_public_key, bytes):
        pem_public_key = pem_public_key.decode("utf-8")
    return load_pem_public_key(pem_public_key.encode("utf-8"))


def derive_pair_salt(public_key_a, public_key_b):
    first = public_key_a.public_bytes(
        encoding=Encoding.PEM,
        format=PublicFormat.SubjectPublicKeyInfo
    )
    second = public_key_b.public_bytes(
        encoding=Encoding.PEM,
        format=PublicFormat.SubjectPublicKeyInfo
    )

    ordered = sorted([first, second])
    return hashlib.sha256(ordered[0] + ordered[1]).digest()



### Message encryption/decryption ---

def encrypt_message(shared_key, plaintext, associated_data):
    nonce = os.urandom(12) # random nonce unique for each message, s.urandom is a CSPRNG
    aesgcm = AESGCM(shared_key) # creating AES-GCM cipher
    
    # encrypting the message
    plaintext_bytes = plaintext.encode('utf-8') # to bytes
    ciphertext = aesgcm.encrypt(nonce, plaintext_bytes, associated_data)
    
    return nonce, ciphertext # receiver also needs nonce to decrypt


def decrypt_message(shared_key, nonce, ciphertext, associated_data):
    # creating AES-GCM cipher with the shared key
    aesgcm = AESGCM(shared_key)
    
    # decrypting the message
    plaintext_bytes = aesgcm.decrypt(nonce, ciphertext, associated_data)
    plaintext = plaintext_bytes.decode('utf-8') # to string
    
    return plaintext



### Digital signatures ---

def sign_message(private_key, message):
    message_bytes = message.encode('utf-8') # to bytes 
    
    # signing message with rsa private key, sign(sk, H(m))
    signature = private_key.sign( 
        message_bytes,
        # PSS is a secure padding scheme for RSA signatures
        padding.PSS( 
            mgf=padding.MGF1(hashes.SHA256()),
            salt_length=padding.PSS.MAX_LENGTH
        ),
        hashes.SHA256()
    )
    return signature


def verify_signature(public_key, message, signature):
    message_bytes = message.encode('utf-8') # to bytes 
    
    # verifying the signature with senders public key
    try:
        public_key.verify(
            signature,
            message_bytes,
            padding.PSS(
                mgf=padding.MGF1(hashes.SHA256()),
                salt_length=padding.PSS.MAX_LENGTH
            ),
            hashes.SHA256()
        )
        return True
    except:
        return False
    


### Fingerprint ---

def generate_fingerprint(public_key):
    # exporting public key to bytes so we can hash it
    public_key_bytes = public_key.public_bytes(
        encoding=Encoding.PEM,
        format=PublicFormat.SubjectPublicKeyInfo
    )
    
    # hashing public key using SHA-256
    hash_bytes = hashlib.sha256(public_key_bytes).digest()
    
    # converting to hex string to make it readable for user
    fingerprint = ""
    for byte in hash_bytes:
        fingerprint = fingerprint + format(byte, '02x')
    
    # splitting into groups of 4 for readability, e.g. a3f2 b891 c234
    groups = []
    for i in range(0, len(fingerprint), 4):
        groups.append(fingerprint[i:i+4])
    
    readable_fingerprint = " ".join(groups)
    
    return readable_fingerprint




### Replay protection ---

# all message IDs already used
seen_message_ids = set()

def generate_message_id():
    # generating unique ID for each message using UUID (random unique string) 
    message_id = str(uuid.uuid4())
    return message_id

def is_replay(message_id):
    # checking if we have seen message ID before
    if message_id in seen_message_ids:
        return True
    else:
        seen_message_ids.add(message_id)
        return False