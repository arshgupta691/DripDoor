package com.dripdoor.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.dripdoor.model.Order;
import com.dripdoor.service.AuthService;
import com.dripdoor.service.FirebaseService;
import com.dripdoor.view.DispatcherView;

/**
 * Manages order lifecycle: creation, Firebase persistence, and dispatch.
 *
 * Orders are stored per user UID both in Firebase (persisted across logins)
 * and in an in-memory cache (for fast access within a session).
 */
public class OrderController {

    // In-memory cache keyed by uid — loaded from Firebase on login
    private static final Map<String, List<Order>> ORDERS_BY_USER = new HashMap<>();

    // ── Called by AuthService after successful login ───────────────────────

    /**
     * Loads orders for the given UID from Firebase into the in-memory cache.
     * Must be called on a background thread (it blocks on network I/O).
     */
    public static void loadOrdersForUser(String uid) {
        List<Order> fromDb = FirebaseService.loadOrders(uid);
        // Recalculate time-based status for any in-progress orders
        for (Order order : fromDb) {
            recalculateStatusFromTime(order);
        }
        ORDERS_BY_USER.put(uid, new ArrayList<>(fromDb));
        System.out.println("[OrderController] Loaded " + fromDb.size() + " orders for " + uid);
    }

    /**
     * Recalculates order status based on elapsed time since placedAt.
     * Fixes the bug where closing the app freezes the status in Firebase.
     *
     * Timeline mirrors DispatcherView (DISPATCH_SECONDS = 600):
     *   0–5 min   → CURATING
     *   5–10 min  → DISPATCHED
     *   10+ min   → DELIVERED
     */
    public static void recalculateStatusFromTime(Order order) {
        // Only auto-advance in-progress orders
        if (order.getStatus() == Order.Status.PENDING  ||
            order.getStatus() == Order.Status.DELIVERED ||
            order.getStatus() == Order.Status.CANCELLED) return;

        if (order.getPlacedAt() == null) return;

        long secondsElapsed = Duration.between(order.getPlacedAt(), LocalDateTime.now()).getSeconds();
        long total = com.dripdoor.config.AppConfig.DISPATCH_SECONDS; // 600 s

        Order.Status corrected;
        if (secondsElapsed >= total) {
            corrected = Order.Status.DELIVERED;
        } else if (secondsElapsed >= total / 2) {
            corrected = Order.Status.DISPATCHED;
        } else {
            corrected = Order.Status.CURATING;
        }

        // Only advance — never go backwards
        if (corrected.ordinal() > order.getStatus().ordinal()) {
            updateStatus(order, corrected);
        }
    }

    // ── Place order ───────────────────────────────────────────────────────

    /**
     * Builds a new Order in memory from the current cart but does NOT
     * save it to Firebase and does NOT clear the cart.
     * Call finaliseOrder(order) after applying the delivery address.
     */
    public static Order buildOrder() {
        if (!AuthService.isLoggedIn()) return null;
        if (ProductController.getCart().isEmpty()) return null;

        String uid = AuthService.getCurrentUser().getUid();
        return new Order(uid, new ArrayList<>(ProductController.getCart()));
    }

    /**
     * Persists the order to Firebase, awards Circular Drip credit,
     * adds it to the in-memory cache, and clears the cart.
     * Must be called AFTER the delivery address has been applied.
     */
    public static void finaliseOrder(Order order) {
        if (order == null) return;
        String uid = order.getUserUid();

        FirebaseService.saveOrder(order);

        double existing = AuthService.getCurrentUser().getDripCredit();
        AuthService.getCurrentUser().setDripCredit(existing + order.getBuybackCredit());

        ORDERS_BY_USER.computeIfAbsent(uid, k -> new ArrayList<>()).add(order);

        ProductController.clearCart();
    }

    /**
     * @deprecated Use buildOrder() + apply address + finaliseOrder() instead.
     * Kept for backward compatibility.
     */
    @Deprecated
    public static Order placeOrder() {
        Order order = buildOrder();
        if (order == null) return null;
        finaliseOrder(order);
        return order;
    }

    // ── Query ─────────────────────────────────────────────────────────────

    /**
     * Returns all orders for the currently logged-in user.
     * Returns an empty list if no user is logged in.
     */
    public static List<Order> getSessionOrders() {
        if (!AuthService.isLoggedIn()) return List.of();
        String uid = AuthService.getCurrentUser().getUid();
        List<Order> userOrders = ORDERS_BY_USER.get(uid);
        return userOrders == null ? List.of() : List.copyOf(userOrders);
    }

    /**
     * Updates an order's status in both the in-memory cache and Firebase.
     */
    public static void updateStatus(Order order, Order.Status newStatus) {
        order.setStatus(newStatus);
        FirebaseService.updateOrderStatus(order);
        // Ensure the in-memory list contains the updated object
        String uid = order.getUserUid();
        List<Order> list = ORDERS_BY_USER.get(uid);
        if (list != null && !list.contains(order)) {
            list.add(order);
        }
    }

    /**
     * Replaces the in-memory cache for a user with a freshly loaded list.
     * Called by FirebaseService's real-time listener.
     */
    public static void replaceCache(String uid, java.util.List<Order> orders) {
        if (uid != null) ORDERS_BY_USER.put(uid, new ArrayList<>(orders));
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /**
     * Evicts the in-memory cache for the given UID.
     * Called by AuthService.logout() so stale data is never shown.
     */
    public static void clearSessionOrders(String uid) {
        if (uid != null) ORDERS_BY_USER.remove(uid);
    }

    // ── Dispatcher ────────────────────────────────────────────────────────

    public static void startDispatcher(JFrame parent, Order order) {
        SwingUtilities.invokeLater(() -> {
            DispatcherView dv = new DispatcherView(parent, order);
            dv.setVisible(true);
            dv.startDispatch();
        });
    }
}