package com.campusride.ui;

import com.campusride.dao.DestinationDAO;
import com.campusride.model.Destination;
import com.campusride.model.Ride;
import com.campusride.model.RideMatch;
import com.campusride.model.Student;
import com.campusride.service.FareService;
import com.campusride.service.RideService;
import com.campusride.ui.components.ModernButton;
import com.campusride.util.DateUtil;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for discovering available rides using smart 30-minute time-window matching.
 */
public class FindRidePanel extends JPanel {
    private final MainFrame mainFrame;
    private final DestinationDAO destinationDAO;
    private final RideService rideService;

    private JComboBox<Destination> cbOrigin;
    private JComboBox<Destination> cbDestination;
    private JSpinner spinDate;
    private JComboBox<String> cbHour;
    private JComboBox<String> cbMinute;
    private JComboBox<String> cbAmPm;
    private JComboBox<String> cbVehicleFilter;
    private ModernButton btnSearch;
    private JLabel lblStatus;

    private JTable tblResults;
    private DefaultTableModel modelResults;
    private List<RideMatch> currentMatches = new ArrayList<>();

    private JPanel resultsCardPanel;
    private CardLayout resultsCardLayout;
    private JLabel lblEmptyState;

    public FindRidePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.destinationDAO = new DestinationDAO();
        this.rideService = new RideService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Find a Ride");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Smart matching pairs you with students travelling the same route within ±30 minutes.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // TOP SEARCH FILTER BAR
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(UITheme.BG_CARD);
        filterPanel.setBorder(UITheme.createCardBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 1 Labels
        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(createLabel("Origin"), gbc);
        gbc.gridx = 1;
        filterPanel.add(createLabel("Destination"), gbc);
        gbc.gridx = 2;
        filterPanel.add(createLabel("Travel Date"), gbc);
        gbc.gridx = 3;
        filterPanel.add(createLabel("Target Time (±30m Window)"), gbc);
        gbc.gridx = 4;
        filterPanel.add(createLabel("Vehicle Type"), gbc);

        // Row 2 Inputs
        cbOrigin = new JComboBox<>();
        UITheme.styleComboBox(cbOrigin);
        gbc.gridx = 0; gbc.gridy = 1;
        filterPanel.add(cbOrigin, gbc);

        cbDestination = new JComboBox<>();
        UITheme.styleComboBox(cbDestination);
        gbc.gridx = 1; gbc.gridy = 1;
        filterPanel.add(cbDestination, gbc);

        SpinnerDateModel dateModel = new SpinnerDateModel();
        spinDate = new JSpinner(dateModel);
        spinDate.setEditor(new JSpinner.DateEditor(spinDate, "yyyy-MM-dd"));
        spinDate.setFont(UITheme.FONT_BODY);
        gbc.gridx = 2; gbc.gridy = 1;
        filterPanel.add(spinDate, gbc);

        // Time panel
        JPanel timePanel = new JPanel(new GridLayout(1, 3, 4, 0));
        timePanel.setOpaque(false);
        String[] hours = new String[12];
        for (int i = 1; i <= 12; i++) hours[i - 1] = String.format("%02d", i);
        cbHour = new JComboBox<>(hours);
        UITheme.styleComboBox(cbHour);

        String[] mins = {"00", "15", "30", "45"};
        cbMinute = new JComboBox<>(mins);
        UITheme.styleComboBox(cbMinute);

        cbAmPm = new JComboBox<>(new String[]{"AM", "PM"});
        cbAmPm.setSelectedItem("PM");
        UITheme.styleComboBox(cbAmPm);

        timePanel.add(cbHour);
        timePanel.add(cbMinute);
        timePanel.add(cbAmPm);
        gbc.gridx = 3; gbc.gridy = 1;
        filterPanel.add(timePanel, gbc);

        cbVehicleFilter = new JComboBox<>(new String[]{"All Vehicles", "Cab", "Auto", "Bike"});
        UITheme.styleComboBox(cbVehicleFilter);
        gbc.gridx = 4; gbc.gridy = 1;
        filterPanel.add(cbVehicleFilter, gbc);

        // Row 3 Search Button & Status
        btnSearch = new ModernButton("🔍 Search Matching Rides", ModernButton.Variant.PRIMARY);
        btnSearch.addActionListener(e -> executeSearch());
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 8, 4, 8);
        filterPanel.add(btnSearch, gbc);

