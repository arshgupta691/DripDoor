package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.ProductController;
import com.dripdoor.model.Product;
import com.dripdoor.service.AuthService;
import com.dripdoor.service.FirebaseService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wishlist page — shows items the user has hearted.
 * Items can be moved to cart or removed.
 *
 * Wishlists are now per-user: each UID has its own in-memory list that
 * is persisted to Firebase under wishlists/{uid}/.
 */
public class WishlistView extends JPanel {

    // ── Per-user in-memory store ──────────────────────────────────────────
    // Key: uid, Value: list of wishlisted products for that user
    private static final Map<String, List<Product>> USER_WISHLISTS = new HashMap<>();

    // ── Helper: get the current user's UID, or null ───────────────────────
    private static String currentUid() {
        com.dripdoor.model.User u = AuthService.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    // ── Helper: get (or create) the list for the given uid ────────────────
    private static List<Product> listFor(String uid) {
        return USER_WISHLISTS.computeIfAbsent(uid, k -> new ArrayList<>());
    }

    // ── Public static API (used by ProductCatalogView) ────────────────────

    /** Adds a product to the current user's wishlist and persists to Firebase. */
    public static void addToWishlist(Product p) {
        String uid = currentUid();
        if (uid == null) return;
        List<Product> list = listFor(uid);
        if (list.stream().noneMatch(x -> x.getId().equals(p.getId()))) {
            list.add(p);
            persistAsync(uid, list);
        }
    }

    /** Removes a product from the current user's wishlist and persists to Firebase. */
    public static void removeFromWishlist(Product p) {
        String uid = currentUid();
        if (uid == null) return;
        List<Product> list = listFor(uid);
        list.removeIf(x -> x.getId().equals(p.getId()));
        persistAsync(uid, list);
    }

    /** Returns true if the product is in the current user's wishlist. */
    public static boolean isWishlisted(Product p) {
        String uid = currentUid();
        if (uid == null) return false;
        return listFor(uid).stream().anyMatch(x -> x.getId().equals(p.getId()));
    }

    /** Returns an unmodifiable snapshot of the current user's wishlist. */
    public static List<Product> getWishlist() {
        String uid = currentUid();
        if (uid == null) return List.of();
        return List.copyOf(listFor(uid));
    }

    /**
     * Called from AuthService after login to populate the in-memory list from
     * Firebase.  Runs synchronously (called from a SwingWorker background thread).
     */
    public static void loadWishlistForUser(String uid) {
        if (uid == null) return;
        List<Product> loaded = FirebaseService.loadWishlist(uid);
        // Merge against live catalogue so price/image are always fresh
        List<Product> catalogue = ProductController.getCatalogue();
        List<Product> resolved = new ArrayList<>();
        for (Product stub : loaded) {
            Product live = catalogue.stream()
                    .filter(pr -> pr.getId().equals(stub.getId()))
                    .findFirst()
                    .orElse(stub);  // fall back to stored stub if product removed
            resolved.add(live);
        }
        USER_WISHLISTS.put(uid, resolved);
    }

    /**
     * Called from AuthService on logout to clear the in-memory list for the
     * given uid so the next user starts clean.
     */
    public static void clearWishlistForUser(String uid) {
        if (uid != null) USER_WISHLISTS.remove(uid);
    }

    // ── Private Firebase persistence (fire-and-forget on background thread) ─

    private static void persistAsync(String uid, List<Product> list) {
        List<Product> snapshot = List.copyOf(list);
        new Thread(() -> FirebaseService.saveWishlist(uid, snapshot),
                "wishlist-persist").start();
    }

    // ── Instance (UI) ─────────────────────────────────────────────────────

    private final MainDashboardView dashboard;
    private JPanel itemsPanel;
    private JLabel emptyLabel;

    public WishlistView(MainDashboardView dashboard) {
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

        JLabel title = new JLabel("My Wishlist");
        title.setFont(new Font("Georgia", Font.BOLD, 28));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        titleBlock.add(title);

        JLabel sub = new JLabel("Items you love");
        sub.setFont(new Font("Georgia", Font.ITALIC, 13));
        sub.setForeground(ThemeConfig.GOLD);
        titleBlock.add(sub);

        header.add(titleBlock, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Empty label
        emptyLabel = new JLabel(
                "<html><center><br><br>Your wishlist is empty.<br><br>"
                + "<span style='color:#999;font-size:11px;'>Browse the Collection and tap the ♡ to save items.</span></center></html>",
                SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Georgia", Font.ITALIC, 16));
        emptyLabel.setForeground(new Color(0x77, 0x77, 0x77));

        // Items panel (grid)
        itemsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 20, 20));
        itemsPanel.setBackground(ThemeConfig.SOFT_GREY);

        JScrollPane scroll = new JScrollPane(itemsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(ThemeConfig.SOFT_GREY);
        scroll.getViewport().setBackground(ThemeConfig.SOFT_GREY);
        add(scroll, BorderLayout.CENTER);
    }

    /** Called by MainDashboardView whenever this tab is shown. */
    public void refresh() {
        itemsPanel.removeAll();

        List<Product> wishlist = getWishlist();

        if (wishlist.isEmpty()) {
            itemsPanel.setLayout(new BorderLayout());
            itemsPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            itemsPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 20, 20));
            for (Product p : wishlist) {
                itemsPanel.add(buildCard(p));
            }
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel buildCard(Product p) {
        JPanel card = new JPanel();
        card.setBackground(ThemeConfig.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setPreferredSize(new Dimension(240, 200));

        JLabel name = new JLabel("<html><b>" + p.getName() + "</b></html>");
        name.setFont(new Font("Georgia", Font.BOLD, 14));
        name.setForeground(ThemeConfig.DEEP_BLACK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(name);

        card.add(Box.createVerticalStrut(4));

        JLabel cat = new JLabel(p.getCategory());
        cat.setFont(new Font("Arial", Font.PLAIN, 11));
        cat.setForeground(Color.GRAY);
        cat.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(cat);

        card.add(Box.createVerticalStrut(8));

        JLabel price = new JLabel(String.format("\u20b9%,.0f", p.getPrice()));
        price.setFont(new Font("Georgia", Font.BOLD, 17));
        price.setForeground(ThemeConfig.DEEP_BLACK);
        price.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(price);

        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(14));

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(ThemeConfig.WHITE);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addCart = new JButton("Add to Cart");
        addCart.setFont(new Font("Arial", Font.BOLD, 11));
        addCart.setBackground(ThemeConfig.DEEP_BLACK);
        addCart.setForeground(ThemeConfig.WHITE);
        addCart.setFocusPainted(false);
        addCart.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        addCart.setOpaque(true);
        addCart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addCart.addActionListener(e -> {
            ProductController.addToCart(p, 1);
            JOptionPane.showMessageDialog(this,
                    p.getName() + " added to cart!", "Added", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton remove = new JButton("Remove");
        remove.setFont(new Font("Arial", Font.PLAIN, 11));
        remove.setFocusPainted(false);
        remove.setBorder(BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)));
        remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        remove.addActionListener(e -> {
            removeFromWishlist(p);
            refresh();
        });

        btnRow.add(addCart);
        btnRow.add(remove);
        card.add(btnRow);

        return card;
    }

    /** Simple wrap layout so cards flow into multiple rows. */
    static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
                int width = 0, height = 0, rowHeight = 0, rowWidth = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component c = target.getComponent(i);
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        width = Math.max(width, rowWidth);
                        height += rowHeight + vgap;
                        rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                width = Math.max(width, rowWidth);
                height += rowHeight + insets.top + insets.bottom + vgap * 2;
                return new Dimension(width, height);
            }
        }
    }
}
