package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.ProductController;
import com.dripdoor.model.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive Outfit Builder — modal dialog showing a mannequin silhouette
 * with labelled drop zones for each accessory category.
 *
 * Flow:
 *  1. Dialog opens with the triggering product already slotted.
 *  2. Each category zone (Necklace, Ring, Bracelet, Earrings, Watch) shows
 *     the assigned product name or an empty prompt.
 *  3. A scrollable product panel on the right lets the user click any item
 *     to assign it to its category slot on the mannequin.
 *  4. "Add All to Cart" queues every assigned product.
 */
public class OutfitBuilderView extends JDialog {

    // ── Slot geometry on the mannequin canvas (x, y, w, h) ──────────────
    // Tuned to a 320-wide silhouette centred in a 400-wide canvas
    private static final Map<String, Rectangle> SLOT_BOUNDS = new HashMap<>();
    static {
        SLOT_BOUNDS.put("Necklace",  new Rectangle(145, 148, 110, 36));
        SLOT_BOUNDS.put("Earrings",  new Rectangle(100, 118, 52, 32));
        SLOT_BOUNDS.put("Ring",      new Rectangle(72,  290, 68, 30));
        SLOT_BOUNDS.put("Bracelet",  new Rectangle(256, 270, 80, 30));
        SLOT_BOUNDS.put("Watch",     new Rectangle(258, 305, 80, 30));
    }

    // ── State ─────────────────────────────────────────────────────────────
    private final Map<String, Product> slots = new HashMap<>();
    private MannequinCanvas mannequin;
    private JPanel          slotLabelPanel;

    public OutfitBuilderView(Frame owner, Product initial) {
        super(owner, "Outfit Builder", true);
        if (initial != null) slots.put(initial.getCategory(), initial);
        initUI();
        setSize(860, 640);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UI
    // ─────────────────────────────────────────────────────────────────────

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConfig.WHITE);
        setContentPane(root);

        // ── Header ───────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConfig.DEEP_BLACK);
        header.setBorder(new EmptyBorder(18, 28, 18, 28));

        JPanel titleBlock = new JPanel();
        titleBlock.setBackground(ThemeConfig.DEEP_BLACK);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("OUTFIT BUILDER");
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setForeground(ThemeConfig.GOLD);
        titleBlock.add(title);

        JLabel sub = new JLabel("Visualise your combination before you buy");
        sub.setFont(new Font("Georgia", Font.ITALIC, 11));
        sub.setForeground(new Color(0xAA, 0xAA, 0xAA));
        titleBlock.add(sub);

        header.add(titleBlock, BorderLayout.WEST);

