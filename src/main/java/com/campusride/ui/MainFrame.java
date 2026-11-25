package com.campusride.ui;

import com.campusride.model.Student;
import com.campusride.service.AuthService;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application frame managing primary navigation, sidebar, top header,
 * and page transitions via CardLayout.
 */
public class MainFrame extends JFrame {
    public static final String SCREEN_LOGIN = "LOGIN";
    public static final String SCREEN_REGISTER = "REGISTER";
    public static final String SCREEN_DASHBOARD = "DASHBOARD";
    public static final String SCREEN_CREATE_RIDE = "CREATE_RIDE";
    public static final String SCREEN_FIND_RIDE = "FIND_RIDE";
    public static final String SCREEN_MY_RIDES = "MY_RIDES";
    public static final String SCREEN_MY_BOOKINGS = "MY_BOOKINGS";
    public static final String SCREEN_HOLIDAY_RUSH = "HOLIDAY_RUSH";
    public static final String SCREEN_SAVINGS = "SAVINGS";
    public static final String SCREEN_PROFILE = "PROFILE";

    private Student currentStudent = null;

    private JPanel rootPanel;
    private JPanel sidebarPanel;
    private JPanel headerPanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private JLabel lblUserBadge;
    private JLabel lblDeptBadge;

    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private DashboardPanel dashboardPanel;
    private CreateRidePanel createRidePanel;
    private FindRidePanel findRidePanel;
    private MyRidesPanel myRidesPanel;
    private MyBookingsPanel myBookingsPanel;
    private HolidayRushPanel holidayRushPanel;
    private SavingsPanel savingsPanel;
    private ProfilePanel profilePanel;

    private final Map<String, JButton> navButtons = new HashMap<>();
    private String currentScreen = SCREEN_LOGIN;

