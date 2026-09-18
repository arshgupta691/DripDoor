package com.dripdoor.util;

import com.dripdoor.config.AppConfig;

/**
 * Circular Drip Math Module.
 *
 * DripDoor's buy-back programme returns 70 % of the purchase value
 * to the customer as redeemable Drip Credit on their next order.
 */
public final class CircularDripCalculator {

    private CircularDripCalculator() {}

    /**
     * Calculates the Circular Drip credit for a given order total.
     *
     * @param orderTotal the gross purchase amount in INR
     * @return  the buy-back credit amount (70 % of orderTotal)
     */
    public static double calculate(double orderTotal) {
        return orderTotal * AppConfig.BUYBACK_RATE;
    }

    /**
     * Returns a formatted summary string for display in the UI.
     *
     * @param orderTotal the gross purchase amount
     * @return  human-readable credit statement
     */
    public static String summary(double orderTotal) {
        double credit = calculate(orderTotal);
        return String.format(
            "Your Circular Drip Credit: ₹%.2f  (%.0f%% of ₹%.2f)",
            credit, AppConfig.BUYBACK_RATE * 100, orderTotal
        );
    }
}
