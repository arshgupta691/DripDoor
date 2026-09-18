package com.dripdoor.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.dripdoor.config.AppConfig;

/**
 * Calls Gemini through the Firebase AI (Vertex AI for Firebase) REST API.
 * No separate Gemini API key is needed -- authentication uses your Firebase
 * Web API key, so billing flows through your Firebase project.
 *
 * Enable the API:
 *   Firebase Console -> Build -> AI -> Get started -> enable Gemini API.
 */
public class GeminiService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Asks Gemini to recommend jewellery based on the user's event,
     * outfit description, and purchase history.
     *
     * @param eventType   e.g. "Wedding", "Gala"
     * @param outfit      what the user is wearing, e.g. "red silk saree"
     * @param extraNotes  any other preferences the user typed
     * @param history     previously purchased product names
     * @return formatted recommendation text
     */
    public static String recommend(String eventType,
                                   String outfit,
                                   String extraNotes,
                                   List<String> history) {
        String prompt      = buildPrompt(eventType, outfit, extraNotes, history);
        String requestBody = buildRequestBody(prompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AppConfig.GEMINI_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                } else if (response.statusCode() == 429 || response.statusCode() == 503) {
                    // High demand or rate limit hit.
                    if (attempt < maxRetries) {
                        System.out.println("[Gemini] High demand. Retrying attempt " + (attempt + 1) + "...");
                        Thread.sleep(2000 * attempt); // Wait 2s, then 4s before retrying
                        continue;
                    } else {
                        return "The AI Stylist is super busy right now. Please try again in a minute!";
                    }
                } else {
                    // It's a different error (e.g., 400 Bad Request)
                    System.err.println("[Gemini] HTTP " + response.statusCode() + ": " + response.body());
                    return extractApiError(response.body());
                }
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[Gemini] Request failed: " + e.getMessage());
            return "Connection error: Check your internet connection.";
        }
        
        return "An unknown error occurred.";
    }

    // -- Private helpers --------------------------------------------------

    private static String buildPrompt(String eventType, String outfit,
                                      String extraNotes, List<String> history) {
        String historyStr = (history == null || history.isEmpty())
                ? "no previous purchases"
                : String.join(", ", history);

        String outfitLine = (outfit == null || outfit.isBlank())
                ? "not specified"
                : outfit;

        String notesLine  = (extraNotes == null || extraNotes.isBlank())
                ? "none"
                : extraNotes;

        return """
               You are DripDoor AI Stylist, a luxury jewellery curator.
               
               Event: %s
               Outfit: %s
               Preferences: %s
               Purchase History: %s
               
               STRICT RULES:
               1. Read the user's Preferences carefully. If they ask for specific item types (like "earrings and necklace"), you MUST generate exactly those items.
               2. Do not write long, drawn-out paragraphs. Keep the 'Why' section to exactly one short sentence.
               3. Format your response exactly like the template below, with a blank line between items. Do not use Markdown formatting like **bold**.
               
               TEMPLATE:
               [Item Name]
               Category: [Category]
               Why: [One short sentence explaining the match]
               Price: [Estimated INR]
               """.formatted(eventType, outfitLine, notesLine, historyStr);
    }

    private static String buildRequestBody(String prompt) {
        JSONObject part    = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        
        JSONObject genCfg  = new JSONObject()
                .put("temperature", 0.3) // Lowered further to enforce the template
                .put("maxOutputTokens", 3000); // Bumped up so it never cuts off
        
        JSONObject body    = new JSONObject()
                .put("contents", new JSONArray().put(content))
                .put("generationConfig", genCfg);
        return body.toString();
    }

    private static String parseResponse(String body) {
        try {
            JSONObject json = new JSONObject(body);
        // Path: candidates[0] -> content -> parts[0] -> text
            return json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim();
         } catch (Exception e) {
            return "Parsing error. Raw response: " + body;
         }
        }

    private static String extractApiError(String body) {
        try {
            JSONObject json = new JSONObject(body);
            String msg = json.getJSONArray("error")
                            .getJSONObject(0)
                            .getString("message");
            return "Gemini API error: " + msg;
        } catch (Exception ignored) {}
        try {
            return "Gemini API error: "
                    + new JSONObject(body).getJSONObject("error").getString("message");
        } catch (Exception ignored) {}
        return "Gemini API error:\n" + body;
    }
}
