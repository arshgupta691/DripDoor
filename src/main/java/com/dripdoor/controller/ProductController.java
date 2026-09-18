package com.dripdoor.controller;

import com.dripdoor.model.CartItem;
import com.dripdoor.model.Product;
import com.dripdoor.service.AuthService;
import com.dripdoor.service.CartService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the product catalogue and the in-session shopping cart.
 *
 * Cart is scoped per user UID and synced to Firebase after every mutation
 * so it survives logout/login.
 */
public class ProductController {

    private static final List<Product> CATALOGUE = new ArrayList<>();

    // Keyed by user UID — each account has its own isolated cart
    private static final Map<String, List<CartItem>> CART_BY_USER = new HashMap<>();

    static { seedCatalogue(); }

    // ── Catalogue ─────────────────────────────────────────────────────────

    public static List<Product> getCatalogue() { return List.copyOf(CATALOGUE); }

    public static List<Product> getByCategory(String category) {
        return CATALOGUE.stream()
                        .filter(p -> p.getCategory().equalsIgnoreCase(category))
                        .toList();
    }

    // ── Cart (per-user, Firebase-backed) ──────────────────────────────────

    private static String currentUid() {
        return AuthService.isLoggedIn()
                ? AuthService.getCurrentUser().getUid()
                : "__guest__";
    }

    private static List<CartItem> currentCart() {
        return CART_BY_USER.computeIfAbsent(currentUid(), k -> new ArrayList<>());
    }

    public static void addToCart(Product product, int quantity) {
        List<CartItem> cart = currentCart();
        for (CartItem item : cart) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                persistCart();
                return;
            }
        }
        cart.add(new CartItem(product, quantity));
        persistCart();
    }

    public static void removeFromCart(String productId) {
        currentCart().removeIf(i -> i.getProduct().getId().equals(productId));
        persistCart();
    }

    /** Clears the in-memory cart and the Firebase copy. */
    public static void clearCart() {
        String uid = currentUid();
        currentCart().clear();
        CartService.clearCart(uid);          // wipe Firebase node
    }

    /**
     * Removes a user's cart from the in-memory map entirely.
     * Does NOT clear Firebase — the cart is preserved for the next login.
     * Call this on logout so the next account starts with a clean in-memory slate.
     */
    public static void clearCartForUser(String uid) {
        if (uid != null) CART_BY_USER.remove(uid);
    }

    public static List<CartItem> getCart()    { return List.copyOf(currentCart()); }
    public static double cartTotal()          { return currentCart().stream().mapToDouble(CartItem::lineTotal).sum(); }
    public static int    cartItemCount()      { return currentCart().stream().mapToInt(CartItem::getQuantity).sum(); }

    /** Async-writes the current cart to Firebase. */
    private static void persistCart() {
        String uid = currentUid();
        if (!"__guest__".equals(uid)) {
            List<CartItem> snapshot = List.copyOf(currentCart());
            // Fire-and-forget on a daemon thread so the UI never blocks
            Thread t = new Thread(() -> CartService.saveCart(uid, snapshot), "cart-persist");
            t.setDaemon(true);
            t.start();
        }
    }

    // ── Seed data ─────────────────────────────────────────────────────────

    private static void seedCatalogue() {
        CATALOGUE.add(new Product("P001", "Diamond Solitaire Necklace",
                "Necklace",  95000.0, "products/diamond_necklace"));
        CATALOGUE.add(new Product("P002", "Sapphire Cocktail Ring",
                "Ring",      55000.0, "products/sapphire_ring"));
        CATALOGUE.add(new Product("P003", "Diamond Tennis Bracelet",
                "Bracelet", 130000.0, "products/tennis_bracelet"));
        CATALOGUE.add(new Product("P004", "Pearl Drop Earrings",
                "Earrings",  38000.0, "products/pearl_earrings"));
        CATALOGUE.add(new Product("P005", "Gold Bangle Set",
                "Bracelet",  72000.0, "products/gold_bangles"));
        CATALOGUE.add(new Product("P006", "Ruby Halo Ring",
                "Ring",      88000.0, "products/ruby_ring"));
        CATALOGUE.add(new Product("P007", "Emerald Pendant",
                "Necklace",  67000.0, "products/emerald_pendant"));
        CATALOGUE.add(new Product("P008", "Diamond Stud Earrings",
                "Earrings",  45000.0, "products/diamond_studs"));
        CATALOGUE.add(new Product("P009", "Rose Gold Watch",
                "Watch",    195000.0, "products/rose_gold_watch"));
        CATALOGUE.add(new Product("P010", "Platinum Chain",
                "Necklace",  82000.0, "products/platinum_chain"));
    }
}
