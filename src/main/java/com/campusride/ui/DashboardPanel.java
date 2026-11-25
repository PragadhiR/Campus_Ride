package com.campusride.ui;

import com.campusride.model.Booking;
import com.campusride.model.Ride;
import com.campusride.model.Student;
import com.campusride.service.FareService;
import com.campusride.service.ReportService;
import com.campusride.ui.components.ModernButton;
import com.campusride.ui.components.StatCard;
import com.campusride.util.DateUtil;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Main student dashboard presenting summary metrics, savings,
 * and recent ride activities loaded from MySQL.
 */
public class DashboardPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ReportService reportService;

    private JLabel lblWelcome;
    private JLabel lblStudentInfo;

    private StatCard cardRidesCreated;
    private StatCard cardActiveBookings;
    private StatCard cardCompletedTrips;
    private StatCard cardMoneySaved;

    private JTable tblRecentCreated;
    private DefaultTableModel modelRecentCreated;

    private JTable tblRecentBookings;
    private DefaultTableModel modelRecentBookings;

    private JLabel lblEmptyCreated;
    private JLabel lblEmptyBookings;

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.reportService = new ReportService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // TOP BANNER
        JPanel topPanel = new JPanel(new BorderLayout(12, 4));
        topPanel.setOpaque(false);

        JPanel titleGrp = new JPanel(new GridLayout(2, 1, 0, 4));
        titleGrp.setOpaque(false);

        lblWelcome = new JLabel("Welcome to CampusRide");
        lblWelcome.setFont(UITheme.FONT_TITLE);
        lblWelcome.setForeground(UITheme.TEXT_PRIMARY);

        lblStudentInfo = new JLabel("Smart Student Ride-Pooling System");
        lblStudentInfo.setFont(UITheme.FONT_BODY);
        lblStudentInfo.setForeground(UITheme.TEXT_SECONDARY);

        titleGrp.add(lblWelcome);
        titleGrp.add(lblStudentInfo);
        topPanel.add(titleGrp, BorderLayout.WEST);

        // Quick Actions
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        quickActions.setOpaque(false);

        ModernButton btnNewRide = new ModernButton("+ Create Ride", ModernButton.Variant.PRIMARY);
        btnNewRide.addActionListener(e -> mainFrame.showCard(MainFrame.SCREEN_CREATE_RIDE));

        ModernButton btnFindRide = new ModernButton("🔍 Find Ride", ModernButton.Variant.OUTLINE);
        btnFindRide.addActionListener(e -> mainFrame.showCard(MainFrame.SCREEN_FIND_RIDE));

        ModernButton btnRefresh = new ModernButton("⟳ Refresh", ModernButton.Variant.SECONDARY);
        btnRefresh.addActionListener(e -> refreshData());

        quickActions.add(btnNewRide);
        quickActions.add(btnFindRide);
        quickActions.add(btnRefresh);
        topPanel.add(quickActions, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // CENTER SCROLLABLE CONTENT
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // 1. STAT CARDS ROW
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        cardRidesCreated = new StatCard("Rides Created", "0", "Published by you", UITheme.PRIMARY);
        cardActiveBookings = new StatCard("Active Bookings", "0", "Seats reserved", UITheme.ACCENT_TEAL);
        cardCompletedTrips = new StatCard("Completed Trips", "0", "Shared journeys", UITheme.STATUS_COMPLETED_FG);
        cardMoneySaved = new StatCard("Money Saved", "₹0.00", "Estimated travel savings", UITheme.STATUS_OPEN_FG);

        statsRow.add(cardRidesCreated);
        statsRow.add(cardActiveBookings);
        statsRow.add(cardCompletedTrips);
        statsRow.add(cardMoneySaved);

        centerContent.add(statsRow);
        centerContent.add(Box.createVerticalStrut(24));

        // 2. RECENT ACTIVITY SPLIT VIEW
        JPanel activityPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        activityPanel.setOpaque(false);

        // Left: Recently Created Rides
        JPanel pnlCreated = createCardContainer("Your Recently Created Rides", e -> mainFrame.showCard(MainFrame.SCREEN_MY_RIDES));
        String[] colsCreated = {"ID", "Route", "Departure", "Fare", "Seats", "Status"};
        modelRecentCreated = new DefaultTableModel(colsCreated, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRecentCreated = new JTable(modelRecentCreated);
        UITheme.styleTable(tblRecentCreated);
        JScrollPane spCreated = new JScrollPane(tblRecentCreated);
        spCreated.getViewport().setBackground(UITheme.BG_CARD);
        spCreated.setBorder(BorderFactory.createEmptyBorder());

        lblEmptyCreated = new JLabel("You haven't published any rides yet. Click '+ Create Ride' to start!", SwingConstants.CENTER);
        lblEmptyCreated.setFont(UITheme.FONT_BODY);
        lblEmptyCreated.setForeground(UITheme.TEXT_MUTED);

        JPanel pnlCreatedContent = new JPanel(new CardLayout());
        pnlCreatedContent.setOpaque(false);
        pnlCreatedContent.add(spCreated, "TABLE");
        pnlCreatedContent.add(lblEmptyCreated, "EMPTY");
        pnlCreated.add(pnlCreatedContent, BorderLayout.CENTER);

        // Right: Recent Bookings
        JPanel pnlBookings = createCardContainer("Your Recent Ride Bookings", e -> mainFrame.showCard(MainFrame.SCREEN_MY_BOOKINGS));
        String[] colsBookings = {"B-ID", "Driver", "Route", "Departure", "Share", "Status"};
        modelRecentBookings = new DefaultTableModel(colsBookings, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRecentBookings = new JTable(modelRecentBookings);
        UITheme.styleTable(tblRecentBookings);
        JScrollPane spBookings = new JScrollPane(tblRecentBookings);
        spBookings.getViewport().setBackground(UITheme.BG_CARD);
        spBookings.setBorder(BorderFactory.createEmptyBorder());

        lblEmptyBookings = new JLabel("You haven't joined any rides yet. Click 'Find Ride' to pool with others!", SwingConstants.CENTER);
        lblEmptyBookings.setFont(UITheme.FONT_BODY);
        lblEmptyBookings.setForeground(UITheme.TEXT_MUTED);

        JPanel pnlBookingsContent = new JPanel(new CardLayout());
        pnlBookingsContent.setOpaque(false);
        pnlBookingsContent.add(spBookings, "TABLE");
        pnlBookingsContent.add(lblEmptyBookings, "EMPTY");
        pnlBookings.add(pnlBookingsContent, BorderLayout.CENTER);

        activityPanel.add(pnlCreated);
        activityPanel.add(pnlBookings);

        centerContent.add(activityPanel);

        JScrollPane mainScroll = new JScrollPane(centerContent);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createCardContainer(String title, java.awt.event.ActionListener viewAllAction) {
        JPanel pnl = new JPanel(new BorderLayout(8, 12));
        pnl.setBackground(UITheme.BG_CARD);
        pnl.setBorder(UITheme.createCardBorder());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setFont(UITheme.FONT_HEADER);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        header.add(lbl, BorderLayout.WEST);

        JButton btnViewAll = new JButton("View All →");
        btnViewAll.setFont(UITheme.FONT_SMALL);
        btnViewAll.setForeground(UITheme.PRIMARY);
        btnViewAll.setBorderPainted(false);
        btnViewAll.setContentAreaFilled(false);
        btnViewAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewAll.addActionListener(viewAllAction);
        header.add(btnViewAll, BorderLayout.EAST);

        pnl.add(header, BorderLayout.NORTH);
        return pnl;
    }

    public void refreshData() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) return;

        lblWelcome.setText("Welcome back, " + current.getFullName() + "!");
        lblStudentInfo.setText(current.getDepartment() + " · Year " + current.getYearOfStudy() +
                " · " + current.getEmail());

        SwingWorker<ReportService.DashboardStats, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportService.DashboardStats doInBackground() throws Exception {
                return reportService.getDashboardStats(current);
            }

            @Override
            protected void done() {
                try {
                    ReportService.DashboardStats stats = get();
                    cardRidesCreated.setValue(String.valueOf(stats.ridesCreatedCount()));
                    cardActiveBookings.setValue(String.valueOf(stats.activeBookingsCount()));
                    cardCompletedTrips.setValue(String.valueOf(stats.completedTripsCount()));
                    cardMoneySaved.setValue(FareService.formatINR(stats.totalSavings()));

                    // Update Recent Created Table
                    modelRecentCreated.setRowCount(0);
                    List<Ride> createdList = stats.recentCreatedRides();
                    CardLayout clCreated = (CardLayout) ((JPanel) tblRecentCreated.getParent().getParent().getParent()).getLayout();
                    if (createdList.isEmpty()) {
                        clCreated.show((JPanel) tblRecentCreated.getParent().getParent().getParent(), "EMPTY");
                    } else {
                        clCreated.show((JPanel) tblRecentCreated.getParent().getParent().getParent(), "TABLE");
                        for (Ride r : createdList) {
                            modelRecentCreated.addRow(new Object[]{
                                "#" + r.getRideId(),
                                r.getOriginName() + " → " + r.getDestinationName(),
                                DateUtil.formatDateTime(r.getDepartureTime()),
                                FareService.formatINR(r.getTotalFare()),
                                r.getAvailableSeats() + "/" + r.getPassengerCapacity(),
                                r.getStatus()
                            });
                        }
                    }

                    // Update Recent Bookings Table
                    modelRecentBookings.setRowCount(0);
                    List<Booking> bookedList = stats.recentJoinedRides();
                    CardLayout clBooked = (CardLayout) ((JPanel) tblRecentBookings.getParent().getParent().getParent()).getLayout();
                    if (bookedList.isEmpty()) {
                        clBooked.show((JPanel) tblRecentBookings.getParent().getParent().getParent(), "EMPTY");
                    } else {
                        clBooked.show((JPanel) tblRecentBookings.getParent().getParent().getParent(), "TABLE");
                        for (Booking b : bookedList) {
                            modelRecentBookings.addRow(new Object[]{
                                "#" + b.getBookingId(),
                                b.getDriverName(),
                                b.getOriginName() + " → " + b.getDestinationName(),
                                DateUtil.formatDateTime(b.getDepartureTime()),
                                FareService.formatINR(b.getFareShareSnapshot()),
                                b.getBookingStatus()
                            });
                        }
                    }

                } catch (Exception ex) {
                    System.err.println("Dashboard error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
