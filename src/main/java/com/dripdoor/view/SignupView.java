package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.AuthController;
import com.dripdoor.util.EmailValidator;
import com.dripdoor.util.PasswordStrengthUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Signup screen with real-time password strength indicator.
 * Network calls run on a SwingWorker background thread; a LoadingOverlay
 * covers the parent window while the request is in flight.
 */
public class SignupView extends JDialog {

    private JTextField     nameField;
    private JTextField     emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JProgressBar   strengthBar;
    private JLabel         strengthLabel;
    private JLabel         errorLabel;
    private JButton        signupBtn;

    private final JFrame parentFrame;

    public SignupView(JFrame parent) {
        super(parent, "DripDoor — Create Account", true);
        this.parentFrame = parent;
        initUI();
    }

    private void initUI() {
        setSize(480, 680);
        setLocationRelativeTo(getOwner());
        setResizable(false);

        JPanel root = new JPanel();
        root.setBackground(ThemeConfig.WHITE);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(40, 50, 40, 50));
        setContentPane(root);

        // Header
        JLabel header = new JLabel("Create Your Account");
        header.setFont(ThemeConfig.HEADER_FONT.deriveFont(22f));
        header.setForeground(ThemeConfig.DEEP_BLACK);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(header);

        JLabel sub = new JLabel("Join the DripDoor inner circle");
        sub.setFont(ThemeConfig.SUB_FONT);
        sub.setForeground(ThemeConfig.GOLD);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(sub);

        root.add(Box.createVerticalStrut(30));

        // Full name
        root.add(lbl("Full Name"));
        root.add(Box.createVerticalStrut(6));
        nameField = field("Alexandra Laurent");
        root.add(nameField);

        root.add(Box.createVerticalStrut(16));

        // Email
        root.add(lbl("Email Address"));
        root.add(Box.createVerticalStrut(6));
        emailField = field("you@example.com");
        root.add(emailField);

        root.add(Box.createVerticalStrut(16));

        // Password
        root.add(lbl("Password"));
        root.add(Box.createVerticalStrut(6));
        passwordField = passField();
        root.add(passwordFieldWithToggle(passwordField));

        root.add(Box.createVerticalStrut(8));

        // Strength bar
        strengthBar = new JProgressBar(0, 100);
        strengthBar.setForeground(ThemeConfig.GOLD);
        strengthBar.setBackground(ThemeConfig.BORDER);
        strengthBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        strengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        strengthBar.setBorderPainted(false);
        root.add(strengthBar);

        root.add(Box.createVerticalStrut(4));

        strengthLabel = new JLabel("Password strength");
        strengthLabel.setFont(ThemeConfig.SMALL_FONT);
        strengthLabel.setForeground(Color.GRAY);
        strengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(strengthLabel);

        root.add(Box.createVerticalStrut(16));

        // Confirm password
        root.add(lbl("Confirm Password"));
        root.add(Box.createVerticalStrut(6));
        confirmField = passField();
        root.add(passwordFieldWithToggle(confirmField));

        root.add(Box.createVerticalStrut(10));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeConfig.SMALL_FONT);
        errorLabel.setForeground(new Color(0xC62828));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(errorLabel);

        root.add(Box.createVerticalStrut(20));

        // Submit button
        signupBtn = LoginView.luxuryButton("CREATE ACCOUNT");
        signupBtn.addActionListener(e -> handleSubmit());
        root.add(signupBtn);

        // Real-time password strength
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateStrength(); }
            public void removeUpdate(DocumentEvent e)  { updateStrength(); }
            public void changedUpdate(DocumentEvent e) { updateStrength(); }
        });
    }

    private void updateStrength() {
        String pwd = new String(passwordField.getPassword());
        PasswordStrengthUtil.Result r = PasswordStrengthUtil.evaluate(pwd);
        strengthBar.setValue(r.score());
        strengthBar.setForeground(r.color());
        strengthLabel.setText(r.label());
        strengthLabel.setForeground(r.color());
    }

    private void handleSubmit() {
        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String pwd     = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        // ── Client-side validation (fast, no network) ─────────────────────
        if (name.isBlank()) {
            showError("Name cannot be empty.");
            return;
        }
        if (!EmailValidator.isValid(email)) {
            showError("Please enter a valid email address.");
            return;
        }
        if (pwd.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!pwd.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        // ── Show overlay on parent window, run registration in background ──
        LoadingOverlay overlay = new LoadingOverlay(parentFrame, "Creating your account…");
        overlay.show();
        setFormEnabled(false);

        new SwingWorker<AuthController.SignupOutcome, Void>() {
            @Override
            protected AuthController.SignupOutcome doInBackground() {
                return AuthController.performRegister(name, email, pwd);
            }

            @Override
            protected void done() {
                overlay.hide();
                setFormEnabled(true);
                try {
                    switch (get()) {
                        case SUCCESS -> {
                            dispose();
                            SwingUtilities.invokeLater(() -> {
                                OTPVerificationView emailInfo =
                                        new OTPVerificationView(email);
                                emailInfo.setVisible(true);
                            });
                        }
                        case EMAIL_EXISTS ->
                            showError("An account with this email already exists.");
                        case WEAK_PASSWORD ->
                            showError("Password is too weak. Use at least 6 characters.");
                        case ERROR ->
                            showError("Registration failed. Check your connection.");
                    }
                } catch (Exception ex) {
                    showError("Unexpected error. Please try again.");
                }
            }
        }.execute();
    }

    public void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setForeground(new Color(0xC62828));
    }

    private void setFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmField.setEnabled(enabled);
        signupBtn.setEnabled(enabled);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(ThemeConfig.UI_BOLD);
        l.setForeground(ThemeConfig.DEEP_BLACK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField field(String hint) {
        JTextField f = new JTextField();
        f.setFont(ThemeConfig.UI_FONT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        f.setBackground(ThemeConfig.WHITE);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setToolTipText(hint);
        return f;
    }

    private JPasswordField passField() {
        JPasswordField f = new JPasswordField();
        f.setFont(ThemeConfig.UI_FONT);
        f.setBackground(ThemeConfig.WHITE);
        f.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return f;
    }

    /**
     * Wraps a JPasswordField in a bordered panel with a gold Show/Hide toggle.
     * The DocumentListener on passwordField keeps working because it is attached
     * to the field object directly, not to this wrapper.
     */
    private JPanel passwordFieldWithToggle(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(ThemeConfig.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 8)));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        wrapper.add(field, BorderLayout.CENTER);

        JButton toggle = new JButton("Show");
        toggle.setFont(ThemeConfig.SMALL_FONT.deriveFont(Font.BOLD));
        toggle.setForeground(ThemeConfig.GOLD);
        toggle.setBackground(ThemeConfig.WHITE);
        toggle.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        toggle.setFocusPainted(false);
        toggle.setContentAreaFilled(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        toggle.addActionListener(e -> {
            if (toggle.getText().equals("Show")) {
                field.setEchoChar((char) 0);
                toggle.setText("Hide");
            } else {
                field.setEchoChar('\u2022');
                toggle.setText("Show");
            }
        });

        wrapper.add(toggle, BorderLayout.EAST);
        return wrapper;
    }


}
