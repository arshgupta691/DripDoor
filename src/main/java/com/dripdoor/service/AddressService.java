package com.dripdoor.service;

import com.dripdoor.config.FirebaseConfig;
import com.dripdoor.model.SavedAddress;
import com.dripdoor.model.User;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Saves and loads delivery addresses under:
 *   users/{emailKey}/addresses/{addressId}
 *
 * Falls back to in-memory only when Firebase is not initialised.
 */
public class AddressService {

    /** Saves a new address to Firebase and adds it to the user's in-memory list. */
    public static boolean saveAddress(User user, SavedAddress address) {
        user.addSavedAddress(address);   // always update in-memory

        if (!FirebaseConfig.isInitialised()) {
            System.out.println("[AddressService DEMO] address saved in-memory only");
            return true;
        }

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(toKey(user.getEmail()))
                    .child("addresses")
                    .child(address.getId());

            Map<String, Object> map = addressToMap(address);

            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] ok = {false};

            ref.setValue(map, (error, r) -> {
                ok[0] = (error == null);
                if (error != null)
                    System.err.println("[AddressService] save failed: " + error.getMessage());
                latch.countDown();
            });

            latch.await(10, TimeUnit.SECONDS);
            return ok[0];

        } catch (Exception e) {
            System.err.println("[AddressService] exception: " + e.getMessage());
            return false;
        }
    }

    /** Loads all saved addresses from Firebase into the user's list. */
    public static void loadAddresses(User user) {
        if (!FirebaseConfig.isInitialised()) return;

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(toKey(user.getEmail()))
                    .child("addresses");

            CountDownLatch latch = new CountDownLatch(1);
            List<SavedAddress> loaded = new ArrayList<>();

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        SavedAddress a = mapToAddress(child);
                        if (a != null) loaded.add(a);
                    }
                    latch.countDown();
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("[AddressService] load cancelled: " + error.getMessage());
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            user.setSavedAddresses(loaded);

        } catch (Exception e) {
            System.err.println("[AddressService] load exception: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static Map<String, Object> addressToMap(SavedAddress a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",      a.getId());
        m.put("label",   a.getLabel());
        m.put("name",    a.getName());
        m.put("phone",   a.getPhone());
        m.put("line1",   a.getLine1());
        m.put("line2",   a.getLine2() != null ? a.getLine2() : "");
        m.put("city",    a.getCity());
        m.put("state",   a.getState());
        m.put("pincode", a.getPincode());
        return m;
    }

    private static SavedAddress mapToAddress(DataSnapshot s) {
        try {
            SavedAddress a = new SavedAddress();
            a.setId(     s.child("id").getValue(String.class));
            a.setLabel(  s.child("label").getValue(String.class));
            a.setName(   s.child("name").getValue(String.class));
            a.setPhone(  s.child("phone").getValue(String.class));
            a.setLine1(  s.child("line1").getValue(String.class));
            a.setLine2(  s.child("line2").getValue(String.class));
            a.setCity(   s.child("city").getValue(String.class));
            a.setState(  s.child("state").getValue(String.class));
            a.setPincode(s.child("pincode").getValue(String.class));
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    private static String toKey(String email) {
        return email.toLowerCase()
                    .replace(".", "_dot_")
                    .replace("@", "_at_");
    }
}
