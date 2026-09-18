package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;

/**
 * A translucent full-window overlay with an animated spinner and message.
 *
 * Usage:
 *   LoadingOverlay overlay = new LoadingOverlay(parentFrame, "Signing in…");
 *   overlay.show();
 *   // … do work on background thread …
 *   overlay.hide();
 */
public class LoadingOverlay extends JPanel {

    private static final int  SPOKE_COUNT  = 12;
    private static final int  SPOKE_LENGTH = 10;
    private static final int  SPOKE_WIDTH  = 3;
    private static final int  DIAMETER     = 36;

    private final Timer animTimer;
    private int angle = 0;

    private final JLabel messageLabel;
    private final JFrame  parent;
    private final JLayeredPane layered;

    public LoadingOverlay(JFrame parent, String message) {
        this.parent  = parent;
        this.layered = parent.getRootPane().getLayeredPane();

        setOpaque(false);
        setLayout(new GridBagLayout());

        // ── Card ──────────────────────────────────────────────────────────
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 235));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.GOLD, 1, true),
                BorderFactory.createEmptyBorder(28, 40, 28, 40)));

        // Spinner canvas
        JPanel spinner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                for (int i = 0; i < SPOKE_COUNT; i++) {
                    float alpha = (float)(i + 1) / SPOKE_COUNT;
                    int   idx   = (angle + i) % SPOKE_COUNT;
                    double rad  = Math.toRadians(idx * (360.0 / SPOKE_COUNT));
                    int x1 = (int)(cx + Math.cos(rad) * (DIAMETER / 2 - SPOKE_LENGTH));
                    int y1 = (int)(cy + Math.sin(rad) * (DIAMETER / 2 - SPOKE_LENGTH));
                    int x2 = (int)(cx + Math.cos(rad) *  DIAMETER / 2);
                    int y2 = (int)(cy + Math.sin(rad) *  DIAMETER / 2);
                    g2.setColor(new Color(
                            ThemeConfig.GOLD.getRed(),
                            ThemeConfig.GOLD.getGreen(),
                            ThemeConfig.GOLD.getBlue(),
                            (int)(alpha * 255)));
                    g2.setStroke(new BasicStroke(SPOKE_WIDTH,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.dispose();
            }
        };
        spinner.setOpaque(false);
        spinner.setPreferredSize(new Dimension(DIAMETER + 10, DIAMETER + 10));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(spinner);

        card.add(Box.createVerticalStrut(14));

        // Message
        messageLabel = new JLabel(message);
        messageLabel.setFont(ThemeConfig.UI_BOLD.deriveFont(13f));
        messageLabel.setForeground(ThemeConfig.DEEP_BLACK);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(messageLabel);

        add(card);

        // ── Timer ─────────────────────────────────────────────────────────
        animTimer = new Timer(80, e -> {
            angle = (angle + 1) % SPOKE_COUNT;
            spinner.repaint();
        });
    }

    /** Shows the overlay over the parent window. */
    public void show() {
        setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(this, JLayeredPane.MODAL_LAYER);
        layered.revalidate();
        layered.repaint();
        animTimer.start();
    }

    /** Hides and removes the overlay. */
    public void hide() {
        animTimer.stop();
        layered.remove(this);
        layered.revalidate();
        layered.repaint();
    }

    /** Updates the spinner message text. */
    public void setMessage(String msg) {
        messageLabel.setText(msg);
    }

    @Override protected void paintComponent(Graphics g) {
        // dim background
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
