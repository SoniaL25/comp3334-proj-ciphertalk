package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.TestKeyFactory;
import com.comp3334_t67.server.Exceptions.InvalidInputException;

class KeyValidatorServiceTest {

    private final KeyValidator validator = new KeyValidator();

    @Test
    void validateAndParseRsaPublicKey_shouldReturnKey_whenPemIsValid() throws Exception {
        // Arrange: generate a valid RSA public key in PEM format.
        String pem = TestKeyFactory.generatePemPublicKey();

        // Act: parse and validate key.
        var key = validator.validateAndParseRsaPublicKey(pem);

        // Assert: parsed key is not null.
        assertNotNull(key);
    }

    @Test
    void validateAndParseRsaPublicKey_shouldThrow_whenPemIsInvalid() {
        // Arrange: use invalid input.
        String invalidPem = "invalid";

        // Act + Assert: validator rejects malformed input.
        assertThrows(InvalidInputException.class, () -> validator.validateAndParseRsaPublicKey(invalidPem));
    }
}
