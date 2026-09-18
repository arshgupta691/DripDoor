package com.dripdoor.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.dripdoor.config.AppConfig;
import com.dripdoor.service.AddressService;
import com.dripdoor.config.FirebaseConfig;
import com.dripdoor.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONObject;

/**
 * Authentication service.
 *
 * Registration flow:
 *   1. Check uniqueness in Realtime Database.
 *   2. Create Firebase Auth user via REST API (signUp endpoint).
 *   3. Send email-verification link via REST API.
 *   4. Save user profile to Realtime Database (emailVerified = false).
 *
 * Login flow:
 *   1. Sign in via Firebase Auth REST API.
 *   2. Confirm emailVerified flag is true.
 *   3. Load profile from Realtime Database.
 */
public class AuthService {

    private static User currentUser = null;

    // ── Firebase Auth REST base URL ───────────────────────────────────────
    private static final String AUTH_BASE =
            "https://identitytoolkit.googleapis.com/v1/accounts:";

    // ── Registration ──────────────────────────────────────────────────────

    public enum RegisterResult { SUCCESS, EMAIL_EXISTS, WEAK_PASSWORD, ERROR }

    public static RegisterResult register(String name, String email, String password) {
        if (password.length() < 6)
            return RegisterResult.WEAK_PASSWORD;

        String emailKey = toKey(email);

        try {
            // ── 1. Check Realtime DB for existing user ────────────────────
            if (FirebaseConfig.isInitialised()) {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("users").child(emailKey);

                CountDownLatch latch = new CountDownLatch(1);
                final boolean[] exists      = {false};
                final boolean[] checkFailed = {false};

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot s) {
                        exists[0] = s.exists();
                        latch.countDown();
                    }
                    @Override public void onCancelled(DatabaseError e) {
                        checkFailed[0] = true;
                        latch.countDown();
                    }
                });

