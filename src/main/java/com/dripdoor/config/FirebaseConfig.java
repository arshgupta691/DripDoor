package com.dripdoor.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Initialises the Firebase Admin SDK.
 *
 * Place your serviceAccountKey.json at:
 *   src/main/resources/config/serviceAccountKey.json
 *
 * The app runs in offline/demo mode if the key is absent.
 */
public class FirebaseConfig {

    private static boolean initialised = false;

    public static synchronized void init() {
        if (initialised || !FirebaseApp.getApps().isEmpty()) {
            initialised = true;
            return;
        }
        try {
            InputStream is = FirebaseConfig.class
                    .getResourceAsStream("/config/serviceAccountKey.json");

            if (is == null) {
                // Fallback: try file-system path
                is = new FileInputStream("src/main/resources/config/serviceAccountKey.json");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .setDatabaseUrl(AppConfig.FIREBASE_DB_URL)
                    .setProjectId(AppConfig.FIREBASE_PROJECT_ID)
                    .build();

            FirebaseApp.initializeApp(options);
            initialised = true;
            System.out.println("[Firebase] Initialised for project: " + AppConfig.FIREBASE_PROJECT_ID);

        } catch (Exception e) {
            System.err.println("[Firebase] Running in OFFLINE mode — " + e.getMessage());
            initialised = true; // prevent repeated attempts
        }
    }

    public static boolean isInitialised() { return initialised; }
}
