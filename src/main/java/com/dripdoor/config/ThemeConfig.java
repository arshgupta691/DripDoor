package com.dripdoor.config;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

/**
 * Installs FlatLightLaf and applies the Dior-inspired colour palette
 * and typography across the entire application.
 */
public class ThemeConfig {

    /* ── Palette ─────────────────────────────────────────────────────── */
    public static final Color WHITE = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SOFT_GREY = new Color(0xF8, 0xF8, 0xF8);
    public static final Color DEEP_BLACK = new Color(0x00, 0x00, 0x00);
    public static final Color GOLD = new Color(0xD4, 0xAF, 0x37);
    public static final Color GOLD_DARK = new Color(0xAA, 0x8C, 0x20);
    public static final Color BORDER = new Color(0xE0, 0xE0, 0xE0);

    /* ── Typography ──────────────────────────────────────────────────── */
    public static final Font HEADER_FONT = new Font("Georgia", Font.BOLD, 26);
    public static final Font SUB_FONT = new Font("Georgia", Font.ITALIC, 14);
    public static final Font UI_FONT = new Font("Arial", Font.PLAIN, 13);
    public static final Font UI_BOLD = new Font("Arial", Font.BOLD, 13);
    public static final Font SMALL_FONT = new Font("Arial", Font.PLAIN, 11);

    public static void install() {
        try {
            FlatLightLaf.setup();

            UIManager.put("Panel.background", SOFT_GREY);
            UIManager.put("TextField.background", WHITE);
            UIManager.put("PasswordField.background", WHITE);
            UIManager.put("Button.background", DEEP_BLACK);
            UIManager.put("Button.foreground", WHITE);
            UIManager.put("Button.hoverBackground", GOLD);
            UIManager.put("Button.hoverForeground", DEEP_BLACK);
            UIManager.put("Button.arc", 4);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.focusColor", GOLD);
            UIManager.put("Component.borderColor", BORDER);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbColor", new Color(0xD4, 0xAF, 0x37, 180));
            UIManager.put("ProgressBar.foreground", GOLD);
            UIManager.put("ProgressBar.background", BORDER);
            UIManager.put("ProgressBar.arc", 6);

            setGlobalFont(new FontUIResource(UI_FONT));

        } catch (Exception e) {
            System.err.println("[ThemeConfig] FlatLaf install failed: " + e.getMessage());
        }
    }

    private static void setGlobalFont(FontUIResource f) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }
}
