package com.dripdoor.controller;

import com.dripdoor.service.AuthService;
import com.dripdoor.util.EmailValidator;
import com.dripdoor.view.LoginView;
import com.dripdoor.view.MainDashboardView;
import com.dripdoor.view.OTPVerificationView;
import com.dripdoor.view.SignupView;

import javax.swing.*;

/**
 * Mediates between Auth views and AuthService.
 *
 * All network/Firebase calls are performed on a SwingWorker background thread
 * via the static performLogin / performRegister helpers.  Views call these
 * from their own SwingWorker.doInBackground() and handle results in done().
 */
public class AuthController {

    // ── Outcome enums (returned from background threads) ──────────────────

    public enum LoginOutcome  { SUCCESS, INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED, ERROR }
    public enum SignupOutcome { SUCCESS, EMAIL_EXISTS, WEAK_PASSWORD, ERROR }

    // ── Instance state ────────────────────────────────────────────────────

    private final LoginView loginView;
    private SignupView signupView;

    public AuthController(LoginView loginView) {
        this.loginView = loginView;
    }

    // ── Navigation ────────────────────────────────────────────────────────

    public void showSignup() {
        signupView = new SignupView(loginView);
        signupView.setVisible(true);
    }

    // ── Background-thread workers (called from SwingWorker.doInBackground) ─

    /** Calls AuthService.login and maps result to LoginOutcome. */
    public static LoginOutcome performLogin(String email, String password) {
        return switch (AuthService.login(email, password)) {
            case SUCCESS           -> LoginOutcome.SUCCESS;
            case INVALID_CREDENTIALS -> LoginOutcome.INVALID_CREDENTIALS;
            case EMAIL_NOT_VERIFIED  -> LoginOutcome.EMAIL_NOT_VERIFIED;
            case ERROR               -> LoginOutcome.ERROR;
        };
    }

    /** Calls AuthService.register and maps result to SignupOutcome. */
    public static SignupOutcome performRegister(String name, String email,
                                                String password) {
        return switch (AuthService.register(name, email, password)) {
            case SUCCESS       -> SignupOutcome.SUCCESS;
            case EMAIL_EXISTS  -> SignupOutcome.EMAIL_EXISTS;
            case WEAK_PASSWORD -> SignupOutcome.WEAK_PASSWORD;
            case ERROR         -> SignupOutcome.ERROR;
        };
    }

    // ── Resend verification email ─────────────────────────────────────────

    /**
     * Resends the Firebase email verification link.
     * Prompts for the password because Firebase requires a fresh ID token.
     */
    public static void handleResendVerification(OTPVerificationView view, String email) {
        String password = JOptionPane.showInputDialog(
                view,
                "Enter your password to resend the verification email:",
                "Resend Verification",
                JOptionPane.PLAIN_MESSAGE
        );

        if (password == null || password.isBlank()) return;

        view.showInfo("Sending…");

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return AuthService.resendVerificationEmail(email, password);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        view.showSuccess("Verification email resent — check your inbox.");
                    } else {
                        view.showError("Could not resend. Check your password and try again.");
                    }
                } catch (Exception ex) {
                    view.showError("An error occurred. Please try again.");
                }
            }
        }.execute();
    }
}
