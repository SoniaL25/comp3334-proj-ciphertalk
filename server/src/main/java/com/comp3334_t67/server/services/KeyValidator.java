package com.comp3334_t67.server.services;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.comp3334_t67.server.Exceptions.InvalidInputException;

@Service
public class KeyValidator {

    private static final String PEM_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_END = "-----END PUBLIC KEY-----";
    private static final int MIN_ENCODED_KEY_BYTES = 200;
    private static final int MAX_ENCODED_KEY_BYTES = 1000;

    public PublicKey validateAndParseRsaPublicKey(String pemKey) {
        validatePemEnvelope(pemKey);

        byte[] decoded = decodePemBody(extractBase64Body(pemKey));
        validateEncodedLength(decoded);

        return parsePublicKey(decoded);
    }

    private static void validatePemEnvelope(String pemKey) {
        if (pemKey == null || pemKey.isBlank()) {
            throw new InvalidInputException("Public key is empty");
        }

        if (!pemKey.contains(PEM_BEGIN) || !pemKey.contains(PEM_END)) {
            throw new InvalidInputException("Invalid PEM public key format");
        }
    }

    private static String extractBase64Body(String pemKey) {
        return pemKey
            .replace(PEM_BEGIN, "")
            .replace(PEM_END, "")
            .replaceAll("\\s", "");
    }

    private static byte[] decodePemBody(String base64Key) {
        try {
            return Base64.getDecoder().decode(base64Key);
        } catch (RuntimeException e) {
            throw new InvalidInputException("Public key is not valid Base64", e);
        }
    }

    private static void validateEncodedLength(byte[] decoded) {
        if (decoded.length < MIN_ENCODED_KEY_BYTES || decoded.length > MAX_ENCODED_KEY_BYTES) {
            throw new InvalidInputException("Public key length is suspicious");
        }
    }

    private static PublicKey parsePublicKey(byte[] decoded) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);

        try {
            try {
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            } catch (Exception ignored) {
                // Fall through to other supported key types.
            }

            try {
                return KeyFactory.getInstance("DH").generatePublic(spec);
            } catch (Exception ignored) {
                // Fall through to other supported key types.
            }

            throw new InvalidInputException("Key is not a supported public key type");
        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("Malformed or unparsable public key", e);
        }
    }
}