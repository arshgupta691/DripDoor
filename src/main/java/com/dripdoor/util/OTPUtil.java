package com.dripdoor.util;

/**
 * OTPUtil is no longer used — email verification is handled by Firebase Auth.
 * This stub is retained only to prevent compilation errors if any legacy
 * reference remains; it performs no operations.
 *
 * @deprecated Use Firebase Auth email verification instead.
 */
@Deprecated
public final class OTPUtil {
    private OTPUtil() {}

    /** @deprecated No-op. */
    @Deprecated
    public static String generate(String email) { return ""; }

    /** @deprecated Always returns false. */
    @Deprecated
    public static boolean validate(String email, String code) { return false; }

    /** @deprecated No-op. */
    @Deprecated
    public static void invalidate(String email) {}
}
