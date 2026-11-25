package com.campusride.ui;

import com.campusride.model.Student;
import com.campusride.service.AuthService;
import com.campusride.ui.components.ModernButton;
import com.campusride.util.UITheme;
import com.campusride.util.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modern login screen for CampusRide.
 */
public class LoginPanel extends JPanel {
    private final MainFrame mainFrame;
    private final AuthService authService;

    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private ModernButton btnLogin;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.authService = new AuthService();

        setLayout(new GridBagLayout());
        setBackground(UITheme.BG_DARKEST);

        initUI();
    }

    private void initUI() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1, true),
            new EmptyBorder(32, 40, 32, 40)
        ));
        card.setPreferredSize(new Dimension(460, 560));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        int row = 0;

        // Brand Title
        JLabel lblBrand = new JLabel("CAMPUSRIDE", SwingConstants.CENTER);
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblBrand.setForeground(UITheme.PRIMARY);
        gbc.gridy = row++;
        card.add(lblBrand, gbc);

        // Tagline
        JLabel lblTagline = new JLabel("Share the Ride. Split the Fare. Save More.", SwingConstants.CENTER);
        lblTagline.setFont(UITheme.FONT_SMALL);
        lblTagline.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblTagline, gbc);

        // Spacer
        gbc.gridy = row++;
        card.add(Box.createVerticalStrut(16), gbc);

        // Section Title
        JLabel lblSignIn = new JLabel("Student Login", SwingConstants.CENTER);
        lblSignIn.setFont(UITheme.FONT_HEADER);
        lblSignIn.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridy = row++;
        card.add(lblSignIn, gbc);

        // Error message label
        lblError = new JLabel("", SwingConstants.CENTER);
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.STATUS_CANCELLED_FG);
        gbc.gridy = row++;
        card.add(lblError, gbc);

        // Email field
        JLabel lblEmail = new JLabel("College Email");
        lblEmail.setFont(UITheme.FONT_BODY_BOLD);
        lblEmail.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblEmail, gbc);

        txtEmail = new JTextField();
        UITheme.styleTextField(txtEmail);
        gbc.gridy = row++;
        card.add(txtEmail, gbc);

        // Password field
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(UITheme.FONT_BODY_BOLD);
        lblPass.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblPass, gbc);

        txtPassword = new JPasswordField();
        UITheme.styleTextField(txtPassword);
        gbc.gridy = row++;
        card.add(txtPassword, gbc);

        // Enter key submits login
        txtPassword.addActionListener(e -> performLogin());

        // Login Button
        btnLogin = new ModernButton("Log In", ModernButton.Variant.PRIMARY);
        btnLogin.addActionListener(e -> performLogin());
        gbc.gridy = row++;
        gbc.insets = new Insets(16, 6, 6, 6);
        card.add(btnLogin, gbc);

        // Register link
        ModernButton btnGoRegister = new ModernButton("Don't have an account? Register Here", ModernButton.Variant.OUTLINE);
        btnGoRegister.addActionListener(e -> mainFrame.showCard(MainFrame.SCREEN_REGISTER));
        gbc.gridy = row++;
        gbc.insets = new Insets(8, 6, 6, 6);
        card.add(btnGoRegister, gbc);

        // Demo Fast-Fill Shortcuts
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        demoPanel.setOpaque(false);
        JLabel lblDemo = new JLabel("Demo fill:");
        lblDemo.setFont(UITheme.FONT_SMALL);
        lblDemo.setForeground(UITheme.TEXT_MUTED);
        demoPanel.add(lblDemo);

        JButton btnFillAarav = new JButton("Student A (Aarav)");
        btnFillAarav.setFont(UITheme.FONT_SMALL);
        btnFillAarav.setForeground(UITheme.ACCENT_CYAN);
        btnFillAarav.setBorderPainted(false);
        btnFillAarav.setContentAreaFilled(false);
        btnFillAarav.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFillAarav.addActionListener(e -> {
            txtEmail.setText("aarav.sharma@campus.edu");
            txtPassword.setText("Password@123");
            lblError.setText("");
        });
        demoPanel.add(btnFillAarav);

        JButton btnFillPriya = new JButton("Student B (Priya)");
        btnFillPriya.setFont(UITheme.FONT_SMALL);
        btnFillPriya.setForeground(UITheme.ACCENT_TEAL);
        btnFillPriya.setBorderPainted(false);
        btnFillPriya.setContentAreaFilled(false);
        btnFillPriya.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFillPriya.addActionListener(e -> {
            txtEmail.setText("priya.patel@campus.edu");
            txtPassword.setText("Password@123");
            lblError.setText("");
        });
        demoPanel.add(btnFillPriya);

        gbc.gridy = row++;
        gbc.insets = new Insets(12, 6, 4, 6);
        card.add(demoPanel, gbc);

        // Database settings button
        JButton btnDbSettings = new JButton("⚙ Database Settings");
        btnDbSettings.setFont(UITheme.FONT_SMALL);
        btnDbSettings.setForeground(UITheme.TEXT_MUTED);
        btnDbSettings.setBorderPainted(false);
        btnDbSettings.setContentAreaFilled(false);
        btnDbSettings.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDbSettings.addActionListener(e -> new DatabaseSetupDialog(mainFrame).setVisible(true));
        gbc.gridy = row++;
        gbc.insets = new Insets(4, 6, 4, 6);
        card.add(btnDbSettings, gbc);

        add(card);
    }

    private void performLogin() {
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());

        lblError.setText("");
        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");

        SwingWorker<Student, Void> worker = new SwingWorker<>() {
            @Override
            protected Student doInBackground() throws Exception {
                return authService.login(email, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Log In");
                try {
                    Student student = get();
                    txtPassword.setText("");
                    mainFrame.onLoginSuccess(student);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ValidationException) {
                        lblError.setText(cause.getMessage());
                    } else {
                        lblError.setText("Database error. Check MySQL service & settings.");
                    }
                }
            }
        };
        worker.execute();
    }

    public void clearFields() {
        txtEmail.setText("");
        txtPassword.setText("");
        lblError.setText("");
    }
}
