"""
security_tests.py — CipherTalk Security Test Suite
====================================================
Group project: COMP3334 — Computer Systems Security

Person D: Testing + Utils + Report Support

This file contains the two required security test cases:
  1. simulate_replay_attack()   — verifies replay protection (R9 / R22)
  2. tamper_message_test()      — verifies AEAD integrity protection (R8)

Run from the project root:
    python tests/security_tests.py

Requirements:
    pip install cryptography bcrypt
"""

import sys
import os

# Make crypto_module importable when running from project root
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from crypto_module import crypto_module as crypto

# ── Helpers ──────────────────────────────────────────────────────────────────

def print_header(title):
    print("\n" + "=" * 60)
    print(f"  {title}")
    print("=" * 60)

def print_result(label, passed):
    status = "PASS ✓" if passed else "FAIL ✗"
    print(f"  [{status}] {label}")

def make_shared_key():
    """
    Generate a fresh DH shared key for testing.
    Both sides use the same parameters and key pair (loopback demo),
    which is sufficient to produce a valid AES-GCM key for these tests.
    """
    params = crypto.generate_dh_parameters()
    priv, pub = crypto.generate_dh_keypair(params)
    return crypto.compute_shared_secret(priv, pub)


# ── Test 1: Replay Attack Simulation ─────────────────────────────────────────

def simulate_replay_attack():
    """
    Security Test Case 1 — Replay Attack Simulation
    ------------------------------------------------
    Threat: A network attacker (or malicious server) captures a valid
    encrypted message and re-sends it to the recipient at a later time,
    hoping the client will accept it as a new message.

    Defence (R9 / R22): The client tracks every message ID it has already
    processed in `seen_message_ids`. When a message ID arrives a second
    time, `is_replay()` returns True and the message is silently dropped.

    Expected outcomes
    -----------------
    Step 1 — First delivery:   is_replay() returns False  → message accepted.
    Step 2 — Replay attempt:   is_replay() returns False  → message accepted.
                               (New message ID, different content.)
    Step 3 — Exact replay:     is_replay() returns True   → message REJECTED.
    Step 4 — Tampered ID replay: is_replay() returns False → treated as new
                               message, but AES-GCM decryption will fail
                               because the ciphertext is bound to the original
                               nonce/key (shown separately in tamper test).
    """
    print_header("TEST 1: Replay Attack Simulation")
    print()
    print("  Scenario: Attacker intercepts a valid ciphertext and")
    print("  replays it to the recipient, hoping it is accepted again.")
    print()

    shared_key = make_shared_key()
    results = []

    # ── Step 1: Alice sends a normal message ─────────────────────────────────
    print("  [Step 1] Alice sends message to Bob (first delivery)")

    message_id_1 = crypto.generate_message_id()
    associated_data = f"msg:{message_id_1}|from:alice|to:bob".encode()

    nonce_1, ciphertext_1 = crypto.encrypt_message(
        shared_key, "Hello Bob, this is secret!", associated_data
    )

    # Simulate Bob receiving it for the first time
    first_seen = not crypto.is_replay(message_id_1)  # should be False → accepted
    accepted_first = first_seen
    print(f"    Message ID : {message_id_1}")
    print(f"    Accepted   : {accepted_first}  (expected: True)")
    print_result("First delivery accepted", accepted_first)
    results.append(accepted_first)

    # ── Step 2: A different, legitimate message ───────────────────────────────
    print()
    print("  [Step 2] Alice sends a second, different message")

    message_id_2 = crypto.generate_message_id()
    associated_data_2 = f"msg:{message_id_2}|from:alice|to:bob".encode()
    nonce_2, ciphertext_2 = crypto.encrypt_message(
        shared_key, "Are you there?", associated_data_2
    )

    second_seen = not crypto.is_replay(message_id_2)
    print(f"    Message ID : {message_id_2}")
    print(f"    Accepted   : {second_seen}  (expected: True)")
    print_result("Second (different) message accepted", second_seen)
    results.append(second_seen)

    # ── Step 3: Attacker replays message_id_1 ────────────────────────────────
    print()
    print("  [Step 3] Attacker replays the first message (same message ID)")
    print("  (Attacker re-sends the exact ciphertext + message ID from Step 1)")

    replayed = crypto.is_replay(message_id_1)   # True → drop
    replay_blocked = replayed
    print(f"    Replayed ID: {message_id_1}")
    print(f"    Blocked    : {replay_blocked}  (expected: True)")
    print_result("Replay correctly blocked", replay_blocked)
    results.append(replay_blocked)

    # ── Step 4: Attacker modifies the message ID (spoofed ID) ────────────────
    print()
    print("  [Step 4] Attacker invents a new message ID but reuses old ciphertext")
    print("  (Spoofed ID passes replay filter, but decryption must fail)")

    spoofed_id = crypto.generate_message_id()   # brand-new ID
    spoofed_ad = f"msg:{spoofed_id}|from:alice|to:bob".encode()

    spoofed_passes_replay_filter = not crypto.is_replay(spoofed_id)
    print(f"    Spoofed ID : {spoofed_id}")
    print(f"    Passes replay filter: {spoofed_passes_replay_filter}  (expected: True — new ID)")

    # But decryption with the changed associated_data must fail:
    decryption_failed = False
    try:
        # Using original nonce + ciphertext but the spoofed associated_data
        crypto.decrypt_message(shared_key, nonce_1, ciphertext_1, spoofed_ad)
    except Exception as e:
        decryption_failed = True
        print(f"    Decryption error : {type(e).__name__} — {e}")

    print(f"    Decryption failed  : {decryption_failed}  (expected: True)")
    print_result("Spoofed-ID message rejected by AEAD", decryption_failed)
    results.append(decryption_failed)

    # ── Summary ──────────────────────────────────────────────────────────────
    print()
    all_passed = all(results)
    if all_passed:
        print("  ✓ RESULT: Replay attack simulation PASSED — all sub-checks OK.")
        print("  The replay protection (seen_message_ids + AEAD binding)")
        print("  correctly prevents acceptance of replayed messages.")
    else:
        print("  ✗ RESULT: One or more sub-checks FAILED. Review output above.")

    return all_passed