        JButton close = new JButton("✕");
        close.setFont(new Font("Arial", Font.PLAIN, 14));
        close.setForeground(new Color(0x99, 0x99, 0x99));
        close.setBackground(ThemeConfig.DEEP_BLACK);
        close.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(hoverGold(close));
        close.addActionListener(e -> dispose());
        header.add(close, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ── Centre: mannequin + slot labels ───────────────────────────────
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(new Color(0xF5, 0xF0, 0xE8));

        mannequin = new MannequinCanvas();
        mannequin.setPreferredSize(new Dimension(400, 530));
        centre.add(mannequin, BorderLayout.CENTER);

        root.add(centre, BorderLayout.CENTER);

        // ── Right: catalogue picker ────────────────────────────────────────
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(ThemeConfig.WHITE);
        right.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ThemeConfig.BORDER));
        right.setPreferredSize(new Dimension(300, 0));

        JLabel pickTitle = new JLabel("Select Pieces");
        pickTitle.setFont(new Font("Georgia", Font.BOLD, 15));
        pickTitle.setForeground(ThemeConfig.DEEP_BLACK);
        pickTitle.setBorder(new EmptyBorder(18, 20, 12, 20));
        right.add(pickTitle, BorderLayout.NORTH);

        JPanel catalogue = new JPanel();
        catalogue.setLayout(new BoxLayout(catalogue, BoxLayout.Y_AXIS));
        catalogue.setBackground(ThemeConfig.WHITE);
        catalogue.setBorder(new EmptyBorder(0, 12, 12, 12));

        List<Product> products = ProductController.getCatalogue();
        for (Product p : products) {
            catalogue.add(buildPickerRow(p));
            catalogue.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(catalogue);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        right.add(scroll, BorderLayout.CENTER);

        // ── Bottom: total + CTA ───────────────────────────────────────────
        JPanel bottomRight = buildBottomBar();
        right.add(bottomRight, BorderLayout.SOUTH);

        root.add(right, BorderLayout.EAST);
    }

    /** Single row in the picker panel. */
    private JPanel buildPickerRow(Product p) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(ThemeConfig.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ThemeConfig.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Category colour dot
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(categoryColor(p.getCategory()));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(10, 10));
        dot.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(ThemeConfig.WHITE);
        left.add(dot);

        JPanel text = new JPanel();
        text.setBackground(ThemeConfig.WHITE);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(p.getName());
        name.setFont(new Font("Georgia", Font.PLAIN, 12));
        name.setForeground(ThemeConfig.DEEP_BLACK);
        text.add(name);

        JLabel cat = new JLabel(p.getCategory() + "  ·  ₹" + String.format("%,.0f", p.getPrice()));
        cat.setFont(new Font("Arial", Font.PLAIN, 10));
        cat.setForeground(new Color(0x88, 0x88, 0x88));
        text.add(cat);

        row.add(text, BorderLayout.CENTER);

        JButton addBtn = new JButton(slots.containsValue(p) ? "✓" : "+");
        addBtn.setFont(new Font("Arial", Font.BOLD, 13));
        addBtn.setForeground(slots.containsValue(p) ? ThemeConfig.GOLD : ThemeConfig.DEEP_BLACK);
        addBtn.setBackground(ThemeConfig.WHITE);
        addBtn.setBorder(new LineBorder(ThemeConfig.BORDER, 1, true));
        addBtn.setPreferredSize(new Dimension(34, 34));
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.add(addBtn, BorderLayout.EAST);

        MouseAdapter hover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                row.setBackground(new Color(0xFD, 0xF8, 0xEC));
                text.setBackground(new Color(0xFD, 0xF8, 0xEC));
                left.setBackground(new Color(0xFD, 0xF8, 0xEC));
            }
            public void mouseExited(MouseEvent e) {
                row.setBackground(ThemeConfig.WHITE);
                text.setBackground(ThemeConfig.WHITE);
                left.setBackground(ThemeConfig.WHITE);
            }
            public void mouseClicked(MouseEvent e) { slotProduct(p, addBtn); }
        };
        row.addMouseListener(hover);
        addBtn.addActionListener(e -> slotProduct(p, addBtn));

        return row;
    }

    /** Assign product to its category slot, refresh canvas. */
    private void slotProduct(Product p, JButton btn) {
        if (slots.get(p.getCategory()) == p) {
            // Toggle off
            slots.remove(p.getCategory());
            btn.setText("+");
            btn.setForeground(ThemeConfig.DEEP_BLACK);
        } else {
            slots.put(p.getCategory(), p);
            btn.setText("✓");
            btn.setForeground(ThemeConfig.GOLD);
        }
        mannequin.repaint();
        refreshBottomBar();
    }

    private JPanel bottomBar;

    private JPanel buildBottomBar() {
        bottomBar = new JPanel(new BorderLayout(0, 8));
        bottomBar.setBackground(ThemeConfig.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeConfig.BORDER),
                new EmptyBorder(14, 18, 18, 18)));
        refreshBottomBar();
        return bottomBar;
    }

    private void refreshBottomBar() {
        if (bottomBar == null) return;
        bottomBar.removeAll();

        double total = slots.values().stream().mapToDouble(Product::getPrice).sum();

        JPanel totRow = new JPanel(new BorderLayout());
        totRow.setBackground(ThemeConfig.WHITE);
        JLabel totLabel = new JLabel("Combination Total");
        totLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        totLabel.setForeground(new Color(0x88, 0x88, 0x88));
        totRow.add(totLabel, BorderLayout.WEST);
        JLabel totAmt = new JLabel(String.format("₹%,.0f", total));
        totAmt.setFont(new Font("Georgia", Font.BOLD, 15));
        totAmt.setForeground(ThemeConfig.DEEP_BLACK);
        totRow.add(totAmt, BorderLayout.EAST);
        bottomBar.add(totRow, BorderLayout.NORTH);

        JButton addAll = new JButton(slots.isEmpty()
                ? "SELECT PIECES ABOVE"
                : "ADD " + slots.size() + " PIECE" + (slots.size() > 1 ? "S" : "") + " TO CART");
        addAll.setFont(new Font("Arial", Font.BOLD, 11));
        addAll.setBackground(slots.isEmpty() ? ThemeConfig.BORDER : ThemeConfig.DEEP_BLACK);
        addAll.setForeground(slots.isEmpty() ? new Color(0x99, 0x99, 0x99) : ThemeConfig.WHITE);
        addAll.setFocusPainted(false);
        addAll.setBorder(new EmptyBorder(12, 16, 12, 16));
        addAll.setCursor(Cursor.getPredefinedCursor(slots.isEmpty()
                ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        addAll.setEnabled(!slots.isEmpty());
        addAll.setOpaque(true);

        if (!slots.isEmpty()) {
            addAll.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    addAll.setBackground(ThemeConfig.GOLD);
                    addAll.setForeground(ThemeConfig.DEEP_BLACK);
                }
                public void mouseExited(MouseEvent e) {
                    addAll.setBackground(ThemeConfig.DEEP_BLACK);
                    addAll.setForeground(ThemeConfig.WHITE);
                }
            });
            addAll.addActionListener(e -> {
                slots.values().forEach(p -> ProductController.addToCart(p, 1));
                showToast(slots.size() + " piece" + (slots.size() > 1 ? "s" : "") + " added to cart");
                dispose();
            });
        }
        bottomBar.add(addAll, BorderLayout.SOUTH);
        bottomBar.revalidate();
        bottomBar.repaint();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Mannequin Canvas
    // ─────────────────────────────────────────────────────────────────────

    private class MannequinCanvas extends JPanel {

        MannequinCanvas() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            drawBackground(g2);
            drawSilhouette(g2, cx);
            drawSlots(g2, cx);
            drawLegend(g2);

            g2.dispose();
        }

        private void drawBackground(Graphics2D g2) {
            // Subtle linen-toned gradient
            GradientPaint bg = new GradientPaint(
                    0, 0, new Color(0xF5, 0xF0, 0xE8),
                    0, getHeight(), new Color(0xEB, 0xE4, 0xD4));
            g2.setPaint(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Decorative corner ornament lines
            g2.setColor(new Color(0xD4, 0xAF, 0x37, 40));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(20, 20, 60, 20); g2.drawLine(20, 20, 20, 60);
            g2.drawLine(getWidth()-20, 20, getWidth()-60, 20); g2.drawLine(getWidth()-20, 20, getWidth()-20, 60);
            g2.drawLine(20, getHeight()-20, 60, getHeight()-20); g2.drawLine(20, getHeight()-20, 20, getHeight()-60);
            g2.drawLine(getWidth()-20, getHeight()-20, getWidth()-60, getHeight()-20);
            g2.drawLine(getWidth()-20, getHeight()-20, getWidth()-20, getHeight()-60);
        }

        private void drawSilhouette(Graphics2D g2, int cx) {
            g2.setColor(new Color(0x1A, 0x1A, 0x1A, 200));

            // ── Head ─────────────────────────────────────────────────────
            g2.fillOval(cx - 28, 30, 56, 62);

            // ── Neck ─────────────────────────────────────────────────────
            g2.fillRoundRect(cx - 11, 90, 22, 30, 8, 8);

            // ── Shoulders & torso ─────────────────────────────────────────
            // Left shoulder curve
            Path2D torso = new Path2D.Double();
            torso.moveTo(cx - 11, 120);
            torso.curveTo(cx - 25, 120, cx - 70, 128, cx - 78, 155); // shoulder L
            torso.lineTo(cx - 72, 310);
            torso.curveTo(cx - 68, 330, cx - 42, 338, cx - 22, 338);
            torso.lineTo(cx + 22, 338);
            torso.curveTo(cx + 42, 338, cx + 68, 330, cx + 72, 310);
            torso.lineTo(cx + 78, 155); // shoulder R
            torso.curveTo(cx + 70, 128, cx + 25, 120, cx + 11, 120);
            torso.closePath();
            g2.fill(torso);

            // ── Arms ──────────────────────────────────────────────────────
            // Left arm
            Path2D armL = new Path2D.Double();
            armL.moveTo(cx - 78, 155);
            armL.curveTo(cx - 92, 175, cx - 96, 220, cx - 90, 270);
            armL.curveTo(cx - 88, 285, cx - 80, 295, cx - 72, 300);
            armL.lineTo(cx - 66, 290);
            armL.curveTo(cx - 72, 285, cx - 76, 275, cx - 76, 265);
            armL.curveTo(cx - 80, 220, cx - 76, 180, cx - 64, 160);
            armL.closePath();
            g2.fill(armL);

            // Right arm
            Path2D armR = new Path2D.Double();
            armR.moveTo(cx + 78, 155);
            armR.curveTo(cx + 92, 175, cx + 96, 220, cx + 90, 270);
            armR.curveTo(cx + 88, 285, cx + 80, 295, cx + 72, 300);
            armR.lineTo(cx + 66, 290);
            armR.curveTo(cx + 72, 285, cx + 76, 275, cx + 76, 265);
            armR.curveTo(cx + 80, 220, cx + 76, 180, cx + 64, 160);
            armR.closePath();
            g2.fill(armR);

            // ── Hands ─────────────────────────────────────────────────────
            // Left hand
            g2.fillOval(cx - 78, 295, 22, 28);
            // Right hand
            g2.fillOval(cx + 56, 295, 22, 28);

            // ── Legs ──────────────────────────────────────────────────────
            // Left leg
            Path2D legL = new Path2D.Double();
            legL.moveTo(cx - 22, 338);
            legL.lineTo(cx - 26, 490);
            legL.lineTo(cx - 8, 490);
            legL.lineTo(cx - 4, 338);
            legL.closePath();
            g2.fill(legL);

            // Right leg
            Path2D legR = new Path2D.Double();
            legR.moveTo(cx + 22, 338);
            legR.lineTo(cx + 26, 490);
            legR.lineTo(cx + 8, 490);
            legR.lineTo(cx + 4, 338);
            legR.closePath();
            g2.fill(legR);

            // ── Feet ──────────────────────────────────────────────────────
            g2.fillRoundRect(cx - 28, 486, 24, 12, 6, 6);
            g2.fillRoundRect(cx + 4, 486, 24, 12, 6, 6);
        }

        private void drawSlots(Graphics2D g2, int cx) {
            // Offset all slots relative to centre
            int offsetX = cx - 200; // slots designed around cx=200

            for (Map.Entry<String, Rectangle> entry : SLOT_BOUNDS.entrySet()) {
                String cat = entry.getKey();
                Rectangle r = entry.getValue();
                int x = r.x + offsetX;
                int y = r.y;

                Product assigned = slots.get(cat);
                boolean filled   = assigned != null;

                // Slot background
                g2.setColor(filled
                        ? new Color(0xD4, 0xAF, 0x37, 220)
                        : new Color(0xFF, 0xFF, 0xFF, 160));
                g2.fillRoundRect(x, y, r.width, r.height, 10, 10);

                // Slot border
                g2.setColor(filled ? ThemeConfig.GOLD : new Color(0xD4, 0xAF, 0x37, 120));
                g2.setStroke(new BasicStroke(filled ? 1.8f : 1f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, filled ? null : new float[]{4, 3}, 0));
                g2.drawRoundRect(x, y, r.width, r.height, 10, 10);
                g2.setStroke(new BasicStroke(1f));

                // Label
                g2.setColor(filled ? ThemeConfig.DEEP_BLACK : new Color(0x88, 0x77, 0x44));
                g2.setFont(new Font("Georgia", Font.PLAIN, 10));
                FontMetrics fm = g2.getFontMetrics();

                String label = filled
                        ? abbreviate(assigned.getName(), r.width - 8)
                        : cat;
                g2.drawString(label,
                        x + (r.width - fm.stringWidth(label)) / 2,
                        y + r.height / 2 + fm.getAscent() / 2 - 2);

                // Pin connector line from slot to body point
                drawConnector(g2, cat, x, y, r, cx, offsetX, filled);
            }
        }

        private void drawConnector(Graphics2D g2, String cat,
                                   int sx, int sy, Rectangle r,
                                   int cx, int offsetX, boolean filled) {
            g2.setColor(new Color(0xD4, 0xAF, 0x37, filled ? 160 : 80));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 0, new float[]{3, 3}, 0));

            // Map each category to its body anchor point
            int ax, ay;
            switch (cat) {
                case "Necklace" -> { ax = cx;       ay = 138; }
                case "Earrings" -> { ax = cx - 27;  ay = 110; }
                case "Ring"     -> { ax = cx - 67;  ay = 300; }
                case "Bracelet" -> { ax = cx + 76;  ay = 272; }
                case "Watch"    -> { ax = cx + 76;  ay = 295; }
                default         -> { return; }
            }
            // Draw from slot edge to anchor
            int slotMidX = sx + r.width / 2;
            int slotMidY = sy + r.height / 2;
            g2.drawLine(slotMidX, slotMidY, ax, ay);
            // Anchor dot
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(filled ? ThemeConfig.GOLD : new Color(0xD4, 0xAF, 0x37, 120));
            g2.fillOval(ax - 3, ay - 3, 7, 7);
        }

        private void drawLegend(Graphics2D g2) {
            // Bottom instruction
            String hint = slots.isEmpty()
                    ? "Click a piece on the right to add it here"
                    : slots.size() + "/" + SLOT_BOUNDS.size() + " slots filled";
            g2.setFont(new Font("Georgia", Font.ITALIC, 11));
            g2.setColor(new Color(0x88, 0x77, 0x44, 180));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint,
                    (getWidth() - fm.stringWidth(hint)) / 2,
                    getHeight() - 16);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static Color categoryColor(String cat) {
        return switch (cat) {
            case "Necklace"  -> new Color(0xD4, 0xAF, 0x37);
            case "Ring"      -> new Color(0x8B, 0x45, 0x13);
            case "Bracelet"  -> new Color(0x2E, 0x8B, 0x57);
            case "Earrings"  -> new Color(0x4B, 0x0E, 0x82);
            case "Watch"     -> new Color(0x1A, 0x6B, 0x9A);
            default          -> Color.GRAY;
        };
    }

    private static String abbreviate(String s, int maxWidth) {
        if (s.length() <= 16) return s;
        return s.substring(0, 14) + "…";
    }

    private MouseAdapter hoverGold(JButton btn) {
        return new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(ThemeConfig.GOLD); }
            public void mouseExited(MouseEvent e)  { btn.setForeground(new Color(0x99,0x99,0x99)); }
        };
    }

    private void showToast(String message) {
        JWindow toast = new JWindow(this);
        toast.setBackground(new Color(0, 0, 0, 0));
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1A, 0x1A, 0x1A, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JLabel check = new JLabel("✓");
        check.setFont(new Font("Arial", Font.BOLD, 14));
        check.setForeground(ThemeConfig.GOLD);
        panel.add(check);
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        lbl.setForeground(Color.WHITE);
        panel.add(lbl);
        toast.add(panel);
        toast.pack();
        Rectangle b = getBounds();
        toast.setLocation(b.x + b.width / 2 - toast.getWidth() / 2,
                          b.y + b.height - toast.getHeight() - 40);
        toast.setVisible(true);
        Timer t = new Timer(2000, e -> toast.dispose());
        t.setRepeats(false);
        t.start();
    }
}
