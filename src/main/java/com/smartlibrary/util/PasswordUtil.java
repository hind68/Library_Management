package com.smartlibrary.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {
    // Prevents creating objects because this class only contains static helpers.
    private PasswordUtil() {}

    // Creates a SHA-256 hash for a password.
    // Parameters: password is the plain password entered by the user.
    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                // Keep each byte as two hexadecimal characters for a stable hash string.
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    // Compares a raw password with a stored password hash.
    // Parameters: rawPassword is the plain password entered by the user; hash is the stored password hash.
    public static boolean matches(String rawPassword, String hash) {
        return hash(rawPassword).equalsIgnoreCase(hash);
    }
}
