package com.agentportal.integration.forgecity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ForgeCityKeyAuthenticator {

    private ForgeCityKeyAuthenticator() {
    }

    public static boolean matches(String supplied, String configured) {
        if (supplied == null || configured == null || configured.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(digest(supplied), digest(configured));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
