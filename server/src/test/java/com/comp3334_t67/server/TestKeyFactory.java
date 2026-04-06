package com.comp3334_t67.server;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class TestKeyFactory {

    private TestKeyFactory() {
    }

    public static String generatePemPublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String base64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }
}
