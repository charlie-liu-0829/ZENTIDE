package com.zentide.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Central password hashing policy.
 *
 * <p>New passwords use BCrypt. The MD5 branch exists only to migrate accounts
 * created by older ZENTIDE versions and should be removed after all active
 * users have successfully logged in once.</p>
 */
public final class PasswordTools {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private PasswordTools() {
    }

    public static String encode(String rawPassword) {
        return StringTools.isEmpty(rawPassword) ? null : BCRYPT.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (StringTools.isEmpty(rawPassword) || StringTools.isEmpty(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return BCRYPT.matches(rawPassword, storedPassword);
        }
        return storedPassword.equalsIgnoreCase(DigestUtils.md5Hex(rawPassword));
    }

    public static boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith("$2a$")
                && !storedPassword.startsWith("$2b$") && !storedPassword.startsWith("$2y$");
    }
}
