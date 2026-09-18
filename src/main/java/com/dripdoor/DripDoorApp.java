package com.dripdoor;

import com.dripdoor.config.FirebaseConfig;
import com.dripdoor.config.ThemeConfig;
import com.dripdoor.view.LoginView;

import javax.swing.*;

/**
 * DripDoor — Luxury E-Commerce Boutique
 * Entry point. Bootstraps FlatLaf and launches the Login screen.
 */
public class DripDoorApp {

    public static void main(String[] args) {
        
        ThemeConfig.install();
        com.dripdoor.config.FirebaseConfig.init();

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            loginView.setVisible(true);
        });
    }
}