    public MainFrame() {
        setTitle("CampusRide · Smart Student Ride-Pooling & Fare-Sharing System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 800);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

        initUI();
        showCard(SCREEN_LOGIN);
    }

    private void initUI() {
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UITheme.BG_DARKEST);
        setContentPane(rootPanel);

        // 1. SIDEBAR NAVIGATION
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(UITheme.BG_SIDEBAR);
        sidebarPanel.setPreferredSize(new Dimension(240, 0));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UITheme.BORDER_COLOR));

        // Brand Banner in Sidebar
        JPanel brandBox = new JPanel(new GridLayout(2, 1, 0, 2));
        brandBox.setOpaque(false);
        brandBox.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel lblBrand = new JLabel("CAMPUSRIDE");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBrand.setForeground(UITheme.PRIMARY);

        JLabel lblSub = new JLabel("Ride-Pool & Save");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        brandBox.add(lblBrand);
        brandBox.add(lblSub);
        sidebarPanel.add(brandBox);

        sidebarPanel.add(Box.createVerticalStrut(10));

        // Navigation Items
        addNavButton("📊 Dashboard", SCREEN_DASHBOARD);
        addNavButton("➕ Create a Ride", SCREEN_CREATE_RIDE);
        addNavButton("🔍 Find a Ride", SCREEN_FIND_RIDE);
        addNavButton("🚗 My Rides", SCREEN_MY_RIDES);
        addNavButton("🎫 My Bookings", SCREEN_MY_BOOKINGS);
        addNavButton("⚡ Holiday Rush", SCREEN_HOLIDAY_RUSH);
        addNavButton("💰 Savings Dashboard", SCREEN_SAVINGS);
        addNavButton("👤 Profile", SCREEN_PROFILE);

        sidebarPanel.add(Box.createVerticalGlue());

        // Logout button at bottom of sidebar
        JButton btnLogout = createNavButton("🚪 Log Out", e -> logout());
        btnLogout.setForeground(UITheme.STATUS_CANCELLED_FG);
        sidebarPanel.add(btnLogout);
        sidebarPanel.add(Box.createVerticalStrut(16));

        rootPanel.add(sidebarPanel, BorderLayout.WEST);

        // 2. MAIN WORKSPACE (Header + CardLayout)
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setOpaque(false);

        // TOP HEADER BAR
        headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setBackground(UITheme.BG_DARK);
        headerPanel.setPreferredSize(new Dimension(0, 52));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(0, 24, 0, 24)
        ));

        JLabel lblTagline = new JLabel("Share the Ride. Split the Fare. Save More.");
        lblTagline.setFont(UITheme.FONT_SMALL);
        lblTagline.setForeground(UITheme.TEXT_MUTED);
        headerPanel.add(lblTagline, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        userPanel.setOpaque(false);

        lblUserBadge = new JLabel("Student");
        lblUserBadge.setFont(UITheme.FONT_BODY_BOLD);
        lblUserBadge.setForeground(UITheme.TEXT_PRIMARY);

        lblDeptBadge = new JLabel("Dept");
        lblDeptBadge.setFont(UITheme.FONT_SMALL);
        lblDeptBadge.setForeground(UITheme.ACCENT_CYAN);

        userPanel.add(lblUserBadge);
        userPanel.add(lblDeptBadge);
        headerPanel.add(userPanel, BorderLayout.EAST);

        workspace.add(headerPanel, BorderLayout.NORTH);

        // 3. CARD CONTAINER
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        // Instantiate Panels
        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);
        dashboardPanel = new DashboardPanel(this);
        createRidePanel = new CreateRidePanel(this);
        findRidePanel = new FindRidePanel(this);
        myRidesPanel = new MyRidesPanel(this);
        myBookingsPanel = new MyBookingsPanel(this);
        holidayRushPanel = new HolidayRushPanel(this);
        savingsPanel = new SavingsPanel(this);
        profilePanel = new ProfilePanel(this);

        cardPanel.add(loginPanel, SCREEN_LOGIN);
        cardPanel.add(registerPanel, SCREEN_REGISTER);
        cardPanel.add(dashboardPanel, SCREEN_DASHBOARD);
        cardPanel.add(createRidePanel, SCREEN_CREATE_RIDE);
        cardPanel.add(findRidePanel, SCREEN_FIND_RIDE);
        cardPanel.add(myRidesPanel, SCREEN_MY_RIDES);
        cardPanel.add(myBookingsPanel, SCREEN_MY_BOOKINGS);
        cardPanel.add(holidayRushPanel, SCREEN_HOLIDAY_RUSH);
        cardPanel.add(savingsPanel, SCREEN_SAVINGS);
        cardPanel.add(profilePanel, SCREEN_PROFILE);

        workspace.add(cardPanel, BorderLayout.CENTER);
        rootPanel.add(workspace, BorderLayout.CENTER);
    }

    private void addNavButton(String text, String screenKey) {
        JButton btn = createNavButton(text, e -> showCard(screenKey));
        navButtons.put(screenKey, btn);
        sidebarPanel.add(btn);
        sidebarPanel.add(Box.createVerticalStrut(4));
    }

    private JButton createNavButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BODY);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BG_SIDEBAR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(240, 42));
        btn.setPreferredSize(new Dimension(240, 42));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getForeground().equals(UITheme.PRIMARY)) {
                    btn.setBackground(UITheme.BG_CARD);
                    btn.setForeground(UITheme.TEXT_PRIMARY);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!btn.getForeground().equals(UITheme.PRIMARY)) {
                    btn.setBackground(UITheme.BG_SIDEBAR);
                    btn.setForeground(UITheme.TEXT_SECONDARY);
                }
            }
        });

        return btn;
    }

    public void showCard(String screenKey) {
        this.currentScreen = screenKey;
        cardLayout.show(cardPanel, screenKey);

        // Hide sidebar and header on login and register screens
        boolean isAuthScreen = SCREEN_LOGIN.equals(screenKey) || SCREEN_REGISTER.equals(screenKey);
        sidebarPanel.setVisible(!isAuthScreen);
        headerPanel.setVisible(!isAuthScreen);

        // Update nav button active states
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(screenKey)) {
                btn.setForeground(UITheme.PRIMARY);
                btn.setBackground(UITheme.BG_CARD);
                btn.setFont(UITheme.FONT_BODY_BOLD);
            } else {
                btn.setForeground(UITheme.TEXT_SECONDARY);
                btn.setBackground(UITheme.BG_SIDEBAR);
                btn.setFont(UITheme.FONT_BODY);
            }
        }

        // Trigger page data refreshes when navigated to
        if (currentStudent != null) {
            switch (screenKey) {
                case SCREEN_DASHBOARD -> dashboardPanel.refreshData();
                case SCREEN_CREATE_RIDE -> createRidePanel.loadDestinations();
                case SCREEN_FIND_RIDE -> findRidePanel.loadDestinations();
                case SCREEN_MY_RIDES -> myRidesPanel.refreshData();
                case SCREEN_MY_BOOKINGS -> myBookingsPanel.refreshData();
                case SCREEN_HOLIDAY_RUSH -> holidayRushPanel.refreshData();
                case SCREEN_SAVINGS -> savingsPanel.refreshData();
                case SCREEN_PROFILE -> profilePanel.refreshData();
            }
        }

        revalidate();
        repaint();
    }

    public void onLoginSuccess(Student student) {
        this.currentStudent = student;
        AuthService.setCurrentStudent(student);

        lblUserBadge.setText(student.getFullName());
        lblDeptBadge.setText(student.getDepartment() + " · Year " + student.getYearOfStudy());

        showCard(SCREEN_DASHBOARD);
    }

    public void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to log out of CampusRide?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            this.currentStudent = null;
            AuthService.logout();
            loginPanel.clearFields();
            showCard(SCREEN_LOGIN);
        }
    }

    public Student getCurrentStudent() { return currentStudent; }
    public DashboardPanel getDashboardPanel() { return dashboardPanel; }
    public FindRidePanel getFindRidePanel() { return findRidePanel; }
}
