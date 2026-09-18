package com.dripdoor.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.ProductController;
import com.dripdoor.model.Product;
import com.dripdoor.service.CloudinaryService;

/**
 * Product catalogue — Dior-inspired editorial grid.
 *
 * Improvements:
 *  • Taller image area with category initial centred in gold
 *  • "Add to Cart" button spans full card width, slides gold on hover
 *  • Quantity spinner embedded in card before add-to-cart
 *  • Toast notification replaces modal dialog for a smoother UX
 *  • After adding, cart badge in the nav is implicitly updated via refreshCart()
 *  • Product images loaded asynchronously from Cloudinary
 */
public class ProductCatalogView extends JPanel {

    private final MainDashboardView dashboard;

    // Category filter bar
    private static final String[] FILTERS =
            {"All", "Necklace", "Ring", "Bracelet", "Earrings", "Watch"};

    private JPanel gridPanel;

    public ProductCatalogView(MainDashboardView dashboard) {
        this.dashboard = dashboard;
        setBackground(ThemeConfig.SOFT_GREY);
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // ── Top bar ───────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(ThemeConfig.WHITE);
        topBar.setBorder(new EmptyBorder(22, 32, 22, 32));

        JPanel titleBlock = new JPanel();
        titleBlock.setBackground(ThemeConfig.WHITE);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("The Collection");
        title.setFont(new Font("Georgia", Font.BOLD, 28));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        titleBlock.add(title);

        JLabel sub = new JLabel("Curated Haute Jewellery");
        sub.setFont(new Font("Georgia", Font.ITALIC, 13));
        sub.setForeground(ThemeConfig.GOLD);
        titleBlock.add(sub);

        topBar.add(titleBlock, BorderLayout.WEST);

        // Filter chips
        JPanel filters = buildFilterBar();
        topBar.add(filters, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ── Grid ──────────────────────────────────────────────────────────
        gridPanel = new JPanel(new GridLayout(0, 3, 24, 24));
        gridPanel.setBackground(ThemeConfig.SOFT_GREY);
        gridPanel.setBorder(new EmptyBorder(28, 28, 28, 28));

        loadGrid("All");

        JScrollPane scroll = new JScrollPane(gridPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBackground(ThemeConfig.SOFT_GREY);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setBackground(ThemeConfig.WHITE);
        for (String f : FILTERS) {
            JButton chip = new JButton(f);
            chip.setFont(new Font("Arial", Font.PLAIN, 11));
            chip.setForeground(ThemeConfig.DEEP_BLACK);
            chip.setBackground(ThemeConfig.WHITE);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                    BorderFactory.createEmptyBorder(5, 14, 5, 14)));
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    chip.setBackground(ThemeConfig.DEEP_BLACK);
                    chip.setForeground(ThemeConfig.WHITE);
                }
                public void mouseExited(MouseEvent e) {
                    chip.setBackground(ThemeConfig.WHITE);
                    chip.setForeground(ThemeConfig.DEEP_BLACK);
                }
            });
            chip.addActionListener(e -> loadGrid(f));
            bar.add(chip);
        }
        return bar;
    }

    private void loadGrid(String filter) {
        gridPanel.removeAll();
        List<Product> products = "All".equals(filter)
                ? ProductController.getCatalogue()
                : ProductController.getByCategory(filter);
        for (Product p : products) {
            gridPanel.add(buildProductCard(p));
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel buildProductCard(Product product) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeConfig.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(ThemeConfig.BORDER, 1, true),
                new EmptyBorder(0, 0, 20, 0)));

        // ── Image area ────────────────────────────────────────────────────
        // Holds the loaded image once the background thread finishes
        final BufferedImage[] loadedImage = {null};
        final boolean[] loadFailed = {false};

        // Build the Cloudinary URL for this product
        final String imageUrl = CloudinaryService.getImageUrl(
                product.getCloudinaryPublicId(), 220, 200);

        JPanel imgBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                if (loadedImage[0] != null) {
                    // Draw the real product image, scaled to fill the box
                    g2.drawImage(loadedImage[0], 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Placeholder: gold-gradient + category initial while loading
                    GradientPaint gp = new GradientPaint(
                            0, 0, new Color(0xF9, 0xF5, 0xE8),
                            0, getHeight(), new Color(0xED, 0xE0, 0xBB));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    int r  = 44;
                    g2.setColor(new Color(0xD4, 0xAF, 0x37, 60));
                    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                    g2.setColor(ThemeConfig.GOLD);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                    g2.setColor(ThemeConfig.GOLD);
                    g2.setFont(new Font("Georgia", Font.BOLD, 32));
                    FontMetrics fm = g2.getFontMetrics();
                    String letter = product.getCategory().substring(0, 1);
                    g2.drawString(letter,
                            cx - fm.stringWidth(letter) / 2,
                            cy + fm.getAscent() / 2 - 2);

                    // Bottom category label strip
                    g2.setColor(new Color(0x00, 0x00, 0x00, 30));
                    g2.fillRect(0, getHeight() - 28, getWidth(), 28);
                    g2.setColor(ThemeConfig.GOLD);
                    g2.setFont(new Font("Georgia", Font.ITALIC, 11));
                    fm = g2.getFontMetrics();
                    String cat = product.getCategory().toUpperCase();
                    g2.drawString(cat,
                            (getWidth() - fm.stringWidth(cat)) / 2,
                            getHeight() - 10);
                }
                g2.dispose();
            }
        };
        imgBox.setPreferredSize(new Dimension(220, 200));
        imgBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        imgBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(imgBox);

        // Load image asynchronously so the UI never freezes
        Thread loader = new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                BufferedImage bi = ImageIO.read(url);
                if (bi != null) {
                    loadedImage[0] = bi;
                    SwingUtilities.invokeLater(imgBox::repaint);
                } else {
                    loadFailed[0] = true;
                }
            } catch (Exception ex) {
                loadFailed[0] = true;
                // Placeholder stays visible — no crash
            }
        }, "img-load-" + product.getId());
        loader.setDaemon(true);
        loader.start();

        // ── Text content ──────────────────────────────────────────────────
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(ThemeConfig.WHITE);
        info.setBorder(new EmptyBorder(16, 20, 0, 20));
        info.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel("<html>" + product.getName() + "</html>");
        name.setFont(new Font("Georgia", Font.BOLD, 15));
        name.setForeground(ThemeConfig.DEEP_BLACK);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(name);

        info.add(Box.createVerticalStrut(6));

        JLabel price = new JLabel(String.format("₹%,.0f", product.getPrice()));
        price.setFont(new Font("Arial", Font.PLAIN, 13));
        price.setForeground(new Color(0x55, 0x55, 0x55));
        price.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(price);

        info.add(Box.createVerticalStrut(4));

        // Circular Drip buyback note
        JLabel buyback = new JLabel(String.format("Drip Credit: ₹%,.0f back", product.getPrice() * 0.70));
        buyback.setFont(new Font("Arial", Font.ITALIC, 11));
        buyback.setForeground(ThemeConfig.GOLD);
        buyback.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(buyback);

        card.add(info);

        card.add(Box.createVerticalStrut(14));

        // ── Qty + Add-to-Cart row ─────────────────────────────────────────
        JPanel actionRow = new JPanel(new BorderLayout(8, 0));
        actionRow.setBackground(ThemeConfig.WHITE);
        actionRow.setBorder(new EmptyBorder(0, 20, 0, 20));
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Qty spinner (1–10)
        SpinnerNumberModel spinModel = new SpinnerNumberModel(1, 1, 10, 1);
        JSpinner qtySpinner = new JSpinner(spinModel);
        qtySpinner.setFont(new Font("Arial", Font.PLAIN, 12));
        qtySpinner.setPreferredSize(new Dimension(58, 36));
        qtySpinner.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true));
        actionRow.add(qtySpinner, BorderLayout.WEST);

        // Add-to-Cart button
        JButton addBtn = new JButton("ADD TO CART");
        addBtn.setFont(new Font("Arial", Font.BOLD, 11));
        addBtn.setBackground(ThemeConfig.DEEP_BLACK);
        addBtn.setForeground(ThemeConfig.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setOpaque(true);

        addBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                addBtn.setBackground(ThemeConfig.GOLD);
                addBtn.setForeground(ThemeConfig.DEEP_BLACK);
            }
            public void mouseExited(MouseEvent e) {
                addBtn.setBackground(ThemeConfig.DEEP_BLACK);
                addBtn.setForeground(ThemeConfig.WHITE);
            }
        });

        addBtn.addActionListener(e -> {
            int qty = (int) qtySpinner.getValue();
            ProductController.addToCart(product, qty);
            showToast(product.getName() + " × " + qty + " added to cart");
        });

        actionRow.add(addBtn, BorderLayout.CENTER);

        // Wishlist heart button
        JButton wishBtn = new JButton(WishlistView.isWishlisted(product) ? "\u2665" : "\u2661");
        wishBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        wishBtn.setForeground(WishlistView.isWishlisted(product)
                ? new Color(0xCC, 0x22, 0x22) : Color.GRAY);
        wishBtn.setBackground(ThemeConfig.WHITE);
        wishBtn.setFocusPainted(false);
        wishBtn.setBorder(BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true));
        wishBtn.setPreferredSize(new Dimension(40, 36));
        wishBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wishBtn.setToolTipText("Add to Wishlist");
        wishBtn.addActionListener(e -> {
            if (WishlistView.isWishlisted(product)) {
                WishlistView.removeFromWishlist(product);
                wishBtn.setText("\u2661");
                wishBtn.setForeground(Color.GRAY);
                wishBtn.setToolTipText("Add to Wishlist");
            } else {
                WishlistView.addToWishlist(product);
                wishBtn.setText("\u2665");
                wishBtn.setForeground(new Color(0xCC, 0x22, 0x22));
                wishBtn.setToolTipText("Remove from Wishlist");
                showToast(product.getName() + " added to wishlist");
            }
        });
        actionRow.add(wishBtn, BorderLayout.EAST);

        card.add(actionRow);

        // ── Try On button ─────────────────────────────────────────────────
        JButton tryOnBtn = new JButton("✦  TRY ON OUTFIT");
        tryOnBtn.setFont(new Font("Georgia", Font.ITALIC, 11));
        tryOnBtn.setForeground(ThemeConfig.GOLD);
        tryOnBtn.setBackground(ThemeConfig.WHITE);
        tryOnBtn.setFocusPainted(false);
        tryOnBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xEE, 0xE8, 0xD0)),
                BorderFactory.createEmptyBorder(9, 20, 9, 20)));
        tryOnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tryOnBtn.setOpaque(true);
        tryOnBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        tryOnBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tryOnBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                tryOnBtn.setBackground(new Color(0xFD, 0xF8, 0xEC));
                tryOnBtn.setForeground(ThemeConfig.GOLD_DARK);
            }
            public void mouseExited(MouseEvent e) {
                tryOnBtn.setBackground(ThemeConfig.WHITE);
                tryOnBtn.setForeground(ThemeConfig.GOLD);
            }
        });
        tryOnBtn.addActionListener(e -> {
            java.awt.Window win = SwingUtilities.getWindowAncestor(card);
            java.awt.Frame frame = (win instanceof java.awt.Frame f) ? f : null;
            OutfitBuilderView builder = new OutfitBuilderView(frame, product);
            builder.setVisible(true);
            // Refresh cart badge after builder closes
            if (dashboard != null) dashboard.getCartView().refreshCart();
        });
        card.add(tryOnBtn);

        // ── Hover border ──────────────────────────────────────────────────
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                        new LineBorder(ThemeConfig.GOLD, 1, true),
                        new EmptyBorder(0, 0, 20, 0)));
            }
            public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                        new LineBorder(ThemeConfig.BORDER, 1, true),
                        new EmptyBorder(0, 0, 20, 0)));
            }
        });

        return card;
    }

    /** Non-blocking, auto-dismissing toast at bottom-right of screen. */
    private void showToast(String message) {
        JWindow toast = new JWindow();
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

        JLabel checkmark = new JLabel("✓");
        checkmark.setFont(new Font("Arial", Font.BOLD, 14));
        checkmark.setForeground(ThemeConfig.GOLD);
        panel.add(checkmark);

        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        lbl.setForeground(Color.WHITE);
        panel.add(lbl);

        toast.add(panel);
        toast.pack();

        // Position bottom-right of the main window
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win != null) {
            Rectangle bounds = win.getBounds();
            toast.setLocation(
                    bounds.x + bounds.width  - toast.getWidth()  - 24,
                    bounds.y + bounds.height - toast.getHeight() - 40);
        }
        toast.setVisible(true);

        Timer timer = new Timer(2000, e -> toast.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}