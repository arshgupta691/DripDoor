package com.dripdoor.util;

/**
 * Evaluates password strength and returns a 0-100 score.
 *
 * Score bands:
 *   0-24   → Very Weak  (red)
 *   25-49  → Weak       (orange)
 *   50-74  → Fair       (yellow)
 *   75-89  → Strong     (lime)
 *   90-100 → Very Strong (gold / #D4AF37)
 */
public final class PasswordStrengthUtil {

    private PasswordStrengthUtil() {}

    public record Result(int score, String label, java.awt.Color color) {}

    public static Result evaluate(String password) {
        if (password == null || password.isEmpty())
            return new Result(0, "Enter password", java.awt.Color.LIGHT_GRAY);

        int score = 0;

        // Length
        if (password.length() >= 8)  score += 20;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        // Character variety
        if (password.chars().anyMatch(Character::isUpperCase))      score += 15;
        if (password.chars().anyMatch(Character::isLowerCase))      score += 10;
        if (password.chars().anyMatch(Character::isDigit))          score += 15;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) score += 20;

        score = Math.min(score, 100);

        return switch (score / 25) {
            case 0  -> new Result(score, "Very Weak",   new java.awt.Color(0xE53935));
            case 1  -> new Result(score, "Weak",        new java.awt.Color(0xFB8C00));
            case 2  -> new Result(score, "Fair",        new java.awt.Color(0xFDD835));
            case 3  -> new Result(score, "Strong",      new java.awt.Color(0x7CB342));
            default -> new Result(score, "Very Strong", new java.awt.Color(0xD4AF37));
        };
    }
}
