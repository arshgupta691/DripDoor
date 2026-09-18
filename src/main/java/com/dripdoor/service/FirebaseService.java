package com.dripdoor.service;

import com.dripdoor.config.AppConfig;
import com.dripdoor.config.FirebaseConfig;
import com.dripdoor.model.CartItem;
import com.dripdoor.model.Order;
import com.dripdoor.model.Product;
import com.dripdoor.controller.ProductController;
import com.google.firebase.database.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles all Firebase Realtime Database operations for the orders/ node.
 *
 * DB layout:
 *   orders/{uid}/{firebaseKey}/
 *     orderId, userUid, totalAmount, buybackCredit, status, placedAt
 *     address/{ name, phone, line1, line2, city, state, pincode }
 *     items/{ item_0: { productId, productName, quantity, lineTotal }, … }
 */
public class FirebaseService {

    /**
     * Persists an order to orders/{uid}/{key} and sets its Firebase key.
     */
    public static boolean saveOrder(Order order) {
        if (!FirebaseConfig.isInitialised()) {
            System.out.println("[Firebase DEMO] Would save: " + order);
            return false;
        }

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference(AppConfig.FIREBASE_ORDERS_NODE)
                    .child(order.getUserUid());

            DatabaseReference newRef = ref.push();
            order.setFirebaseKey(newRef.getKey());

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            newRef.setValue(orderToMap(order), (error, reference) -> {
                if (error == null) {
                    success[0] = true;
                    System.out.println("[Firebase] Order saved: " + order.getOrderId());
                } else {
                    System.err.println("[Firebase] Save failed: " + error.getMessage());
                }
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("[Firebase] saveOrder error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates only the status field of an existing order in Firebase.
     */
    public static void updateOrderStatus(Order order) {
        if (!FirebaseConfig.isInitialised() || order.getFirebaseKey() == null) return;
        try {
            FirebaseDatabase.getInstance()
                    .getReference(AppConfig.FIREBASE_ORDERS_NODE)
                    .child(order.getUserUid())
                    .child(order.getFirebaseKey())
                    .child("status")
                    .setValue(order.getStatus().name(), (error, r) -> {
                        if (error != null)
                            System.err.println("[Firebase] updateStatus failed: " + error.getMessage());
                    });
        } catch (Exception e) {
            System.err.println("[Firebase] updateOrderStatus error: " + e.getMessage());
        }
    }

    /**
     * Loads all orders for the given UID from Firebase.
     * Returns an empty list if Firebase is not available.
     */
    public static List<Order> loadOrders(String uid) {
        List<Order> result = new ArrayList<>();
        if (!FirebaseConfig.isInitialised() || uid == null) return result;

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference(AppConfig.FIREBASE_ORDERS_NODE)
                    .child(uid);

            CountDownLatch latch = new CountDownLatch(1);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Order o = mapToOrder(snap, uid);
                        if (o != null) result.add(o);
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("[Firebase] loadOrders cancelled: " + error.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("[Firebase] loadOrders error: " + e.getMessage());
        }

        return result;
    }

    // ── Serialisation helpers ────────────────────────────────────────────

    private static Map<String, Object> orderToMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId",       order.getOrderId());
        map.put("userUid",       order.getUserUid());
        map.put("totalAmount",   order.getTotalAmount());
        map.put("buybackCredit", order.getBuybackCredit());
        map.put("status",        order.getStatus().name());
        map.put("placedAt",      order.getPlacedAt().toString());

        // Delivery address
        Map<String, Object> addr = new HashMap<>();
        addr.put("name",    nullSafe(order.getAddressName()));
        addr.put("phone",   nullSafe(order.getAddressPhone()));
        addr.put("line1",   nullSafe(order.getAddressLine1()));
        addr.put("line2",   nullSafe(order.getAddressLine2()));
        addr.put("city",    nullSafe(order.getAddressCity()));
        addr.put("state",   nullSafe(order.getAddressState()));
        addr.put("pincode", nullSafe(order.getAddressPincode()));
        map.put("address", addr);

        // Items
        Map<String, Object> itemsMap = new HashMap<>();
        for (int i = 0; i < order.getItems().size(); i++) {
            CartItem item = order.getItems().get(i);
            Map<String, Object> im = new HashMap<>();
            im.put("productId",   item.getProduct().getId());
            im.put("productName", item.getProduct().getName());
            im.put("category",    item.getProduct().getCategory());
            im.put("price",       item.getProduct().getPrice());
            im.put("quantity",    item.getQuantity());
            im.put("lineTotal",   item.lineTotal());
            itemsMap.put("item_" + i, im);
        }
        map.put("items", itemsMap);
        return map;
    }

    private static Order mapToOrder(DataSnapshot snap, String uid) {
        try {
            Order o = new Order();
            // Use setter reflection-free approach: Order has a no-arg constructor
            // and we set fields via setters.
            o.setUserUid(uid);
            o.setFirebaseKey(snap.getKey());

            String orderId = snap.child("orderId").getValue(String.class);
            if (orderId != null) setOrderId(o, orderId);

            String statusStr = snap.child("status").getValue(String.class);
            if (statusStr != null) {
                try { o.setStatus(Order.Status.valueOf(statusStr)); }
                catch (IllegalArgumentException ignored) {}
            }

            String placedAtStr = snap.child("placedAt").getValue(String.class);
            if (placedAtStr != null) {
                try { setPlacedAt(o, LocalDateTime.parse(placedAtStr)); }
                catch (Exception ignored) {}
            }

            Double total = snap.child("totalAmount").getValue(Double.class);
            Double credit = snap.child("buybackCredit").getValue(Double.class);

            // Address
            DataSnapshot addr = snap.child("address");
            o.setAddressName(   addr.child("name").getValue(String.class));
            o.setAddressPhone(  addr.child("phone").getValue(String.class));
            o.setAddressLine1(  addr.child("line1").getValue(String.class));
            o.setAddressLine2(  addr.child("line2").getValue(String.class));
            o.setAddressCity(   addr.child("city").getValue(String.class));
            o.setAddressState(  addr.child("state").getValue(String.class));
            o.setAddressPincode(addr.child("pincode").getValue(String.class));

            // Items — reconstruct from catalogue; fall back to stub product if not found
            List<CartItem> items = new ArrayList<>();
            List<Product> catalogue = ProductController.getCatalogue();
            for (DataSnapshot itemSnap : snap.child("items").getChildren()) {
                String productId   = itemSnap.child("productId").getValue(String.class);
                String productName = itemSnap.child("productName").getValue(String.class);
                String category    = itemSnap.child("category").getValue(String.class);
                Double price       = itemSnap.child("price").getValue(Double.class);
                Long   qtyLong     = itemSnap.child("quantity").getValue(Long.class);
                int    qty         = (qtyLong != null) ? qtyLong.intValue() : 1;

                // Try to get live product from catalogue first
                Product p = catalogue.stream()
                        .filter(pr -> pr.getId().equals(productId))
                        .findFirst()
                        .orElse(null);

                if (p == null) {
                    // Reconstruct a read-only stub so history still renders
                    p = new Product(
                            productId != null ? productId : "?",
                            productName != null ? productName : "Unknown",
                            category != null ? category : "",
                            price != null ? price : 0.0,
                            ""
                    );
                }
                items.add(new CartItem(p, qty));
            }
            o.setItems(items);

            // Recalculate totals from live items (keeps data consistent)
            o.recalculate();
            // But override with stored values if items are stubs (price may differ)
            if (total != null)  setTotalAmount(o, total);
            if (credit != null) setBuybackCredit(o, credit);

            return o;

        } catch (Exception e) {
            System.err.println("[Firebase] mapToOrder failed: " + e.getMessage());
            return null;
        }
    }

    // ── Reflection-free field setters for fields without public setters ───

    private static void setOrderId(Order o, String id) {
        try {
            var f = Order.class.getDeclaredField("orderId");
            f.setAccessible(true);
            f.set(o, id);
        } catch (Exception ignored) {}
    }

    private static void setPlacedAt(Order o, LocalDateTime dt) {
        try {
            var f = Order.class.getDeclaredField("placedAt");
            f.setAccessible(true);
            f.set(o, dt);
        } catch (Exception ignored) {}
    }

    private static void setTotalAmount(Order o, double v) {
        try {
            var f = Order.class.getDeclaredField("totalAmount");
            f.setAccessible(true);
            f.setDouble(o, v);
        } catch (Exception ignored) {}
    }

    private static void setBuybackCredit(Order o, double v) {
        try {
            var f = Order.class.getDeclaredField("buybackCredit");
            f.setAccessible(true);
            f.setDouble(o, v);
        } catch (Exception ignored) {}
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }

    // ── Real-time order status listener ──────────────────────────────────

    private static final java.util.Map<String, ValueEventListener> STATUS_LISTENERS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Attaches a real-time Firebase listener on orders/{uid}/.
     * Whenever any order's status changes in Firebase the in-memory cache
     * is refreshed and onUpdate is called on the EDT.
     * Safe to call multiple times — previous listener for the same uid is
     * removed first to avoid duplicates.
     */
    public static void attachOrderStatusListener(String uid, Runnable onUpdate) {
        if (!FirebaseConfig.isInitialised() || uid == null) return;

        detachOrderStatusListener(uid); // remove stale listener if any

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference(AppConfig.FIREBASE_ORDERS_NODE)
                .child(uid);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Order> fresh = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Order o = mapToOrder(snap, uid);
                    if (o != null) fresh.add(o);
                }
                com.dripdoor.controller.OrderController.replaceCache(uid, fresh);
                javax.swing.SwingUtilities.invokeLater(onUpdate);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("[Firebase] statusListener cancelled: " + error.getMessage());
            }
        };

        ref.addValueEventListener(listener);
        STATUS_LISTENERS.put(uid, listener);
    }

