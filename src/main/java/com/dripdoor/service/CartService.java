package com.dripdoor.service;

import com.dripdoor.config.AppConfig;
import com.dripdoor.config.FirebaseConfig;
import com.dripdoor.model.CartItem;
import com.dripdoor.model.Product;
import com.dripdoor.controller.ProductController;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Persists and loads the shopping cart for each user under:
 *   carts/{uid}/items/{productId}  →  { productId, quantity }
 *
 * Falls back to in-memory only when Firebase is not initialised.
 */
public class CartService {

    private static final String NODE = "carts";

    // ── Save entire cart (called after every add/remove) ──────────────────

    public static void saveCart(String uid, List<CartItem> items) {
        if (!FirebaseConfig.isInitialised() || uid == null) return;

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference(NODE)
                    .child(uid)
                    .child("items");

            // Overwrite the whole items node with current cart
            Map<String, Object> itemsMap = new HashMap<>();
            for (CartItem item : items) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("productId", item.getProduct().getId());
                entry.put("quantity",  item.getQuantity());
                itemsMap.put(item.getProduct().getId(), entry);
            }

            ref.setValue(itemsMap, (error, r) -> {
                if (error != null)
                    System.err.println("[CartService] save failed: " + error.getMessage());
            });

        } catch (Exception e) {
            System.err.println("[CartService] saveCart exception: " + e.getMessage());
        }
    }

    // ── Load cart on login ────────────────────────────────────────────────

    /**
     * Loads the user's persisted cart from Firebase and populates
     * ProductController's per-user cart via addToCart.
     */
    public static void loadCart(String uid) {
        if (!FirebaseConfig.isInitialised() || uid == null) return;

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference(NODE)
                    .child(uid)
                    .child("items");

            CountDownLatch latch = new CountDownLatch(1);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Product> catalogue = ProductController.getCatalogue();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String productId = child.child("productId").getValue(String.class);
                        Long   qtyLong   = child.child("quantity").getValue(Long.class);
                        int    qty       = (qtyLong != null) ? qtyLong.intValue() : 1;

                        catalogue.stream()
                                .filter(p -> p.getId().equals(productId))
                                .findFirst()
                                .ifPresent(p -> ProductController.addToCart(p, qty));
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("[CartService] load cancelled: " + error.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("[CartService] loadCart exception: " + e.getMessage());
        }
    }

    // ── Clear cart in DB on checkout / logout ─────────────────────────────

    public static void clearCart(String uid) {
        if (!FirebaseConfig.isInitialised() || uid == null) return;
        try {
            FirebaseDatabase.getInstance()
                    .getReference(NODE)
                    .child(uid)
                    .child("items")
                    .removeValue((error, r) -> {
                        if (error != null)
                            System.err.println("[CartService] clear failed: " + error.getMessage());
                    });
        } catch (Exception e) {
            System.err.println("[CartService] clearCart exception: " + e.getMessage());
        }
    }
}
