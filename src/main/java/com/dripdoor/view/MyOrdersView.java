package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.OrderController;
import com.dripdoor.model.CartItem;
import com.dripdoor.model.Order;
import com.dripdoor.service.AuthService;
import com.dripdoor.service.FirebaseService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * My Orders page — Flipkart-style list on the left,
 * order detail panel on the right when an order is clicked.
 */
public class MyOrdersView extends JPanel {

    private static final int   LIST_WIDTH   = 400;
    private static final Color PENDING_FG   = new Color(0x85, 0x65, 0x04);
    private static final Color CURATING_FG  = new Color(0x15, 0x65, 0xC0);
    private static final Color DISPATCH_FG  = new Color(0x00, 0x4B, 0x8D);
    private static final Color DELIVERED_FG = new Color(0x15, 0x55, 0x24);
    private static final Color CANCEL_FG    = new Color(0xC6, 0x28, 0x28);

    private JPanel      listPanel;
    private JPanel      detailContent;
    private JScrollPane detailScroll;
    private Order       selectedOrder = null;

    public MyOrdersView() {
        setBackground(ThemeConfig.SOFT_GREY);
        setLayout(new BorderLayout());
        initUI();
        // Attach real-time Firebase listener once, at construction time
        // (not inside refresh() which would re-attach on every call)
        if (AuthService.isLoggedIn()) {
            FirebaseService.attachOrderStatusListener(
                AuthService.getCurrentUser().getUid(), this::refresh);
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConfig.WHITE);
        header.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel titleBlock = new JPanel();
        titleBlock.setBackground(ThemeConfig.WHITE);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("My Orders");
        title.setFont(new Font("Georgia", Font.BOLD, 26));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        titleBlock.add(title);
        JLabel sub = new JLabel("Track your deliveries");
        sub.setFont(new Font("Georgia", Font.ITALIC, 12));
        sub.setForeground(ThemeConfig.GOLD);
        titleBlock.add(sub);
        header.add(titleBlock, BorderLayout.WEST);

        // Refresh button with spinner icon
        JButton refreshBtn = new JButton() {
            private Timer spinTimer;
            private int   spinAngle = 0;
            private boolean spinning = false;

            {
                setText("  Refresh");
                setFont(new Font("Arial", Font.BOLD, 12));
                setForeground(ThemeConfig.GOLD);
                setBackground(ThemeConfig.WHITE);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeConfig.GOLD, 1, true),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)));
                setFocusPainted(false);
                setContentAreaFilled(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setIconTextGap(6);
                updateIcon(0);

                spinTimer = new Timer(30, ev -> {
                    spinAngle = (spinAngle + 12) % 360;
                    updateIcon(spinAngle);
                    repaint();
                });

                addActionListener(e -> {
                    if (!spinning) {
                        spinning = true;
                        setEnabled(false);
                        spinTimer.start();
                        new SwingWorker<Void, Void>() {
                            protected Void doInBackground() {
                                // slight delay so spinner is visible
                                try { Thread.sleep(600); } catch (InterruptedException ignored) {}
                                return null;
                            }
                            protected void done() {
                                refresh();
                                spinTimer.stop();
                                spinning = false;
                                spinAngle = 0;
                                updateIcon(0);
                                setEnabled(true);
                            }
                        }.execute();
                    }
                });
            }

            private void updateIcon(int angle) {
                setIcon(new Icon() {
                    public int getIconWidth()  { return 14; }
                    public int getIconHeight() { return 14; }
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(ThemeConfig.GOLD);
                        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND));
                        // Draw arc that rotates when spinning
                        Arc2D arc = new Arc2D.Float(x, y, 12, 12, angle, 300, Arc2D.OPEN);
                        g2.draw(arc);
                        // Arrowhead at end of arc
                        double radEnd = Math.toRadians(angle);
                        int ax = x + 6 + (int)(6 * Math.cos(radEnd));
                        int ay = y + 6 - (int)(6 * Math.sin(radEnd));
                        g2.fillOval(ax - 2, ay - 2, 4, 4);
                        g2.dispose();
                    }
                });
            }
        };
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Split pane
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(1);
        split.setDividerLocation(LIST_WIDTH);
        split.setBackground(ThemeConfig.BORDER);
        split.setBorder(null);
        split.setResizeWeight(0.0);

        // Left: order list
        listPanel = new JPanel();
        listPanel.setBackground(ThemeConfig.WHITE);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane listScroll = new JScrollPane(listPanel);
        listScroll.setBorder(null);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        listScroll.getViewport().setBackground(ThemeConfig.WHITE);
        split.setLeftComponent(listScroll);

        // Right: detail
        detailContent = new JPanel();
        detailContent.setBackground(ThemeConfig.SOFT_GREY);
        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setBorder(new EmptyBorder(20, 20, 20, 20));
        detailScroll = new JScrollPane(detailContent);
        detailScroll.setBorder(null);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        detailScroll.getViewport().setBackground(ThemeConfig.SOFT_GREY);
        split.setRightComponent(detailScroll);

        add(split, BorderLayout.CENTER);
        showPlaceholder();
    }

    // ── Refresh ───────────────────────────────────────────────────────────

    public void refresh() {
        listPanel.removeAll();
        List<Order> orders = OrderController.getSessionOrders();

        if (orders.isEmpty()) {
            JLabel empty = new JLabel(
                "<html><center><br><br>No orders yet.<br>"
                + "<span style='color:#999;font-size:11px;'>Place an order from My Cart.</span>"
                + "</center></html>", SwingConstants.CENTER);
            empty.setFont(new Font("Georgia", Font.ITALIC, 14));
            empty.setForeground(new Color(0x77, 0x77, 0x77));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(40));
            listPanel.add(empty);
        } else {
            for (int i = orders.size() - 1; i >= 0; i--) {
                listPanel.add(buildOrderRow(orders.get(i)));
                JPanel div = new JPanel();
                div.setBackground(ThemeConfig.BORDER);
                div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                div.setPreferredSize(new Dimension(1, 1));
                listPanel.add(div);
            }
        }

        listPanel.revalidate();
        listPanel.repaint();

        // Re-render detail if an order is selected
        if (selectedOrder != null) {
            orders.stream()
                .filter(o -> o.getOrderId().equals(selectedOrder.getOrderId()))
                .findFirst()
                .ifPresent(this::showDetail);
        }

    }

    @Override
    public void removeNotify() {
        if (AuthService.isLoggedIn())
            FirebaseService.detachOrderStatusListener(
                AuthService.getCurrentUser().getUid());
        super.removeNotify();
    }

    // ── Order list row ─────────────────────────────────────────────────────

    private JPanel buildOrderRow(Order order) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(ThemeConfig.WHITE);
        row.setBorder(new EmptyBorder(14, 18, 14, 18));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Left: item name + order id
        JPanel left = new JPanel();
        left.setBackground(ThemeConfig.WHITE);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        String firstName = order.getItems().isEmpty() ? "\u2014"
            : order.getItems().get(0).getProduct().getName();
        if (firstName.length() > 30) firstName = firstName.substring(0, 28) + "\u2026";
        JLabel nameLabel = new JLabel(firstName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        nameLabel.setForeground(ThemeConfig.DEEP_BLACK);
        left.add(nameLabel);

        if (order.getItems().size() > 1) {
            JLabel more = new JLabel("+" + (order.getItems().size() - 1) + " more item(s)");
            more.setFont(new Font("Arial", Font.PLAIN, 11));
            more.setForeground(Color.GRAY);
            left.add(more);
        }
        left.add(Box.createVerticalStrut(3));

        JLabel idLabel = new JLabel("Order #" + order.getOrderId());
        idLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        idLabel.setForeground(Color.GRAY);
        left.add(idLabel);

        // Right: colored dot + status + date
        JPanel right = new JPanel();
        right.setBackground(ThemeConfig.WHITE);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        Color fg = statusFg(order.getStatus());
        JPanel dotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        dotRow.setBackground(ThemeConfig.WHITE);
        JLabel dot = new JLabel("\u25cf");
        dot.setFont(new Font("Arial", Font.PLAIN, 10));
        dot.setForeground(fg);
        JLabel statusLbl = new JLabel(statusText(order.getStatus()));
        statusLbl.setFont(new Font("Arial", Font.BOLD, 12));
        statusLbl.setForeground(fg);
        dotRow.add(dot);
        dotRow.add(statusLbl);
        right.add(dotRow);

        JLabel dateLbl = new JLabel(order.getPlacedAt()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        dateLbl.setFont(new Font("Arial", Font.PLAIN, 10));
        dateLbl.setForeground(Color.GRAY);
        dateLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(dateLbl);

        row.add(left,  BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);

        // Hover / click
        MouseAdapter ma = new MouseAdapter() {
            Color selectedBg() {
                return order == selectedOrder
                    ? new Color(0xFF, 0xF8, 0xE8)
                    : ThemeConfig.WHITE;
            }
            public void mouseEntered(MouseEvent e) { tint(new Color(0xFA, 0xF8, 0xF2)); }
            public void mouseExited(MouseEvent e)  { tint(selectedBg()); }
            public void mouseClicked(MouseEvent e) {
                selectedOrder = order;
                showDetail(order);
                tint(new Color(0xFF, 0xF8, 0xE8));
            }
            void tint(Color c) {
                row.setBackground(c); left.setBackground(c);
                right.setBackground(c); dotRow.setBackground(c);
            }
        };
        row.addMouseListener(ma);

        return row;
    }

    // ── Order detail (right panel) ─────────────────────────────────────────

    private void showPlaceholder() {
        detailContent.removeAll();
        JLabel hint = new JLabel(
            "<html><center><br><br>Select an order to view details</center></html>",
            SwingConstants.CENTER);
        hint.setFont(new Font("Georgia", Font.ITALIC, 14));
        hint.setForeground(new Color(0xAA, 0xAA, 0xAA));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailContent.add(hint);
        detailContent.revalidate();
        detailContent.repaint();
    }

    private void showDetail(Order order) {
        detailContent.removeAll();

        // ── Status timeline card ───────────────────────────────────────────
        JPanel timelineCard = card();
        JLabel orderTitle = new JLabel("Order #" + order.getOrderId());
        orderTitle.setFont(new Font("Georgia", Font.BOLD, 15));
        orderTitle.setForeground(ThemeConfig.DEEP_BLACK);
        timelineCard.add(orderTitle);
        timelineCard.add(Box.createVerticalStrut(3));

        String placedStr = order.getPlacedAt()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
        JLabel placedOn = new JLabel("Placed on  " + placedStr);
        placedOn.setFont(new Font("Arial", Font.PLAIN, 11));
        placedOn.setForeground(Color.GRAY);
        timelineCard.add(placedOn);
        timelineCard.add(Box.createVerticalStrut(16));
        timelineCard.add(buildTimeline(order));
        detailContent.add(timelineCard);
        detailContent.add(Box.createVerticalStrut(12));

        // ── Items card ────────────────────────────────────────────────────
        JPanel itemsCard = card();
        sectionLabel(itemsCard, "ITEMS ORDERED");
        itemsCard.add(Box.createVerticalStrut(8));

        for (CartItem item : order.getItems()) {
            JPanel iRow = new JPanel(new BorderLayout(8, 0));
            iRow.setBackground(ThemeConfig.WHITE);
            iRow.setBorder(new EmptyBorder(6, 0, 6, 0));
            iRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            JPanel info = new JPanel();
            info.setBackground(ThemeConfig.WHITE);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            JLabel n = new JLabel(item.getProduct().getName());
            n.setFont(new Font("Arial", Font.BOLD, 13));
            n.setForeground(ThemeConfig.DEEP_BLACK);
            info.add(n);
            JLabel cat = new JLabel(item.getProduct().getCategory()
                + "  \u00b7  Qty: " + item.getQuantity());
            cat.setFont(new Font("Arial", Font.PLAIN, 11));
            cat.setForeground(Color.GRAY);
            info.add(cat);

            JLabel price = new JLabel("\u20b9" + String.format("%,.0f", item.lineTotal()));
            price.setFont(new Font("Georgia", Font.BOLD, 13));
            price.setForeground(ThemeConfig.DEEP_BLACK);

            iRow.add(info,  BorderLayout.CENTER);
            iRow.add(price, BorderLayout.EAST);
            itemsCard.add(iRow);

            JPanel divLine = new JPanel();
            divLine.setBackground(ThemeConfig.BORDER);
            divLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            itemsCard.add(divLine);
        }

        itemsCard.add(Box.createVerticalStrut(8));
        addKeyValue(itemsCard, "Order Total",
            "\u20b9" + String.format("%,.2f", order.getTotalAmount()),
            ThemeConfig.DEEP_BLACK, new Font("Georgia", Font.BOLD, 14));
        itemsCard.add(Box.createVerticalStrut(2));

        // Circular Drip Credit row with custom diamond icon
        JPanel creditRow = new JPanel(new BorderLayout());
        creditRow.setBackground(ThemeConfig.WHITE);
        creditRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JPanel creditKey = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        creditKey.setBackground(ThemeConfig.WHITE);

        // Custom diamond icon label (avoids unicode font rendering issues)
        JLabel diamondIcon = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeConfig.GOLD);
                int[] xp = {7, 14, 7, 0};
                int[] yp = {0,  7, 14, 7};
                g2.fillPolygon(xp, yp, 4);
                g2.dispose();
            }
        };
        diamondIcon.setPreferredSize(new Dimension(14, 14));

        JLabel creditKeyLabel = new JLabel("Circular Drip Credit Earned");
        creditKeyLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        creditKeyLabel.setForeground(Color.GRAY);

        creditKey.add(diamondIcon);
        creditKey.add(creditKeyLabel);

        JLabel creditVal = new JLabel("\u20b9" + String.format("%,.2f", order.getBuybackCredit()));
        creditVal.setFont(new Font("Arial", Font.BOLD, 12));
        creditVal.setForeground(ThemeConfig.GOLD);

        creditRow.add(creditKey, BorderLayout.WEST);
        creditRow.add(creditVal, BorderLayout.EAST);
        itemsCard.add(creditRow);

        detailContent.add(itemsCard);
        detailContent.add(Box.createVerticalStrut(12));

        // ── Delivery address card ──────────────────────────────────────────
        JPanel addrCard = card();
        sectionLabel(addrCard, "DELIVERY ADDRESS");
        addrCard.add(Box.createVerticalStrut(8));

        String addr = order.getFormattedAddress();
        if ("No address provided".equals(addr)) {
            JLabel noAddr = new JLabel("No address on record for this order.");
            noAddr.setFont(new Font("Arial", Font.ITALIC, 12));
            noAddr.setForeground(Color.GRAY);
            addrCard.add(noAddr);
        } else {
            for (String line : addr.split("\n")) {
                JLabel al = new JLabel(line);
                al.setFont(new Font("Arial", Font.PLAIN, 13));
                al.setForeground(ThemeConfig.DEEP_BLACK);
                addrCard.add(al);
                addrCard.add(Box.createVerticalStrut(2));
            }
        }

        detailContent.add(addrCard);
        detailContent.add(Box.createVerticalStrut(20));

        detailContent.revalidate();
        detailContent.repaint();
        SwingUtilities.invokeLater(() ->
            detailScroll.getVerticalScrollBar().setValue(0));
    }

    // ── Status timeline ────────────────────────────────────────────────────

    private JPanel buildTimeline(Order order) {
        Order.Status current = order.getStatus();
        boolean cancelled = (current == Order.Status.CANCELLED);

        JPanel tl = new JPanel();
        tl.setBackground(ThemeConfig.WHITE);
        tl.setLayout(new BoxLayout(tl, BoxLayout.Y_AXIS));
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (cancelled) {
            tl.add(step(true, false, "Order Placed",
                order.getPlacedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
            tl.add(connector(true));
            tl.add(stepCancelled());
        } else {
            Order.Status[] stages = {
                Order.Status.PENDING, Order.Status.CURATING,
                Order.Status.DISPATCHED, Order.Status.DELIVERED
            };
            String[] labels = {"Order Placed", "Being Curated", "Dispatched", "Delivered"};
            String[] subs = {
                order.getPlacedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                "Preparing your luxury items",
                "On the way to you",
                "Successfully delivered"
            };

            int idx = -1;
            for (int i = 0; i < stages.length; i++)
                if (stages[i] == current) { idx = i; break; }

            for (int i = 0; i < stages.length; i++) {
                boolean done   = (i <= idx);
                boolean active = (i == idx);
                tl.add(step(done, active, labels[i], subs[i]));
                if (i < stages.length - 1) tl.add(connector(done && i < idx));
            }
        }
        return tl;
    }

    private JPanel step(boolean done, boolean active, String label, String sub) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(ThemeConfig.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel circle = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (done) {
                    g2.setColor(new Color(0x2E, 0x7D, 0x32));
                    g2.fillOval(2, 2, 18, 18);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Arial", Font.BOLD, 10));
                    g2.drawString("\u2713", 5, 15);
                } else {
                    g2.setColor(new Color(0xCC, 0xCC, 0xCC));
                    g2.drawOval(2, 2, 18, 18);
                }
                g2.dispose();
            }
        };
        circle.setPreferredSize(new Dimension(22, 22));

        JPanel text = new JPanel();
        text.setBackground(ThemeConfig.WHITE);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel(label);
        l1.setFont(new Font("Arial", done ? Font.BOLD : Font.PLAIN, 13));
        l1.setForeground(done ? ThemeConfig.DEEP_BLACK : Color.GRAY);
        text.add(l1);
        JLabel l2 = new JLabel(sub);
        l2.setFont(new Font("Arial", Font.PLAIN, 11));
        l2.setForeground(active ? new Color(0x2E, 0x7D, 0x32) : Color.GRAY);
        text.add(l2);

        row.add(circle, BorderLayout.WEST);
        row.add(text,   BorderLayout.CENTER);
        return row;
    }

    private JPanel stepCancelled() {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(ThemeConfig.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel circle = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CANCEL_FG);
                g2.fillOval(2, 2, 18, 18);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.drawString("\u2715", 5, 15);
                g2.dispose();
            }
        };
        circle.setPreferredSize(new Dimension(22, 22));

        JPanel text = new JPanel();
        text.setBackground(ThemeConfig.WHITE);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel l1 = new JLabel("Cancelled");
        l1.setFont(new Font("Arial", Font.BOLD, 13));
        l1.setForeground(CANCEL_FG);
        text.add(l1);
        JLabel l2 = new JLabel("Your order was cancelled");
        l2.setFont(new Font("Arial", Font.PLAIN, 11));
        l2.setForeground(Color.GRAY);
        text.add(l2);

        row.add(circle, BorderLayout.WEST);
        row.add(text,   BorderLayout.CENTER);
        return row;
    }

    private JPanel connector(boolean filled) {
        JPanel p = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(filled ? new Color(0x2E, 0x7D, 0x32) : new Color(0xCC, 0xCC, 0xCC));
                g2.fillRect(11, 0, 2, getHeight());
                g2.dispose();
            }
        };
        p.setBackground(ThemeConfig.WHITE);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        p.setPreferredSize(new Dimension(22, 14));
        return p;
    }

    // ── Layout helpers ─────────────────────────────────────────────────────

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(ThemeConfig.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private void sectionLabel(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 10));
        l.setForeground(Color.GRAY);
        panel.add(l);
    }

    private void addKeyValue(JPanel panel, String key, String val,
                             Color valColor, Font valFont) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ThemeConfig.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel k = new JLabel(key);
        k.setFont(new Font("Arial", Font.PLAIN, 12));
        k.setForeground(Color.GRAY);
        JLabel v = new JLabel(val);
        v.setFont(valFont);
        v.setForeground(valColor);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        panel.add(row);
    }

    private Color statusFg(Order.Status s) {
        return switch (s) {
            case PENDING    -> PENDING_FG;
            case CURATING   -> CURATING_FG;
            case DISPATCHED -> DISPATCH_FG;
            case DELIVERED  -> DELIVERED_FG;
            case CANCELLED  -> CANCEL_FG;
        };
    }

    private String statusText(Order.Status s) {
        return switch (s) {
            case PENDING    -> "Order Placed";
            case CURATING   -> "Being Curated";
            case DISPATCHED -> "Dispatched";
            case DELIVERED  -> "Delivered";
            case CANCELLED  -> "Cancelled";
        };
    }
}
