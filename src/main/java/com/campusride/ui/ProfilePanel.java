package com.campusride.ui;

import com.campusride.model.Student;
import com.campusride.ui.components.ModernButton;
import com.campusride.util.DateUtil;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Screen presenting student profile details and account identity.
 */
public class ProfilePanel extends JPanel {
    private final MainFrame mainFrame;

    private JLabel lblName;
    private JLabel lblEmail;
    private JLabel lblDepartment;
    private JLabel lblYear;
    private JLabel lblGender;
    private JLabel lblMemberSince;
    private JLabel lblStudentId;

    public ProfilePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // TOP HEADER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Student Profile");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Your authenticated campus identity and enrollment details.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // CENTER CARD
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(UITheme.createCardBorder());
        card.setPreferredSize(new Dimension(550, 480));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 14, 10, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Avatar / Initials Circle
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.PRIMARY);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                String initial = "S";
                Student s = mainFrame.getCurrentStudent();
                if (s != null && s.getFullName() != null && !s.getFullName().isEmpty()) {
                    initial = s.getFullName().substring(0, 1).toUpperCase();
                }
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(64, 64));
        avatarPanel.setOpaque(false);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(avatarPanel, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;

        // Fields
        lblName = addFieldRow(card, "Full Name", "", row++);
        lblEmail = addFieldRow(card, "College Email", "", row++);
        lblStudentId = addFieldRow(card, "Student ID", "", row++);
        lblDepartment = addFieldRow(card, "Department", "", row++);
        lblYear = addFieldRow(card, "Year of Study", "", row++);
        lblGender = addFieldRow(card, "Gender", "", row++);
        lblMemberSince = addFieldRow(card, "Member Since", "", row++);

        // Actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 16));
        btnPanel.setOpaque(false);

        ModernButton btnLogout = new ModernButton("Log Out", ModernButton.Variant.DANGER);
        btnLogout.addActionListener(e -> mainFrame.logout());

        btnPanel.add(btnLogout);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        card.add(btnPanel, gbc);

        JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        outer.setOpaque(false);
        outer.add(card);

        add(outer, BorderLayout.CENTER);
    }

    private JLabel addFieldRow(JPanel card, String label, String value, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 14, 6, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridx = 0; gbc.gridy = row;
        card.add(lbl, gbc);

        JLabel val = new JLabel(value);
        val.setFont(UITheme.FONT_BODY);
        val.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridx = 1; gbc.gridy = row;
        card.add(val, gbc);

        return val;
    }

    public void refreshData() {
        Student s = mainFrame.getCurrentStudent();
        if (s == null) return;

        lblName.setText(s.getFullName());
        lblEmail.setText(s.getEmail());
        lblStudentId.setText("#" + s.getStudentId());
        lblDepartment.setText(s.getDepartment());
        lblYear.setText("Year " + s.getYearOfStudy());
        lblGender.setText(s.getGender());
        lblMemberSince.setText(s.getCreatedAt() != null ? DateUtil.formatDateOnly(s.getCreatedAt()) : "Recent");
        repaint();
    }
}
