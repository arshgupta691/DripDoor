package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.OrderController;
import com.dripdoor.model.Order;
import com.dripdoor.util.CircularDripCalculator;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Order confirmation dialog — shows the Circular Drip credit breakdown
 * and launches the 10-minute dispatch progress bar.
 */
public class CheckoutView extends JDialog {

    private final Order order;

    public CheckoutView(JFrame parent, Order order) {
        super(parent, "DripDoor — Order Confirmed", true);
        this.order = order;
        initUI();
    }

    private void initUI() {
        setSize(520, 560);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel();
        root.setBackground(ThemeConfig.WHITE);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(40, 48, 36, 48));
        setContentPane(root);

        // ── Tick ──────────────────────────────────────────────────────────
        JLabel tick = new JLabel("✔", SwingConstants.CENTER);
        tick.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        tick.setForeground(new Color(0x2E7D32));
        tick.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(tick);

        root.add(Box.createVerticalStrut(10));

        // ── Order ID ──────────────────────────────────────────────────────
        JLabel orderIdLabel = new JLabel("Order #" + order.getOrderId());
        orderIdLabel.setFont(ThemeConfig.HEADER_FONT.deriveFont(20f));
        orderIdLabel.setForeground(ThemeConfig.DEEP_BLACK);
        orderIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(orderIdLabel);

        root.add(Box.createVerticalStrut(4));

        JLabel confirmed = new JLabel("Your order has been confirmed.");
        confirmed.setFont(ThemeConfig.UI_FONT);
        confirmed.setForeground(Color.GRAY);
        confirmed.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(confirmed);

        root.add(Box.createVerticalStrut(28));

        // ── Breakdown card ────────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setBackground(ThemeConfig.SOFT_GREY);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                new LineBorder(ThemeConfig.BORDER, 1, true),
                new EmptyBorder(18, 22, 18, 22)));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        addRow(card, "Items",           order.getItems().size() + " piece(s)");
        addRow(card, "Subtotal",        String.format("₹%,.2f", order.getTotalAmount()));

        card.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConfig.GOLD);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(10));

        // Circular Drip credit highlight
        JLabel creditTitle = new JLabel("✦ Circular Drip Credit Earned");
        creditTitle.setFont(ThemeConfig.UI_BOLD);
        creditTitle.setForeground(ThemeConfig.GOLD);
        creditTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(creditTitle);

        card.add(Box.createVerticalStrut(6));

        JLabel creditAmt = new JLabel(String.format("₹%,.2f  (70%% of order value)",
                order.getBuybackCredit()));
        creditAmt.setFont(new Font("Georgia", Font.BOLD, 18));
        creditAmt.setForeground(ThemeConfig.DEEP_BLACK);
        creditAmt.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(creditAmt);

        root.add(card);

        root.add(Box.createVerticalStrut(28));

        // ── Dispatch button ───────────────────────────────────────────────
        JButton dispatchBtn = LoginView.luxuryButton("START CURATING YOUR DRIP →");
        dispatchBtn.addActionListener(e -> {
            dispose();
            OrderController.startDispatcher(
                    (JFrame) SwingUtilities.getWindowAncestor(getParent()), order);
        });
        root.add(dispatchBtn);

        root.add(Box.createVerticalStrut(14));

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(ThemeConfig.SMALL_FONT);
        closeBtn.setFocusPainted(false);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dispose());
        root.add(closeBtn);
    }

    private void addRow(JPanel panel, String key, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ThemeConfig.SOFT_GREY);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel k = new JLabel(key);
        k.setFont(ThemeConfig.UI_FONT);
        k.setForeground(Color.GRAY);

        JLabel v = new JLabel(value);
        v.setFont(ThemeConfig.UI_BOLD);
        v.setForeground(ThemeConfig.DEEP_BLACK);

        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        panel.add(row);
        panel.add(Box.createVerticalStrut(6));
    }
}
