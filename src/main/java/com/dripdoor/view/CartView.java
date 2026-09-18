package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.OrderController;
import com.dripdoor.controller.ProductController;
import com.dripdoor.model.CartItem;
import com.dripdoor.model.Order;
import com.dripdoor.util.CircularDripCalculator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class CartView extends JPanel {

    private final MainDashboardView dashboard;
    private JTable            cartTable;
    private DefaultTableModel tableModel;
    private JLabel            totalLabel;
    private JLabel            creditLabel;
    private JLabel            emptyLabel;
    private JScrollPane       scroll;

    public CartView(MainDashboardView dashboard) {
        this.dashboard = dashboard;
        setBackground(ThemeConfig.SOFT_GREY);
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConfig.WHITE);
        header.setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel titleBlock = new JPanel();
        titleBlock.setBackground(ThemeConfig.WHITE);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Cart");
        title.setFont(new Font("Georgia", Font.BOLD, 28));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        titleBlock.add(title);

        JLabel sub = new JLabel("Review your selection");
        sub.setFont(new Font("Georgia", Font.ITALIC, 13));
        sub.setForeground(ThemeConfig.GOLD);
        titleBlock.add(sub);

        header.add(titleBlock, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Empty state
        emptyLabel = new JLabel(
                "<html><center>Your cart is empty.<br><br>"
                + "<span style='font-size:11px;color:#888;'>Browse the Collection to add items.</span></center></html>",
                SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Georgia", Font.ITALIC, 16));
        emptyLabel.setForeground(new Color(0x77, 0x77, 0x77));
        emptyLabel.setVisible(false);

        // Table
        String[] cols = {"Item", "Category", "Unit Price", "Qty", "Line Total", ""};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 5; }
        };

        cartTable = new JTable(tableModel);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 13));
        cartTable.setRowHeight(48);
        cartTable.setShowGrid(false);
        cartTable.setIntercellSpacing(new Dimension(0, 6));
        cartTable.setSelectionBackground(new Color(0xD4, 0xAF, 0x37, 40));
        cartTable.setBackground(ThemeConfig.WHITE);
        cartTable.setFillsViewportHeight(true);

        JTableHeader th = cartTable.getTableHeader();
        th.setFont(new Font("Arial", Font.BOLD, 11));
        th.setBackground(ThemeConfig.SOFT_GREY);
        th.setForeground(new Color(0x55, 0x55, 0x55));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ThemeConfig.BORDER));

        cartTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        cartTable.getColumnModel().getColumn(5).setMaxWidth(90);
        cartTable.getColumnModel().getColumn(5).setCellRenderer(new RemoveBtnRenderer());
        cartTable.getColumnModel().getColumn(5).setCellEditor(
                new RemoveBtnEditor(new JCheckBox(), cartTable, tableModel, this));

        scroll = new JScrollPane(cartTable);
        scroll.setBorder(new EmptyBorder(0,0,0,0));
        scroll.getViewport().setBackground(ThemeConfig.WHITE);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBackground(ThemeConfig.SOFT_GREY);
        tableWrapper.setBorder(new EmptyBorder(16, 24, 0, 24));
        tableWrapper.add(emptyLabel, BorderLayout.NORTH);
        tableWrapper.add(scroll, BorderLayout.CENTER);
        add(tableWrapper, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel();
        footer.setBackground(ThemeConfig.WHITE);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(20, 32, 28, 32));

        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConfig.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        footer.add(sep);
        footer.add(Box.createVerticalStrut(18));

        totalLabel = new JLabel("Total: \u20b90.00");
        totalLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        totalLabel.setForeground(ThemeConfig.DEEP_BLACK);
        totalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        footer.add(totalLabel);

        footer.add(Box.createVerticalStrut(6));

        creditLabel = new JLabel("Circular Drip Credit: \u20b90.00 (70% back)");
        creditLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        creditLabel.setForeground(ThemeConfig.GOLD);
        creditLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        footer.add(creditLabel);

        footer.add(Box.createVerticalStrut(20));
        footer.add(buildCheckoutButton());

        add(footer, BorderLayout.SOUTH);
    }

    private JButton buildCheckoutButton() {
        JButton btn = new JButton("PROCEED TO CHECKOUT");
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setBackground(ThemeConfig.DEEP_BLACK);
        btn.setForeground(ThemeConfig.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(14, 36, 14, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btn.setOpaque(true);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(ThemeConfig.GOLD);
                btn.setForeground(ThemeConfig.DEEP_BLACK);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(ThemeConfig.DEEP_BLACK);
                btn.setForeground(ThemeConfig.WHITE);
            }
        });

        btn.addActionListener(e -> {
            if (ProductController.getCart().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Your cart is empty.",
                        "Empty Cart", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // ── Step 1: Collect delivery address ──────────────────────────
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            AddressDialog addrDlg = new AddressDialog(parentFrame);
            addrDlg.setVisible(true);   // blocks until dialog closes

            if (!addrDlg.isConfirmed()) return;  // user cancelled

            // ── Step 2: Build order in memory, apply address, THEN save ───
            Order order = OrderController.buildOrder();
            if (order == null) return;

            addrDlg.applyToOrder(order);        // address set BEFORE Firebase save
            OrderController.finaliseOrder(order); // now persist with address

            // ── Step 3: Show confirmation dialog ──────────────────────────
            CheckoutView cv = new CheckoutView(parentFrame, order);
            cv.setVisible(true);
            refreshCart();
        });

        return btn;
    }

    public void refreshCart() {
        tableModel.setRowCount(0);
        List<CartItem> items = ProductController.getCart();

        boolean hasItems = !items.isEmpty();
        emptyLabel.setVisible(!hasItems);
        scroll.setVisible(hasItems);

        for (CartItem item : items) {
            tableModel.addRow(new Object[]{
                    item.getProduct().getName(),
                    item.getProduct().getCategory(),
                    String.format("\u20b9%,.0f", item.getProduct().getPrice()),
                    item.getQuantity(),
                    String.format("\u20b9%,.0f", item.lineTotal()),
                    "Remove"
            });
        }

        double total = ProductController.cartTotal();
        totalLabel.setText(String.format("Total: \u20b9%,.2f", total));
        creditLabel.setText(CircularDripCalculator.summary(total));
    }

    // Remove button helpers (unchanged)
    static class RemoveBtnRenderer extends JButton implements TableCellRenderer {
        public RemoveBtnRenderer() {
            setOpaque(true); setText("\u2715  Remove");
            setFont(new Font("Arial", Font.BOLD, 11));
            setBackground(new Color(0xF5,0xF5,0xF5));
            setForeground(new Color(0x99,0x22,0x22));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xDD,0xDD,0xDD),1,true),
                    BorderFactory.createEmptyBorder(5,10,5,10)));
        }
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) { return this; }
    }

    static class RemoveBtnEditor extends DefaultCellEditor {
        private final JButton btn;
        private final DefaultTableModel model;
        private final CartView cart;
        private String productName;

        public RemoveBtnEditor(JCheckBox cb, JTable table,
                               DefaultTableModel model, CartView cart) {
            super(cb);
            this.model = model; this.cart = cart;
            btn = new JButton("\u2715  Remove");
            btn.setFont(new Font("Arial", Font.BOLD, 11));
            btn.setBackground(new Color(0xF5,0xF5,0xF5));
            btn.setForeground(new Color(0x99,0x22,0x22));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xDD,0xDD,0xDD),1,true),
                    BorderFactory.createEmptyBorder(5,10,5,10)));
            btn.setOpaque(true);
            btn.addActionListener(e -> {
                ProductController.getCatalogue().stream()
                        .filter(p -> p.getName().equals(productName))
                        .findFirst()
                        .ifPresent(p -> ProductController.removeFromCart(p.getId()));
                SwingUtilities.invokeLater(cart::refreshCart);
                fireEditingStopped();
            });
        }
        public Component getTableCellEditorComponent(JTable t, Object v,
                boolean sel, int r, int c) {
            productName = (String) t.getValueAt(r, 0);
            return btn;
        }
        public Object getCellEditorValue() { return "Remove"; }
    }
}
