package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.AuthController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Shown after successful signup — instructs the user to click the
 * verification link that Firebase sent to their email address.
 */
public class OTPVerificationView extends JDialog {

    private final String email;
    private JLabel messageLabel;

    public OTPVerificationView(String email) {
        super((Frame) null, "DripDoor — Verify Your Email", true);
        this.email = email;
        initUI();
    }

    private void initUI() {
        setSize(440, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel();
        root.setBackground(ThemeConfig.WHITE);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(50, 50, 40, 50));
        setContentPane(root);

        // Icon
        JLabel icon = new JLabel("✉", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(icon);

        root.add(Box.createVerticalStrut(16));

        JLabel title = new JLabel("Check Your Email");
        title.setFont(ThemeConfig.HEADER_FONT.deriveFont(20f));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(title);

        root.add(Box.createVerticalStrut(12));

        JLabel sub = new JLabel(
            "<html><center>A verification link has been sent to<br>"
            + "<b>" + email + "</b><br><br>"
            + "Click the link in that email, then come<br>"
            + "back here and log in.</center></html>"
        );
        sub.setFont(ThemeConfig.UI_FONT);
        sub.setForeground(Color.GRAY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(sub);

        root.add(Box.createVerticalStrut(20));

        // Feedback label
        messageLabel = new JLabel(" ");
        messageLabel.setFont(ThemeConfig.SMALL_FONT);
        messageLabel.setForeground(ThemeConfig.GOLD_DARK);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(messageLabel);

        root.add(Box.createVerticalStrut(10));

        // Go to Login button
        JButton loginBtn = LoginView.luxuryButton("GO TO LOGIN");
        loginBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
        });
        root.add(loginBtn);

        root.add(Box.createVerticalStrut(14));

        // Resend link
        JLabel resend = new JLabel("<html><u>Resend verification email</u></html>");
        resend.setFont(ThemeConfig.SMALL_FONT);
        resend.setForeground(ThemeConfig.GOLD_DARK);
        resend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resend.setAlignmentX(Component.CENTER_ALIGNMENT);
        resend.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                AuthController.handleResendVerification(OTPVerificationView.this, email);
            }
        });
        root.add(resend);
    }

    public void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setForeground(new Color(0xC62828));
    }

    public void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.setForeground(new Color(0x2E7D32));
    }

    public void showInfo(String msg) {
        messageLabel.setText(msg);
        messageLabel.setForeground(ThemeConfig.GOLD_DARK);
    }
}
