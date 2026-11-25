package com.campusride.ui;

import com.campusride.config.AppConfig;
import com.campusride.db.DBConnection;
import com.campusride.ui.components.ModernButton;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

/**
 * Modal dialog for configuring and validating MySQL database credentials
 * with live connection testing and local persistence.
 */
public class DatabaseSetupDialog extends JDialog {
    private final JTextField txtHost;
    private final JTextField txtPort;
    private final JTextField txtDbName;
    private final JTextField txtUser;
    private final JPasswordField txtPassword;
    private final JLabel lblStatus;

    public DatabaseSetupDialog(Frame parent) {
        super(parent, "Database Configuration & Connection", true);
        setSize(460, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_DARKEST);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UITheme.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
            new EmptyBorder(24, 28, 24, 28)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        int row = 0;

        JLabel lblTitle = new JLabel("MySQL Database Settings", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_HEADER);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridy = row++;
        panel.add(lblTitle, gbc);

        lblStatus = new JLabel("Adjust your local MySQL credentials if needed", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        panel.add(lblStatus, gbc);

        // Host
        panel.add(createLabel("Host"), gbcWithRow(gbc, row++));
        txtHost = new JTextField(AppConfig.getDbHost());
        UITheme.styleTextField(txtHost);
        panel.add(txtHost, gbcWithRow(gbc, row++));

        // Port & DB Name row
        JPanel rowPortDb = new JPanel(new GridLayout(1, 2, 8, 0));
        rowPortDb.setOpaque(false);

        JPanel pnlPort = new JPanel(new BorderLayout(2, 2));
        pnlPort.setOpaque(false);
        pnlPort.add(createLabel("Port"), BorderLayout.NORTH);
        txtPort = new JTextField(String.valueOf(AppConfig.getDbPort()));
        UITheme.styleTextField(txtPort);
        pnlPort.add(txtPort, BorderLayout.CENTER);

        JPanel pnlDb = new JPanel(new BorderLayout(2, 2));
        pnlDb.setOpaque(false);
        pnlDb.add(createLabel("Database Name"), BorderLayout.NORTH);
        txtDbName = new JTextField(AppConfig.getDbName());
        UITheme.styleTextField(txtDbName);
        pnlDb.add(txtDbName, BorderLayout.CENTER);

        rowPortDb.add(pnlPort);
        rowPortDb.add(pnlDb);
        panel.add(rowPortDb, gbcWithRow(gbc, row++));

        // User
        panel.add(createLabel("Username"), gbcWithRow(gbc, row++));
        txtUser = new JTextField(AppConfig.getDbUser());
        UITheme.styleTextField(txtUser);
        panel.add(txtUser, gbcWithRow(gbc, row++));

        // Password
        panel.add(createLabel("Password"), gbcWithRow(gbc, row++));
        txtPassword = new JPasswordField(AppConfig.getDbPassword());
        UITheme.styleTextField(txtPassword);
        panel.add(txtPassword, gbcWithRow(gbc, row++));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setOpaque(false);

        ModernButton btnTest = new ModernButton("Test Connection", ModernButton.Variant.SECONDARY);
        btnTest.addActionListener(e -> testConn());

        ModernButton btnSave = new ModernButton("Save & Connect", ModernButton.Variant.PRIMARY);
        btnSave.addActionListener(e -> saveAndClose());

        btnPanel.add(btnTest);
        btnPanel.add(btnSave);
        panel.add(btnPanel, gbcWithRow(gbc, row++));

        getContentPane().add(panel);
    }

    private GridBagConstraints gbcWithRow(GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        return gbc;
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    private void testConn() {
        lblStatus.setText("Testing connection...");
        lblStatus.setForeground(UITheme.TEXT_SECONDARY);

        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            lblStatus.setText("Invalid port number.");
            lblStatus.setForeground(UITheme.STATUS_CANCELLED_FG);
            return;
        }
        String dbName = txtDbName.getText().trim();
        String user = txtUser.getText().trim();
        String password = new String(txtPassword.getPassword());

        new Thread(() -> {
            try {
                DBConnection.validateCredentials(host, port, dbName, user, password);
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("✓ Connection Successful! Ready to use.");
                    lblStatus.setForeground(UITheme.STATUS_OPEN_FG);
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Connection failed: " + ex.getMessage());
                    lblStatus.setForeground(UITheme.STATUS_CANCELLED_FG);
                });
            }
        }).start();
    }

    private void saveAndClose() {
        String host = txtHost.getText().trim();
        int port = Integer.parseInt(txtPort.getText().trim());
        String dbName = txtDbName.getText().trim();
        String user = txtUser.getText().trim();
        String password = new String(txtPassword.getPassword());

        try {
            AppConfig.saveConfig(host, port, dbName, user, password);
            JOptionPane.showMessageDialog(this, "Database configuration saved successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to save configuration: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