        lblStatus = new JLabel("Enter your travel route and target time to find active matches.");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 3;
        filterPanel.add(lblStatus, gbc);

        add(filterPanel, BorderLayout.CENTER);

        // RESULTS SECTION (South in layout)
        JPanel resultsOuter = new JPanel(new BorderLayout(8, 8));
        resultsOuter.setOpaque(false);
        resultsOuter.setPreferredSize(new Dimension(800, 360));

        JLabel lblResultsHeader = new JLabel("Available Rides Matching Your Route");
        lblResultsHeader.setFont(UITheme.FONT_HEADER);
        lblResultsHeader.setForeground(UITheme.TEXT_PRIMARY);
        resultsOuter.add(lblResultsHeader, BorderLayout.NORTH);

        String[] cols = {"Ride ID", "Driver / Creator", "Route", "Departure Time", "Vehicle", "Total Fare", "Your Share", "Seats", "Smart Match Reason"};
        modelResults = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblResults = new JTable(modelResults);
        UITheme.styleTable(tblResults);

        // Action on double click or button
        tblResults.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && tblResults.getSelectedRow() >= 0) {
                    viewSelectedRide();
                }
            }
        });

        JScrollPane scrollTable = new JScrollPane(tblResults);
        scrollTable.getViewport().setBackground(UITheme.BG_CARD);
        scrollTable.setBorder(UITheme.createCardBorder());

        lblEmptyState = new JLabel("No matching rides found within ±30 minutes. Try adjusting your departure time or create a new ride!", SwingConstants.CENTER);
        lblEmptyState.setFont(UITheme.FONT_BODY);
        lblEmptyState.setForeground(UITheme.TEXT_MUTED);

        resultsCardLayout = new CardLayout();
        resultsCardPanel = new JPanel(resultsCardLayout);
        resultsCardPanel.setOpaque(false);
        resultsCardPanel.add(scrollTable, "TABLE");
        resultsCardPanel.add(lblEmptyState, "EMPTY");
        resultsOuter.add(resultsCardPanel, BorderLayout.CENTER);

        // Action Buttons Row
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        ModernButton btnViewDetails = new ModernButton("View Details & Join Ride →", ModernButton.Variant.SUCCESS);
        btnViewDetails.addActionListener(e -> viewSelectedRide());
        bottomBar.add(btnViewDetails);
        resultsOuter.add(bottomBar, BorderLayout.SOUTH);

        // Wrap filter + results in container
        JPanel centerContainer = new JPanel(new BorderLayout(0, 16));
        centerContainer.setOpaque(false);
        centerContainer.add(filterPanel, BorderLayout.NORTH);
        centerContainer.add(resultsOuter, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

        loadDestinations();
        resultsCardLayout.show(resultsCardPanel, "EMPTY");
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    public void loadDestinations() {
        cbOrigin.removeAllItems();
        cbDestination.removeAllItems();
        try {
            List<Destination> list = destinationDAO.getAllActive();
            for (Destination d : list) {
                cbOrigin.addItem(d);
                cbDestination.addItem(d);
            }
            if (cbDestination.getItemCount() > 1) {
                cbDestination.setSelectedIndex(1);
            }
        } catch (Exception ignored) {}
    }

    public void setPreselectedRoute(int originId, int destId) {
        loadDestinations();
        for (int i = 0; i < cbOrigin.getItemCount(); i++) {
            if (cbOrigin.getItemAt(i).getDestinationId() == originId) {
                cbOrigin.setSelectedIndex(i);
                break;
            }
        }
        for (int i = 0; i < cbDestination.getItemCount(); i++) {
            if (cbDestination.getItemAt(i).getDestinationId() == destId) {
                cbDestination.setSelectedIndex(i);
                break;
            }
        }
        executeSearch();
    }

    private void executeSearch() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) {
            lblStatus.setText("Please log in first.");
            return;
        }

        Destination orig = (Destination) cbOrigin.getSelectedItem();
        Destination dest = (Destination) cbDestination.getSelectedItem();
        if (orig == null || dest == null) {
            lblStatus.setText("Please select origin and destination.");
            return;
        }
        if (orig.getDestinationId() == dest.getDestinationId()) {
            lblStatus.setText("Origin and destination must be different.");
            return;
        }

        java.util.Date utilDate = (java.util.Date) spinDate.getValue();
        LocalDate localDate = utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        int hour = Integer.parseInt((String) cbHour.getSelectedItem());
        int minute = Integer.parseInt((String) cbMinute.getSelectedItem());
        String amPm = (String) cbAmPm.getSelectedItem();
        if ("PM".equals(amPm) && hour < 12) hour += 12;
        if ("AM".equals(amPm) && hour == 12) hour = 0;

        LocalDateTime requestedTime = LocalDateTime.of(localDate, LocalTime.of(hour, minute));

        btnSearch.setEnabled(false);
        btnSearch.setText("Searching...");
        lblStatus.setText("Finding rides departing within ±30 minutes of " + DateUtil.formatTimeOnly(requestedTime) + "...");

        String vehicleFilter = (String) cbVehicleFilter.getSelectedItem();

        SwingWorker<List<RideMatch>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RideMatch> doInBackground() throws Exception {
                List<RideMatch> allMatches = rideService.searchRides(
                    current, orig.getDestinationId(), dest.getDestinationId(), requestedTime
                );

                if (vehicleFilter != null && !vehicleFilter.startsWith("All")) {
                    return allMatches.stream()
                        .filter(m -> m.getRide().getVehicleType().equalsIgnoreCase(vehicleFilter))
                        .toList();
                }
                return allMatches;
            }

            @Override
            protected void done() {
                btnSearch.setEnabled(true);
                btnSearch.setText("🔍 Search Matching Rides");
                try {
                    currentMatches = get();
                    modelResults.setRowCount(0);

                    if (currentMatches.isEmpty()) {
                        resultsCardLayout.show(resultsCardPanel, "EMPTY");
                        lblStatus.setText("No rides found for this route within ±30 minutes. Consider publishing one!");
                    } else {
                        resultsCardLayout.show(resultsCardPanel, "TABLE");
                        lblStatus.setText("Found " + currentMatches.size() + " matching ride(s) within the 30-minute time window.");
                        for (RideMatch m : currentMatches) {
                            Ride r = m.getRide();
                            modelResults.addRow(new Object[]{
                                "#" + r.getRideId(),
                                r.getDriverName(),
                                r.getOriginName() + " → " + r.getDestinationName(),
                                DateUtil.formatDateTime(r.getDepartureTime()),
                                r.getVehicleType(),
                                FareService.formatINR(r.getTotalFare()),
                                FareService.formatINR(m.getEstimatedShare()),
                                r.getAvailableSeats() + " left",
                                m.getMatchReason()
                            });
                        }
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Search error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void viewSelectedRide() {
        int row = tblResults.getSelectedRow();
        if (row < 0 || row >= currentMatches.size()) {
            JOptionPane.showMessageDialog(this, "Please select a ride from the list first.", "No Ride Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RideMatch selectedMatch = currentMatches.get(row);
        RideDetailsDialog dialog = new RideDetailsDialog(mainFrame, selectedMatch.getRide(), () -> {
            executeSearch();
            mainFrame.getDashboardPanel().refreshData();
        });
        dialog.setVisible(true);
    }
}
