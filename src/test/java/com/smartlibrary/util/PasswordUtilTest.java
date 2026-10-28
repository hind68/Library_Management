package com.smartlibrary.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test
    void newPasswordsUseSaltedBcryptHashes() {
        String first = PasswordUtil.hash("correct horse battery staple");
        String second = PasswordUtil.hash("correct horse battery staple");

        assertTrue(first.startsWith("$2a$12$"));
        assertNotEquals(first, second);
        assertTrue(PasswordUtil.matches("correct horse battery staple", first));
        assertFalse(PasswordUtil.matches("wrong", first));
        assertFalse(PasswordUtil.needsUpgrade(first));
    }

    @Test
    void legacySha256RemainsCompatibleAndRequiresUpgrade() {
        String legacy = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";

        assertTrue(PasswordUtil.matches("admin123", legacy));
        assertFalse(PasswordUtil.matches("wrong", legacy));
        assertTrue(PasswordUtil.needsUpgrade(legacy));
    }

    @Test
    void malformedHashesFailClosed() {
        assertFalse(PasswordUtil.matches("password", null));
        assertFalse(PasswordUtil.matches("password", "not-a-hash"));
        assertFalse(PasswordUtil.matches("password", "$2a$bad"));
    }
}
