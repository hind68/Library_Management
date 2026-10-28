package com.smartlibrary.util;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtil {
    private static final int BCRYPT_COST = 12;
    // Prevents creating objects because this class only contains static helpers.
    private PasswordUtil() {}

    // Creates a salted BCrypt hash for every new password.
    // Parameters: password is the plain password entered by the user.
    public static String hash(String password) {
        if (password == null) throw new IllegalArgumentException("Password is required.");
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
    }

    // Compares a raw password with a stored password hash.
    // Parameters: rawPassword is the plain password entered by the user; hash is the stored password hash.
    public static boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null || hash.isBlank()) return false;
        if (isBcrypt(hash)) {
            try {
                return BCrypt.checkpw(rawPassword, hash);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        if (!hash.matches("(?i)[0-9a-f]{64}")) return false;
        byte[] expected = hash.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = legacySha256(rawPassword).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }

    public static boolean needsUpgrade(String hash) {
        return !isBcrypt(hash);
    }

    private static boolean isBcrypt(String hash) {
        return hash != null && hash.matches("^\\$2[aby]\\$\\d{2}\\$.*");
    }

    static String legacySha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(64);
            for (byte b : bytes) builder.append(String.format("%02x", b));
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
