package com.comp3334_t67.server.services;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
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
    private static final int MIN_RSA_KEY_BITS = 2048;
    private static final BigInteger EXPECTED_RSA_EXPONENT = BigInteger.valueOf(65537);
    
    
    public RSAPublicKey validateAndParseRsaPublicKey(String pemKey) {
        // Validate the PEM envelope first.
        validatePemEnvelope(pemKey);

        // Decode the Base64 body inside the PEM markers.
        byte[] decoded = decodePemBody(extractBase64Body(pemKey));
        validateEncodedLength(decoded);

        // Parse and type-check RSA public key.
        RSAPublicKey rsaPublicKey = parseRsaPublicKey(decoded);

        // Enforce key strength and expected exponent.
        validateRsaParameters(rsaPublicKey);
        return rsaPublicKey;
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

    private static RSAPublicKey parseRsaPublicKey(byte[] decoded) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
                throw new InvalidInputException("Key is not an RSA public key");
            }
            return rsaPublicKey;

        } catch (InvalidInputException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidInputException("Malformed or unparsable RSA public key", e);
        }
    }

    private static void validateRsaParameters(RSAPublicKey rsaKey) {
        int bitLength = rsaKey.getModulus().bitLength();

        if (bitLength < MIN_RSA_KEY_BITS) {
            throw new InvalidInputException("RSA key too weak (must be at least 2048 bits)");
        }

        if (!rsaKey.getPublicExponent().equals(EXPECTED_RSA_EXPONENT)) {
            throw new InvalidInputException("Unexpected RSA public exponent");
        }

        if (rsaKey.getModulus().signum() <= 0) {
            throw new InvalidInputException("Invalid RSA modulus");
        }
    }
}
