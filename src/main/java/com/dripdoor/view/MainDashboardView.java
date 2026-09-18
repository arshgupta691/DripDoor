package com.dripdoor.view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.dripdoor.config.ThemeConfig;
import com.dripdoor.service.AuthService;

/**
 * Main application window after login.
 * Sidebar: Collection | AI Stylist | Wishlist | My Cart | My Orders
 */
public class MainDashboardView extends JFrame {

    private JPanel     contentPanel;
    private CardLayout cardLayout;

    private ProductCatalogView catalogView;
    private AIStylistView      stylistView;
    private WishlistView       wishlistView;
    private CartView           cartView;
    private MyOrdersView       ordersView;

    private JButton activeNavBtn;

    public MainDashboardView() {
        initUI();
    }

    private void initUI() {
        String userName = AuthService.getCurrentUser() != null
                ? AuthService.getCurrentUser().getName() : "Guest";

        setTitle("DripDoor — Welcome, " + userName);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 760);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(960, 640));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConfig.WHITE);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(ThemeConfig.SOFT_GREY);

        catalogView  = new ProductCatalogView(this);
        stylistView  = new AIStylistView();
        wishlistView = new WishlistView(this);
        cartView     = new CartView(this);
        ordersView   = new MyOrdersView();

        contentPanel.add(catalogView,  "CATALOGUE");
        contentPanel.add(stylistView,  "STYLIST");
        contentPanel.add(wishlistView, "WISHLIST");
        contentPanel.add(cartView,     "CART");
        contentPanel.add(ordersView,   "ORDERS");

        root.add(contentPanel, BorderLayout.CENTER);
        showCard("CATALOGUE");
    }

    public void showCard(String name) {
        cardLayout.show(contentPanel, name);
        switch (name) {
            case "CART"     -> cartView.refreshCart();
            case "WISHLIST" -> wishlistView.refresh();
            case "ORDERS"   -> ordersView.refresh();
        }
    }

    public CartView     getCartView()     { return cartView; }
    public WishlistView getWishlistView() { return wishlistView; }

    // ── Sidebar ────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(ThemeConfig.DEEP_BLACK);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(36, 0, 36, 0));

        JLabel brand = new JLabel("DRIP DOOR");
        brand.setFont(new Font("Georgia", Font.BOLD, 22));
        brand.setForeground(ThemeConfig.GOLD);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(brand);

        sidebar.add(Box.createVerticalStrut(4));

        JLabel tagline = new JLabel("Luxury Redefined");
        tagline.setFont(ThemeConfig.SMALL_FONT.deriveFont(Font.ITALIC, 12f));
        tagline.setForeground(new Color(0xAA, 0xAA, 0xAA));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(tagline);

        sidebar.add(Box.createVerticalStrut(28));
        JPanel div = new JPanel();
        div.setMaximumSize(new Dimension(140, 1));
        div.setBackground(ThemeConfig.GOLD);
        div.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(div);
        sidebar.add(Box.createVerticalStrut(28));

        // Nav items
        JButton first = addNavItem(sidebar, "THE COLLECTION", "CATALOGUE");
                        addNavItem(sidebar, "AI STYLIST",      "STYLIST");
                        addNavItem(sidebar, "\u2665  WISHLIST", "WISHLIST");
                        addNavItem(sidebar, "MY CART",          "CART");
                        addNavItem(sidebar, "MY ORDERS",        "ORDERS");

        activeNavBtn = first;
        highlightNav(first);

        sidebar.add(Box.createVerticalGlue());

        // Drip credit badge
        if (AuthService.getCurrentUser() != null) {
            double credit = AuthService.getCurrentUser().getDripCredit();
            JPanel creditBadge = new JPanel();
            creditBadge.setBackground(new Color(0x1A, 0x1A, 0x1A));
            creditBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.GOLD, 1, true),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)));
            creditBadge.setMaximumSize(new Dimension(160, 50));
            creditBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel creditLabel = new JLabel(String.format("Drip Credit  \u20b9%.0f", credit));
            creditLabel.setFont(ThemeConfig.SMALL_FONT.deriveFont(Font.BOLD, 11f));
            creditLabel.setForeground(ThemeConfig.GOLD);
            creditBadge.add(creditLabel);
            sidebar.add(creditBadge);
            sidebar.add(Box.createVerticalStrut(16));
        }

        // Logout
        JButton logout = new JButton("LOG OUT");
        logout.setFont(ThemeConfig.SMALL_FONT.deriveFont(Font.BOLD, 10f));
        logout.setForeground(new Color(0x99, 0x99, 0x99));
        logout.setBackground(ThemeConfig.DEEP_BLACK);
        logout.setBorder(BorderFactory.createLineBorder(new Color(0x44, 0x44, 0x44), 1, true));
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setMaximumSize(new Dimension(120, 34));
        logout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                logout.setForeground(ThemeConfig.WHITE);
                logout.setBorder(BorderFactory.createLineBorder(ThemeConfig.GOLD, 1, true));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                logout.setForeground(new Color(0x99, 0x99, 0x99));
                logout.setBorder(BorderFactory.createLineBorder(new Color(0x44, 0x44, 0x44), 1, true));
            }
        });
        logout.addActionListener(e -> {
            AuthService.logout();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
        });
        sidebar.add(logout);

        return sidebar;
    }

    private JButton addNavItem(JPanel sidebar, String label, String card) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Georgia", Font.PLAIN, 13));
        btn.setForeground(new Color(0xCC, 0xCC, 0xCC));
        btn.setBackground(ThemeConfig.DEEP_BLACK);
        btn.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn != activeNavBtn) {
                    btn.setContentAreaFilled(true);
                    btn.setBackground(new Color(0x33, 0x33, 0x33));
                    btn.setForeground(ThemeConfig.WHITE);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn != activeNavBtn) {
                    btn.setContentAreaFilled(false);
                    btn.setForeground(new Color(0xCC, 0xCC, 0xCC));
                }
            }
        });

        btn.addActionListener(e -> {
            if (activeNavBtn != null) unhighlightNav(activeNavBtn);
            activeNavBtn = btn;
            highlightNav(btn);
            showCard(card);
        });

        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(4));
        return btn;
    }

    private void highlightNav(JButton btn) {
        btn.setForeground(ThemeConfig.GOLD);
        btn.setFont(new Font("Georgia", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ThemeConfig.GOLD),
                BorderFactory.createEmptyBorder(14, 29, 14, 32)));
    }

    private void unhighlightNav(JButton btn) {
        btn.setForeground(new Color(0xCC, 0xCC, 0xCC));
        btn.setFont(new Font("Georgia", Font.PLAIN, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
    }
}
