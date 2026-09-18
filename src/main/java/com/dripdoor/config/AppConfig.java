package com.dripdoor.config;

/**
 * Central application configuration -- API keys, project IDs, and constants.
 *
 * Gemini is called via the Google AI (generativelanguage.googleapis.com) REST API
 * using the Firebase Web API key -- this is the same key Firebase AI Logic uses
 * internally as its proxy. No separate Gemini API key is needed.
 *
 * How to get your key:
 *   Firebase Console -> Project Settings -> General -> Web API Key
 */
public final class AppConfig {

    private AppConfig() {}

    /* -- Firebase -------------------------------------------------------- */
    public static final String FIREBASE_PROJECT_ID  = "YOUR_FIREBASE_PROJECT_ID";
    public static final String FIREBASE_DB_URL       = "YOUR_FIREBASE_DB_URL";
    public static final String FIREBASE_ORDERS_NODE     = "orders";
    public static final String FIREBASE_WISHLISTS_NODE  = "wishlists";

    /**
     * Firebase Web API key.
     * Found in: Firebase Console -> Project Settings -> General -> Web API Key
     * This same key is used to authenticate Gemini API calls.
     */
    public static final String FIREBASE_WEB_API_KEY =
            System.getenv("FIREBASE_WEB_API_KEY") != null
            ? System.getenv("FIREBASE_WEB_API_KEY")
            : "YOUR_FIREBASE_API_KEY";

    /* -- Gemini (via Firebase / Google AI) ------------------------------ */
    /**
     * gemini-2.0-flash shuts down June 1 2026.
     * Using gemini-2.5-flash-lite -- the recommended replacement.
     * You can also use "gemini-2.5-flash" for better quality.
     */
    public static final String GEMINI_MODEL = "gemini-2.5-flash-lite";

    /**
     * REST endpoint -- uses the Firebase Web API key for auth.
     * This is exactly what Firebase AI Logic uses under the hood.
     */
   // Change this in your AppConfig.java
    public static final String GEMINI_KEY = "YOUR_API_KEY";
    public static final String GEMINI_ENDPOINT = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_KEY;
    /* -- Cloudinary ------------------------------------------------------ */
    public static final String CLOUDINARY_CLOUD    = "dzehllngp";
    public static final String CLOUDINARY_BASE_URL = "https://res.cloudinary.com/"
            + CLOUDINARY_CLOUD + "/image/upload/";

    /* -- Business Rules -------------------------------------------------- */
    public static final double BUYBACK_RATE     = 0.70;
    public static final int    DISPATCH_SECONDS = 600;
}