                if (!latch.await(15, TimeUnit.SECONDS) || checkFailed[0])
                    return RegisterResult.ERROR;
                if (exists[0])
                    return RegisterResult.EMAIL_EXISTS;
            }

            // ── 2. Create Firebase Auth user via REST ─────────────────────
            JSONObject signUpBody = new JSONObject();
            signUpBody.put("email", email.toLowerCase());
            signUpBody.put("password", password);
            signUpBody.put("returnSecureToken", true);

            JSONObject signUpResp = firebaseAuthPost("signUp", signUpBody);

            if (signUpResp.has("error")) {
                String errMsg = signUpResp.getJSONObject("error").optString("message", "");
                System.err.println("[AuthService] signUp error: " + errMsg);
                if (errMsg.contains("EMAIL_EXISTS"))
                    return RegisterResult.EMAIL_EXISTS;
                if (errMsg.contains("WEAK_PASSWORD"))
                    return RegisterResult.WEAK_PASSWORD;
                return RegisterResult.ERROR;
            }

            String idToken = signUpResp.optString("idToken");
            String uid     = signUpResp.optString("localId");

            // ── 3. Send email verification ────────────────────────────────
            JSONObject verifyBody = new JSONObject();
            verifyBody.put("requestType", "VERIFY_EMAIL");
            verifyBody.put("idToken", idToken);

            JSONObject verifyResp = firebaseAuthPost("sendOobCode", verifyBody);
            if (verifyResp.has("error")) {
                System.err.println("[AuthService] sendOobCode error: "
                        + verifyResp.getJSONObject("error").optString("message"));
                // Non-fatal — user is created; verification email is best-effort
            } else {
                System.out.println("[AuthService] Verification email sent to: " + email);
            }

            // ── 4. Save profile to Realtime Database ──────────────────────
            if (FirebaseConfig.isInitialised()) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("uid",           uid);
                userData.put("name",          name);
                userData.put("email",         email.toLowerCase());
                userData.put("passwordHash",  hash(password));
                userData.put("emailVerified", false);

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("users").child(emailKey);

                CountDownLatch saveLatch = new CountDownLatch(1);
                final boolean[] saved = {false};

                ref.setValue(userData, (firebaseError, ref2) -> {
                    if (firebaseError != null) {
                        System.err.println("[AuthService] DB write failed: "
                                + firebaseError.getMessage());
                    } else {
                        saved[0] = true;
                    }
                    saveLatch.countDown();
                });

                if (!saveLatch.await(15, TimeUnit.SECONDS) || !saved[0])
                    return RegisterResult.ERROR;
            }

            return RegisterResult.SUCCESS;

        } catch (Exception e) {
            System.err.println("[AuthService] Register exception: " + e.getMessage());
            e.printStackTrace();
            return RegisterResult.ERROR;
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────

    public enum LoginResult { SUCCESS, INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED, ERROR }

    public static LoginResult login(String email, String password) {
        try {
            // ── 1. Sign in via Firebase Auth REST ─────────────────────────
            JSONObject body = new JSONObject();
            body.put("email",            email.toLowerCase());
            body.put("password",         password);
            body.put("returnSecureToken", true);

            JSONObject resp = firebaseAuthPost("signInWithPassword", body);

            if (resp.has("error")) {
                String msg = resp.getJSONObject("error").optString("message", "");
                System.out.println("[AuthService] signIn error: " + msg);
                if (msg.contains("EMAIL_NOT_FOUND") || msg.contains("INVALID_PASSWORD")
                        || msg.contains("INVALID_LOGIN_CREDENTIALS"))
                    return LoginResult.INVALID_CREDENTIALS;
                return LoginResult.ERROR;
            }

            // ── 2. Check emailVerified via getAccountInfo ─────────────────
            String idToken = resp.optString("idToken");

            JSONObject infoBody = new JSONObject();
            infoBody.put("idToken", idToken);
            JSONObject infoResp = firebaseAuthPost("lookup", infoBody);

            boolean emailVerified = false;
            String uid  = resp.optString("localId");
            String name = "";

            if (!infoResp.has("error")) {
                JSONObject userInfo = infoResp.optJSONArray("users") != null
                        ? infoResp.getJSONArray("users").optJSONObject(0)
                        : new JSONObject();
                emailVerified = userInfo.optBoolean("emailVerified", false);
            }

            if (!emailVerified) {
                System.out.println("[AuthService] Email not verified for: " + email);
                return LoginResult.EMAIL_NOT_VERIFIED;
            }

            // ── 3. Load display name from Realtime DB ─────────────────────
            if (FirebaseConfig.isInitialised()) {
                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("users").child(toKey(email));

                CountDownLatch latch = new CountDownLatch(1);
                final String[] nameHolder = {""};

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot s) {
                        nameHolder[0] = s.child("name").getValue(String.class) != null
                                ? s.child("name").getValue(String.class) : "";
                        // Mark emailVerified true in DB
                        s.getRef().child("emailVerified").getRef()
                                .setValue((Object) true, null);
                        latch.countDown();
                    }
                    @Override public void onCancelled(DatabaseError e) { latch.countDown(); }
                });

                latch.await(10, TimeUnit.SECONDS);
                name = nameHolder[0];
            }

            currentUser = new User(uid, name, email.toLowerCase());
            currentUser.setEmailVerified(true);
            // Load saved addresses, cart and orders from Firebase
            AddressService.loadAddresses(currentUser);
            com.dripdoor.service.CartService.loadCart(uid);
            com.dripdoor.controller.OrderController.loadOrdersForUser(uid);
            com.dripdoor.view.WishlistView.loadWishlistForUser(uid);
            return LoginResult.SUCCESS;

        } catch (Exception e) {
            System.err.println("[AuthService] Login exception: " + e.getMessage());
            return LoginResult.ERROR;
        }
    }

    // ── Resend verification email ─────────────────────────────────────────

    /**
     * Re-authenticates with password and resends the verification email.
     * Returns true if the email was sent successfully.
     */
    public static boolean resendVerificationEmail(String email, String password) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email.toLowerCase());
            body.put("password", password);
            body.put("returnSecureToken", true);

            JSONObject resp = firebaseAuthPost("signInWithPassword", body);
            if (resp.has("error")) return false;

            String idToken = resp.optString("idToken");

            JSONObject verifyBody = new JSONObject();
            verifyBody.put("requestType", "VERIFY_EMAIL");
            verifyBody.put("idToken", idToken);

            JSONObject verifyResp = firebaseAuthPost("sendOobCode", verifyBody);
            return !verifyResp.has("error");

        } catch (Exception e) {
            System.err.println("[AuthService] resendVerificationEmail error: " + e.getMessage());
            return false;
        }
    }

    // ── Session ───────────────────────────────────────────────────────────

    public static void logout() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            // Evict in-memory caches only — Firebase data is preserved for next login
            com.dripdoor.controller.ProductController.clearCartForUser(uid);
            com.dripdoor.controller.OrderController.clearSessionOrders(uid);
            com.dripdoor.view.WishlistView.clearWishlistForUser(uid);
        }
        currentUser = null;
    }
    public static User getCurrentUser()   { return currentUser; }
    public static boolean isLoggedIn()    { return currentUser != null; }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Posts JSON to a Firebase Auth REST endpoint and returns the parsed response.
     */
    private static JSONObject firebaseAuthPost(String endpoint, JSONObject payload)
            throws Exception {
        String urlStr = AUTH_BASE + endpoint + "?key=" + AppConfig.FIREBASE_WEB_API_KEY;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);

        byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }

        int status = conn.getResponseCode();
        java.io.InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        conn.disconnect();
        return new JSONObject(body);
    }

    private static String toKey(String email) {
        return email.toLowerCase()
                    .replace(".", "_dot_")
                    .replace("@", "_at_");
    }

    private static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
