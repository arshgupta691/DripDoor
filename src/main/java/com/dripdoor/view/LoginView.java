package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.AuthController;
import com.dripdoor.util.EmailValidator;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * DripDoor Login Screen — Dior-inspired minimalist design.
 */
public class LoginView extends JFrame {

    private JTextField     emailField;
    private JPasswordField passwordField;
    private JLabel         errorLabel;
    private JButton        loginBtn;
    private AuthController controller;

    public LoginView() {
        controller = new AuthController(this);
        initUI();
    }

    private void initUI() {
        setTitle("DripDoor — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        // ── Root panel ────────────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConfig.WHITE);
        setContentPane(root);

        // ── Left accent bar ───────────────────────────────────────────────
        JPanel accent = new JPanel();
        accent.setBackground(ThemeConfig.GOLD);
        accent.setPreferredSize(new Dimension(6, 0));
        root.add(accent, BorderLayout.WEST);

        // ── Centre form ───────────────────────────────────────────────────
        JPanel centre = new JPanel();
        centre.setBackground(ThemeConfig.WHITE);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBorder(new EmptyBorder(60, 50, 40, 50));
        root.add(centre, BorderLayout.CENTER);

        // Logo / brand
        JLabel logo = new JLabel("DRIP DOOR");
        logo.setFont(ThemeConfig.HEADER_FONT.deriveFont(32f));
        logo.setForeground(ThemeConfig.DEEP_BLACK);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(logo);

        JLabel tagline = new JLabel("Haute Jewellery, Curated for You");
        tagline.setFont(ThemeConfig.SUB_FONT);
        tagline.setForeground(ThemeConfig.GOLD);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(tagline);

        centre.add(Box.createVerticalStrut(40));

        // Email
        centre.add(fieldLabel("Email Address"));
        centre.add(Box.createVerticalStrut(6));
        emailField = styledTextField("you@example.com");
        centre.add(emailField);

        centre.add(Box.createVerticalStrut(20));

        // Password
        centre.add(fieldLabel("Password"));
        centre.add(Box.createVerticalStrut(6));
        passwordField = styledPasswordField();
        centre.add(passwordFieldWithToggle(passwordField));

        centre.add(Box.createVerticalStrut(8));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(ThemeConfig.SMALL_FONT);
        errorLabel.setForeground(new Color(0xC62828));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(errorLabel);

        centre.add(Box.createVerticalStrut(20));

        // Login button
        loginBtn = luxuryButton("SIGN IN");
        loginBtn.addActionListener(e -> handleLogin());
        centre.add(loginBtn);

        centre.add(Box.createVerticalStrut(20));

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConfig.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        centre.add(sep);

        centre.add(Box.createVerticalStrut(20));

        // Signup link
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkPanel.setBackground(ThemeConfig.WHITE);
        JLabel newText = new JLabel("New to DripDoor?");
        newText.setFont(ThemeConfig.UI_FONT);
        JLabel signupLink = new JLabel("<html><u>Create Account</u></html>");
        signupLink.setFont(ThemeConfig.UI_BOLD);
        signupLink.setForeground(ThemeConfig.GOLD_DARK);
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { controller.showSignup(); }
        });
        linkPanel.add(newText);
        linkPanel.add(signupLink);
        linkPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(linkPanel);

        // Enter key on password
        passwordField.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (!EmailValidator.isValid(email)) {
            showError("Please enter a valid email address.");
            return;
        }
        if (password.isEmpty()) {
            showError("Password cannot be empty.");
            return;
        }

        // ── Show loading overlay and run on background thread ─────────────
        LoadingOverlay overlay = new LoadingOverlay(this, "Signing in…");
        overlay.show();
        setFormEnabled(false);

        new SwingWorker<AuthController.LoginOutcome, Void>() {
            @Override
            protected AuthController.LoginOutcome doInBackground() {
                return AuthController.performLogin(email, password);
            }

            @Override
            protected void done() {
                overlay.hide();
                setFormEnabled(true);
                try {
                    AuthController.LoginOutcome outcome = get();
                    switch (outcome) {
                        case SUCCESS -> {
                            dispose();
                            SwingUtilities.invokeLater(() ->
                                    new MainDashboardView().setVisible(true));
                        }
                        case INVALID_CREDENTIALS ->
                            showError("Incorrect email or password.");
                        case EMAIL_NOT_VERIFIED ->
                            showError("Please verify your email — check your inbox.");
                        case ERROR ->
                            showError("Connection error. Please try again.");
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

    /** Enables/disables form inputs during loading. */
    private void setFormEnabled(boolean enabled) {
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginBtn.setEnabled(enabled);
    }

    // ── Factory helpers ───────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(ThemeConfig.UI_BOLD);
        lbl.setForeground(ThemeConfig.DEEP_BLACK);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField f = new JTextField(placeholder) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() || getText().equals(placeholder)) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.setFont(ThemeConfig.UI_FONT.deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 10, getHeight() / 2 + 5);
                }
            }
        };
        f.setFont(ThemeConfig.UI_FONT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        f.setBackground(ThemeConfig.WHITE);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) f.setText("");
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) f.setText(placeholder);
            }
        });
        return f;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(ThemeConfig.UI_FONT);
        f.setBackground(ThemeConfig.WHITE);
        f.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return f;
    }

    /**
     * Wraps a JPasswordField in a bordered panel with a gold Show/Hide toggle.
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



    public static JButton luxuryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(180, 180, 180));
                } else if (getModel().isRollover()) {
                    g2.setColor(ThemeConfig.GOLD);
                } else {
                    g2.setColor(ThemeConfig.DEEP_BLACK);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(getModel().isRollover() && isEnabled()
                        ? ThemeConfig.DEEP_BLACK : ThemeConfig.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(ThemeConfig.UI_BOLD.deriveFont(14f));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }
}
