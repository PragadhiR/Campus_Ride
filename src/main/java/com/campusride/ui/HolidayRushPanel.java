package com.campusride.ui;

import com.campusride.dao.ReportDAO;
import com.campusride.service.FareService;
import com.campusride.service.ReportService;
import com.campusride.ui.components.ModernButton;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Screen for Holiday Rush analytics:
 * Real DBMS aggregate insights using GROUP BY, COUNT(), and ORDER BY to identify
 * peak travel hotspots, busiest departure times, and vehicle distributions.
 */
public class HolidayRushPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ReportService reportService;

    private JTable tblPopular;
    private DefaultTableModel modelPopular;
    private List<ReportDAO.PopularDestinationStat> currentDestStats = List.of();

    private JTable tblTimeSlots;
    private DefaultTableModel modelTimeSlots;

    private JTable tblVehicles;
    private DefaultTableModel modelVehicles;

    public HolidayRushPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.reportService = new ReportService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // TOP HEADER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Holiday Rush Hub ⚡");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Data-driven destination insights & peak departure windows around busy campus travel breaks.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // MAIN CONTENT (Split: Popular Destinations on left, Analytics on right)
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setOpaque(false);

        // LEFT: Most Popular Destinations
        JPanel pnlPopular = new JPanel(new BorderLayout(8, 12));
        pnlPopular.setBackground(UITheme.BG_CARD);
        pnlPopular.setBorder(UITheme.createCardBorder());

        JLabel lblPopTitle = new JLabel("Most Popular Travel Hotspots");
        lblPopTitle.setFont(UITheme.FONT_HEADER);
        lblPopTitle.setForeground(UITheme.ACCENT_CYAN);
        pnlPopular.add(lblPopTitle, BorderLayout.NORTH);

        String[] colsPop = {"Destination", "Category", "Rides", "Available Seats", "Completed"};
        modelPopular = new DefaultTableModel(colsPop, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblPopular = new JTable(modelPopular);
        UITheme.styleTable(tblPopular);
        JScrollPane spPopular = new JScrollPane(tblPopular);
        spPopular.getViewport().setBackground(UITheme.BG_CARD);
        spPopular.setBorder(BorderFactory.createEmptyBorder());
        pnlPopular.add(spPopular, BorderLayout.CENTER);

        // Action button to find rides for selected destination
        JPanel popAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        popAction.setOpaque(false);
        ModernButton btnGoFind = new ModernButton("Find Rides to Selected Hotspot →", ModernButton.Variant.SUCCESS);
        btnGoFind.addActionListener(e -> navigateToFindRide());
        popAction.add(btnGoFind);
        pnlPopular.add(popAction, BorderLayout.SOUTH);

        // RIGHT: Split vertically into Busiest Time Slots and Vehicle Breakdown
        JPanel pnlRight = new JPanel(new GridLayout(2, 1, 0, 16));
        pnlRight.setOpaque(false);

        // Right Top: Busiest Time Slots
        JPanel pnlSlots = new JPanel(new BorderLayout(8, 8));
        pnlSlots.setBackground(UITheme.BG_CARD);
        pnlSlots.setBorder(UITheme.createCardBorder());

        JLabel lblSlotTitle = new JLabel("Busiest Departure Windows (Peak Rush Hours)");
        lblSlotTitle.setFont(UITheme.FONT_SUBHEADER);
        lblSlotTitle.setForeground(UITheme.PRIMARY);
        pnlSlots.add(lblSlotTitle, BorderLayout.NORTH);

        String[] colsSlots = {"Departure Window", "Active & Completed Rides"};
        modelTimeSlots = new DefaultTableModel(colsSlots, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblTimeSlots = new JTable(modelTimeSlots);
        UITheme.styleTable(tblTimeSlots);
        JScrollPane spSlots = new JScrollPane(tblTimeSlots);
        spSlots.getViewport().setBackground(UITheme.BG_CARD);
        spSlots.setBorder(BorderFactory.createEmptyBorder());
        pnlSlots.add(spSlots, BorderLayout.CENTER);

        // Right Bottom: Vehicle Distribution
        JPanel pnlVehicles = new JPanel(new BorderLayout(8, 8));
        pnlVehicles.setBackground(UITheme.BG_CARD);
        pnlVehicles.setBorder(UITheme.createCardBorder());

        JLabel lblVehTitle = new JLabel("Vehicle Fleet Distribution & Fares");
        lblVehTitle.setFont(UITheme.FONT_SUBHEADER);
        lblVehTitle.setForeground(UITheme.ACCENT_TEAL);
        pnlVehicles.add(lblVehTitle, BorderLayout.NORTH);

        String[] colsVeh = {"Vehicle Type", "Total Rides", "Average Trip Fare"};
        modelVehicles = new DefaultTableModel(colsVeh, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblVehicles = new JTable(modelVehicles);
        UITheme.styleTable(tblVehicles);
        JScrollPane spVeh = new JScrollPane(tblVehicles);
        spVeh.getViewport().setBackground(UITheme.BG_CARD);
        spVeh.setBorder(BorderFactory.createEmptyBorder());
        pnlVehicles.add(spVeh, BorderLayout.CENTER);

        pnlRight.add(pnlSlots);
        pnlRight.add(pnlVehicles);

        centerPanel.add(pnlPopular);
        centerPanel.add(pnlRight);
        add(centerPanel, BorderLayout.CENTER);

        // BOTTOM REFRESH
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);
        ModernButton btnRefresh = new ModernButton("⟳ Refresh Analytics", ModernButton.Variant.SECONDARY);
        btnRefresh.addActionListener(e -> refreshData());
        bottomBar.add(btnRefresh);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<ReportDAO.PopularDestinationStat> destList;
            List<ReportDAO.TimeSlotStat> timeList;
            List<ReportDAO.VehicleBreakdownStat> vehList;

            @Override
            protected Void doInBackground() throws Exception {
                destList = reportService.getHolidayRushDestinations();
                timeList = reportService.getHolidayRushTimeSlots();
                vehList = reportService.getHolidayRushVehicleBreakdown();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    currentDestStats = destList;

                    // 1. Destinations
                    modelPopular.setRowCount(0);
                    for (ReportDAO.PopularDestinationStat d : destList) {
                        modelPopular.addRow(new Object[]{
                            d.destinationName(),
                            d.category(),
                            d.totalRides(),
                            d.availableSeats() + " open",
                            d.completedRides()
                        });
                    }

                    // 2. Time Slots
                    modelTimeSlots.setRowCount(0);
                    for (ReportDAO.TimeSlotStat s : timeList) {
                        modelTimeSlots.addRow(new Object[]{
                            s.displayTime(),
                            s.rideCount() + " rides"
                        });
                    }

                    // 3. Vehicles
                    modelVehicles.setRowCount(0);
                    for (ReportDAO.VehicleBreakdownStat v : vehList) {
                        modelVehicles.addRow(new Object[]{
                            v.vehicleType(),
                            v.rideCount(),
                            FareService.formatINR(v.averageFare())
                        });
                    }
                } catch (Exception ex) {
                    System.err.println("Holiday rush error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void navigateToFindRide() {
        int row = tblPopular.getSelectedRow();
        if (row < 0 || row >= currentDestStats.size()) {
            JOptionPane.showMessageDialog(this, "Please select a destination hotspot from the table.", "Select Hotspot", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ReportDAO.PopularDestinationStat selected = currentDestStats.get(row);
        mainFrame.showCard(MainFrame.SCREEN_FIND_RIDE);
        mainFrame.getFindRidePanel().setPreselectedRoute(1, selected.destinationId()); // Campus (1) -> Selected
    }
}