# ── Test 2: Message Tampering Test ────────────────────────────────────────────

def tamper_message_test():
    """
    Security Test Case 2 — Message Tampering / Integrity Test
    ----------------------------------------------------------
    Threat: A network attacker or honest-but-curious server intercepts
    the ciphertext in transit and flips bits, trying to alter the
    plaintext or the authenticated metadata without being detected.

    Defence (R8): AES-GCM is an Authenticated Encryption with Associated
    Data (AEAD) scheme. The 128-bit authentication tag covers both the
    ciphertext and the associated data. Any modification to either field
    causes decryption to raise an InvalidTag exception, guaranteeing
    both confidentiality and integrity.

    Expected outcomes
    -----------------
    Case A — Ciphertext byte flipped:    decryption raises InvalidTag → BLOCKED.
    Case B — Associated data modified:   decryption raises InvalidTag → BLOCKED.
    Case C — Nonce replaced:             decryption raises InvalidTag → BLOCKED.
    Case D — Unmodified message:         decryption succeeds → ACCEPTED.
    """
    print_header("TEST 2: Message Tampering Test (AEAD Integrity)")
    print()
    print("  Scenario: Attacker intercepts ciphertext and tries to")
    print("  modify its content without being detected.")
    print()

    shared_key = make_shared_key()
    plaintext  = "Transfer 100 USD to Alice"
    message_id = crypto.generate_message_id()
    associated_data = f"msg:{message_id}|from:alice|to:bob|chat:42".encode()

    nonce, ciphertext = crypto.encrypt_message(shared_key, plaintext, associated_data)
    print(f"  Original plaintext : \"{plaintext}\"")
    print(f"  Ciphertext length  : {len(ciphertext)} bytes")
    print()

    results = []

    # ── Case A: Flip a byte in the ciphertext ────────────────────────────────
    print("  [Case A] Attacker flips bit 0 of the ciphertext")

    tampered_ct = bytearray(ciphertext)
    tampered_ct[0] ^= 0xFF          # flip all bits in first byte
    tampered_ct = bytes(tampered_ct)

    failed_a = False
    try:
        crypto.decrypt_message(shared_key, nonce, tampered_ct, associated_data)
    except Exception as e:
        failed_a = True
        print(f"    Exception caught : {type(e).__name__}")

    print(f"    Tampered ciphertext rejected: {failed_a}  (expected: True)")
    print_result("Case A — ciphertext tampering detected", failed_a)
    results.append(failed_a)

    # ── Case B: Modify the associated data ───────────────────────────────────
    print()
    print("  [Case B] Attacker changes receiver in associated data")
    print("           (trying to redirect message to a different user)")

    malicious_ad = f"msg:{message_id}|from:alice|to:eve|chat:42".encode()

    failed_b = False
    try:
        crypto.decrypt_message(shared_key, nonce, ciphertext, malicious_ad)
    except Exception as e:
        failed_b = True
        print(f"    Exception caught : {type(e).__name__}")

    print(f"    Tampered metadata rejected : {failed_b}  (expected: True)")
    print_result("Case B — associated data tampering detected", failed_b)
    results.append(failed_b)

    # ── Case C: Replace the nonce ─────────────────────────────────────────────
    print()
    print("  [Case C] Attacker replaces the nonce with a random value")

    import os
    wrong_nonce = os.urandom(12)

    failed_c = False
    try:
        crypto.decrypt_message(shared_key, wrong_nonce, ciphertext, associated_data)
    except Exception as e:
        failed_c = True
        print(f"    Exception caught : {type(e).__name__}")

    print(f"    Wrong nonce rejected : {failed_c}  (expected: True)")
    print_result("Case C — nonce substitution detected", failed_c)
    results.append(failed_c)

    # ── Case D: Legitimate decryption (control case) ──────────────────────────
    print()
    print("  [Case D] Legitimate decryption with correct key, nonce, and AD")

    success_d = False
    try:
        recovered = crypto.decrypt_message(shared_key, nonce, ciphertext, associated_data)
        success_d = (recovered == plaintext)
        print(f"    Recovered plaintext: \"{recovered}\"")
    except Exception as e:
        print(f"    Unexpected error: {e}")

    print(f"    Decryption succeeded : {success_d}  (expected: True)")
    print_result("Case D — unmodified message decrypts correctly", success_d)
    results.append(success_d)

    # ── Summary ──────────────────────────────────────────────────────────────
    print()
    all_passed = all(results)
    if all_passed:
        print("  ✓ RESULT: Tampering test PASSED — all sub-checks OK.")
        print("  AES-GCM authentication tag correctly detects any modification")
        print("  to the ciphertext, associated data, or nonce.")
    else:
        print("  ✗ RESULT: One or more sub-checks FAILED. Review output above.")

    return all_passed


# ── Runner ────────────────────────────────────────────────────────────────────

def run_all_tests():
    print("\n" + "#" * 60)
    print("#  CipherTalk — Security Test Suite")
    print("#  COMP3334 Group Project")
    print("#" * 60)

    # Reset replay state between test runs so tests are independent
    crypto.seen_message_ids.clear()

    results = {
        "simulate_replay_attack": simulate_replay_attack(),
        "tamper_message_test"   : tamper_message_test(),
    }

    print_header("OVERALL RESULTS")
    all_ok = True
    for name, passed in results.items():
        print_result(name, passed)
        if not passed:
            all_ok = False

    print()
    if all_ok:
        print("  All security tests PASSED.")
    else:
        print("  Some tests FAILED — see details above.")
    print()


if __name__ == "__main__":
    run_all_tests()
