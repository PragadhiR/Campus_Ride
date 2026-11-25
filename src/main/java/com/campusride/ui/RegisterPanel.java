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
 * Registration panel for student onboarding with input validation.
 */
public class RegisterPanel extends JPanel {
    private final MainFrame mainFrame;
    private final AuthService authService;

    private JTextField txtName;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JComboBox<String> cbDepartment;
    private JSpinner spinYear;
    private JComboBox<String> cbGender;
    private JLabel lblError;
    private ModernButton btnRegister;

    public RegisterPanel(MainFrame mainFrame) {
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
            new EmptyBorder(28, 36, 28, 36)
        ));
        card.setPreferredSize(new Dimension(500, 680));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        int row = 0;

        // Title
        JLabel lblTitle = new JLabel("Create Student Account", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_HEADER);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridy = row++;
        card.add(lblTitle, gbc);

        JLabel lblSub = new JLabel("Join your campus ride-pooling community", SwingConstants.CENTER);
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblSub, gbc);

        // Error message
        lblError = new JLabel("", SwingConstants.CENTER);
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.STATUS_CANCELLED_FG);
        gbc.gridy = row++;
        card.add(lblError, gbc);

        // Full Name
        JLabel lblName = new JLabel("Full Name");
        lblName.setFont(UITheme.FONT_BODY_BOLD);
        lblName.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblName, gbc);

        txtName = new JTextField();
        UITheme.styleTextField(txtName);
        gbc.gridy = row++;
        card.add(txtName, gbc);

        // College Email
        JLabel lblEmail = new JLabel("College Email");
        lblEmail.setFont(UITheme.FONT_BODY_BOLD);
        lblEmail.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblEmail, gbc);

        txtEmail = new JTextField();
        UITheme.styleTextField(txtEmail);
        gbc.gridy = row++;
        card.add(txtEmail, gbc);

        // Password
        JLabel lblPass = new JLabel("Password (min 6 characters)");
        lblPass.setFont(UITheme.FONT_BODY_BOLD);
        lblPass.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblPass, gbc);

        txtPassword = new JPasswordField();
        UITheme.styleTextField(txtPassword);
        gbc.gridy = row++;
        card.add(txtPassword, gbc);

        // Department
        JLabel lblDept = new JLabel("Department");
        lblDept.setFont(UITheme.FONT_BODY_BOLD);
        lblDept.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridy = row++;
        card.add(lblDept, gbc);

        cbDepartment = new JComboBox<>(new String[]{
            "Computer Science", "Information Science", "Electronics & Comm",
            "Electrical Eng", "Mechanical Eng", "Civil Eng", "Biotech", "Business School"
        });
        UITheme.styleComboBox(cbDepartment);
        gbc.gridy = row++;
        card.add(cbDepartment, gbc);

        // Row with Year of Study & Gender
        JPanel rowPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        rowPanel.setOpaque(false);

        JPanel pnlYear = new JPanel(new BorderLayout(4, 4));
        pnlYear.setOpaque(false);
        JLabel lblYear = new JLabel("Year of Study");
        lblYear.setFont(UITheme.FONT_BODY_BOLD);
        lblYear.setForeground(UITheme.TEXT_SECONDARY);
        spinYear = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        spinYear.setFont(UITheme.FONT_BODY);
        pnlYear.add(lblYear, BorderLayout.NORTH);
        pnlYear.add(spinYear, BorderLayout.CENTER);

        JPanel pnlGender = new JPanel(new BorderLayout(4, 4));
        pnlGender.setOpaque(false);
        JLabel lblGender = new JLabel("Gender");
        lblGender.setFont(UITheme.FONT_BODY_BOLD);
        lblGender.setForeground(UITheme.TEXT_SECONDARY);
        cbGender = new JComboBox<>(new String[]{"Female", "Male", "Non-Binary", "Prefer not to say"});
        UITheme.styleComboBox(cbGender);
        pnlGender.add(lblGender, BorderLayout.NORTH);
        pnlGender.add(cbGender, BorderLayout.CENTER);

        rowPanel.add(pnlYear);
        rowPanel.add(pnlGender);

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 6, 8, 6);
        card.add(rowPanel, gbc);

        // Register Button
        btnRegister = new ModernButton("Complete Registration", ModernButton.Variant.SUCCESS);
        btnRegister.addActionListener(e -> performRegister());
        gbc.gridy = row++;
        gbc.insets = new Insets(16, 6, 6, 6);
        card.add(btnRegister, gbc);

        // Back to Login Link
        ModernButton btnBackLogin = new ModernButton("Already registered? Back to Login", ModernButton.Variant.OUTLINE);
        btnBackLogin.addActionListener(e -> mainFrame.showCard(MainFrame.SCREEN_LOGIN));
        gbc.gridy = row++;
        gbc.insets = new Insets(6, 6, 6, 6);
        card.add(btnBackLogin, gbc);

        add(card);
    }

    private void performRegister() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());
        String department = (String) cbDepartment.getSelectedItem();
        int year = (Integer) spinYear.getValue();
        String gender = (String) cbGender.getSelectedItem();

        lblError.setText("");
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        SwingWorker<Student, Void> worker = new SwingWorker<>() {
            @Override
            protected Student doInBackground() throws Exception {
                return authService.register(name, email, password, department, year, gender);
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                btnRegister.setText("Complete Registration");
                try {
                    Student student = get();
                    JOptionPane.showMessageDialog(
                        RegisterPanel.this,
                        "Welcome to CampusRide, " + student.getFullName() + "!\nYour account has been created successfully.",
                        "Registration Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    clearFields();
                    mainFrame.onLoginSuccess(student);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ValidationException) {
                        lblError.setText(cause.getMessage());
                    } else {
                        lblError.setText("Error during registration: " + cause.getMessage());
                    }
                }
            }
        };
        worker.execute();
    }

    public void clearFields() {
        txtName.setText("");
        txtEmail.setText("");
        txtPassword.setText("");
        cbDepartment.setSelectedIndex(0);
        spinYear.setValue(1);
        cbGender.setSelectedIndex(0);
        lblError.setText("");
    }
}
