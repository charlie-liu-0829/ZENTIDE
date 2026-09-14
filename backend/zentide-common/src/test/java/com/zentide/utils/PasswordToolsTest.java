package com.zentide.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordToolsTest {

    @Test
    void encodesNewPasswordsWithBcrypt() {
        String encoded = PasswordTools.encode("Zentide-Password-2026");

        assertTrue(encoded.startsWith("$2"));
        assertNotEquals("Zentide-Password-2026", encoded);
        assertTrue(PasswordTools.matches("Zentide-Password-2026", encoded));
        assertFalse(PasswordTools.matches("wrong-password", encoded));
        assertFalse(PasswordTools.needsUpgrade(encoded));
    }

    @Test
    void acceptsLegacyMd5OnlyForMigration() {
        String legacy = DigestUtils.md5Hex("Legacy-Password-2026");

        assertTrue(PasswordTools.matches("Legacy-Password-2026", legacy));
        assertFalse(PasswordTools.matches("wrong-password", legacy));
        assertTrue(PasswordTools.needsUpgrade(legacy));
    }
}