    /**
     * Removes the real-time listener for the given uid (if any).
     */
    public static void detachOrderStatusListener(String uid) {
        if (!FirebaseConfig.isInitialised() || uid == null) return;
        ValueEventListener listener = STATUS_LISTENERS.remove(uid);
        if (listener != null) {
            FirebaseDatabase.getInstance()
                    .getReference(AppConfig.FIREBASE_ORDERS_NODE)
                    .child(uid)
                    .removeEventListener(listener);
        }
    }



    /**
     * Persists the user's wishlist to wishlists/{uid}/.
     * Each product is stored as a child keyed by its productId.
     */
    public static boolean saveWishlist(String uid, List<com.dripdoor.model.Product> products) {
        if (!FirebaseConfig.isInitialised() || uid == null) return false;
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference(AppConfig.FIREBASE_WISHLISTS_NODE)
                    .child(uid);

            // Build map: productId -> { id, name, category, price, imageUrl }
            Map<String, Object> wishMap = new HashMap<>();
            for (com.dripdoor.model.Product p : products) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id",       p.getId());
                pm.put("name",     nullSafe(p.getName()));
                pm.put("category", nullSafe(p.getCategory()));
                pm.put("price",    p.getPrice());
                pm.put("cloudinaryPublicId", nullSafe(p.getCloudinaryPublicId()));
                wishMap.put(p.getId(), pm);
            }

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            // setValue replaces the whole node — gives us clean remove semantics
            ref.setValue(wishMap.isEmpty() ? null : wishMap, (error, r) -> {
                success[0] = (error == null);
                if (error != null)
                    System.err.println("[Firebase] saveWishlist failed: " + error.getMessage());
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);
            return success[0];

        } catch (Exception e) {
            System.err.println("[Firebase] saveWishlist error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads the wishlist for the given UID from wishlists/{uid}/.
     * Returns product stubs; ProductCatalogView will match them against the live
     * catalogue by ID so the latest price/image is always shown.
     */
    public static List<com.dripdoor.model.Product> loadWishlist(String uid) {
        List<com.dripdoor.model.Product> result = new ArrayList<>();
        if (!FirebaseConfig.isInitialised() || uid == null) return result;
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference(AppConfig.FIREBASE_WISHLISTS_NODE)
                    .child(uid);

            CountDownLatch latch = new CountDownLatch(1);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        try {
                            String id       = snap.child("id").getValue(String.class);
                            String name     = snap.child("name").getValue(String.class);
                            String category = snap.child("category").getValue(String.class);
                            Double price    = snap.child("price").getValue(Double.class);
                            String cloudinaryPublicId = snap.child("cloudinaryPublicId").getValue(String.class);
                            if (id != null) {
                                result.add(new com.dripdoor.model.Product(
                                        id,
                                        name     != null ? name     : "",
                                        category != null ? category : "",
                                        price    != null ? price    : 0.0,
                                        cloudinaryPublicId != null ? cloudinaryPublicId : ""
                                ));
                            }
                        } catch (Exception ex) {
                            System.err.println("[Firebase] loadWishlist item error: " + ex.getMessage());
                        }
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("[Firebase] loadWishlist cancelled: " + error.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("[Firebase] loadWishlist error: " + e.getMessage());
        }
        return result;
    }
}
