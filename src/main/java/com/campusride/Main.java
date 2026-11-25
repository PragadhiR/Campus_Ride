package com.campusride;

import com.campusride.db.DBConnection;
import com.campusride.ui.DatabaseSetupDialog;
import com.campusride.ui.MainFrame;

import javax.swing.*;

/**
 * CampusRide Application Entry Point.
 * Initializes desktop Look and Feel, verifies database readiness, and launches MainFrame.
 */
public class Main {
    public static void main(String[] args) {
        // Set cross-platform dark system properties where available
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            // Background check for database availability on initial launch
            new Thread(() -> {
                boolean dbConnected = DBConnection.testConnection();
                if (!dbConnected) {
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showConfirmDialog(
                            frame,
                            "CampusRide could not connect to MySQL with default credentials.\n" +
                            "Would you like to configure your local MySQL credentials now?",
                            "Database Connection Required",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        if (choice == JOptionPane.YES_OPTION) {
                            new DatabaseSetupDialog(frame).setVisible(true);
                        }
                    });
                }
            }).start();
        });
    }
}
