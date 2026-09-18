package com.dripdoor.controller;

import com.dripdoor.service.GeminiService;

import java.util.List;

/**
 * Bridges AIStylistView with GeminiService.
 */
public class AIStylistController {

    /**
     * Fetches a personalised recommendation from Gemini (via Firebase AI).
     *
     * @param eventType  e.g. "Wedding", "Gala"
     * @param outfit     what the user is wearing, e.g. "navy blue lehenga"
     * @param extraNotes any other user preferences
     * @return formatted recommendation text
     */
    public static String getRecommendation(String eventType,
                                           String outfit,
                                           String extraNotes) {
        List<String> history = OrderController.getSessionOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getProduct().getName())
                .distinct()
                .limit(5)
                .toList();

        return GeminiService.recommend(eventType, outfit, extraNotes, history);
    }
}
