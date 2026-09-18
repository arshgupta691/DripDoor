package com.dripdoor.util;

import java.util.regex.Pattern;

/**
 * RFC-5322-inspired email validator using compiled regex.
 */
public final class EmailValidator {

    private EmailValidator() {}

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * Returns true if the given string is a syntactically valid email address.
     */
    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
