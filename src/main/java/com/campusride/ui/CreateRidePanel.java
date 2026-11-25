package com.campusride.ui;

import com.campusride.dao.DestinationDAO;
import com.campusride.model.Destination;
import com.campusride.model.Ride;
import com.campusride.model.Student;
import com.campusride.service.FareService;
import com.campusride.service.RideService;
import com.campusride.ui.components.ModernButton;
import com.campusride.util.UITheme;
import com.campusride.util.ValidationException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Screen for creating and publishing new student rides.
 */
public class CreateRidePanel extends JPanel {
    private final MainFrame mainFrame;
    private final DestinationDAO destinationDAO;
    private final RideService rideService;
    private final FareService fareService;

    private JComboBox<Destination> cbOrigin;
    private JComboBox<Destination> cbDestination;
    private JSpinner spinDate;
    private JComboBox<String> cbHour;
    private JComboBox<String> cbMinute;
    private JComboBox<String> cbAmPm;
    private JComboBox<String> cbVehicleType;
    private JSpinner spinCapacity;
    private JTextField txtTotalFare;
    private JLabel lblSuggestedFare;
    private JCheckBox chkWomenOnly;
    private JCheckBox chkSameDept;
    private JTextField txtNotes;
    private JLabel lblError;
    private ModernButton btnPublish;

    public CreateRidePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.destinationDAO = new DestinationDAO();
        this.rideService = new RideService();
        this.fareService = new FareService(destinationDAO);

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Create a Ride");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Publish your upcoming trip, split fuel/taxi fare, and commute together.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // FORM CARD (Centered in scrollpane)
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(UITheme.createCardBorder());
        card.setMaximumSize(new Dimension(800, 700));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Error message label
        lblError = new JLabel("", SwingConstants.CENTER);
        lblError.setFont(UITheme.FONT_BODY_BOLD);
        lblError.setForeground(UITheme.STATUS_CANCELLED_FG);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        card.add(lblError, gbc);
        gbc.gridwidth = 1;

        // ROW 1: Origin & Destination
        gbc.gridx = 0; gbc.gridy = row;
        card.add(createFieldLabel("Origin (Campus Hotspot)"), gbc);
        gbc.gridx = 1;
        card.add(createFieldLabel("Destination (Drop-off Point)"), gbc);
        row++;

        cbOrigin = new JComboBox<>();
        UITheme.styleComboBox(cbOrigin);
        cbOrigin.addActionListener(e -> updateSuggestedFare());
        gbc.gridx = 0; gbc.gridy = row;
        card.add(cbOrigin, gbc);

        cbDestination = new JComboBox<>();
        UITheme.styleComboBox(cbDestination);
        cbDestination.addActionListener(e -> updateSuggestedFare());
        gbc.gridx = 1;
        card.add(cbDestination, gbc);
        row++;

        // ROW 2: Departure Date & Departure Time
        gbc.gridx = 0; gbc.gridy = row;
        card.add(createFieldLabel("Departure Date"), gbc);
        gbc.gridx = 1;
        card.add(createFieldLabel("Departure Time"), gbc);
        row++;

        // Date Spinner (default today/tomorrow)
        SpinnerDateModel dateModel = new SpinnerDateModel();
        spinDate = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spinDate, "yyyy-MM-dd");
        spinDate.setEditor(dateEditor);
        spinDate.setFont(UITheme.FONT_BODY);
        gbc.gridx = 0; gbc.gridy = row;
        card.add(spinDate, gbc);

        // Time components
        JPanel timePanel = new JPanel(new GridLayout(1, 3, 6, 0));
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
        gbc.gridx = 1;
        card.add(timePanel, gbc);
        row++;

        // ROW 3: Vehicle Type & Passenger Capacity
        gbc.gridx = 0; gbc.gridy = row;
        card.add(createFieldLabel("Vehicle Type"), gbc);
        gbc.gridx = 1;
        card.add(createFieldLabel("Passenger Capacity (Seats Available to Share)"), gbc);
        row++;

        cbVehicleType = new JComboBox<>(new String[]{"Cab (4-Wheeler Taxi)", "Auto (Rickshaw)", "Bike (2-Wheeler)"});
        UITheme.styleComboBox(cbVehicleType);
        cbVehicleType.addActionListener(e -> onVehicleTypeChanged());
        gbc.gridx = 0; gbc.gridy = row;
        card.add(cbVehicleType, gbc);

        spinCapacity = new JSpinner(new SpinnerNumberModel(4, 1, 4, 1));
        spinCapacity.setFont(UITheme.FONT_BODY);
        gbc.gridx = 1;
        card.add(spinCapacity, gbc);
        row++;

        // ROW 4: Total Estimated Fare
        gbc.gridx = 0; gbc.gridy = row;
        card.add(createFieldLabel("Total Estimated Fare (₹ INR)"), gbc);
        gbc.gridx = 1;
        card.add(createFieldLabel("Suggested Fare Breakdown"), gbc);
        row++;

        txtTotalFare = new JTextField("400.00");
        UITheme.styleTextField(txtTotalFare);
        gbc.gridx = 0; gbc.gridy = row;
        card.add(txtTotalFare, gbc);

        lblSuggestedFare = new JLabel("Calculated route distance: ~14.5 km · Suggested: ₹419.00");
        lblSuggestedFare.setFont(UITheme.FONT_SMALL);
        lblSuggestedFare.setForeground(UITheme.ACCENT_CYAN);
        gbc.gridx = 1;
        card.add(lblSuggestedFare, gbc);
        row++;

        // ROW 5: Ride Preferences
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel prefPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        prefPanel.setOpaque(false);

        chkWomenOnly = new JCheckBox("Women-Only Ride (Only female students may join)");
        chkWomenOnly.setFont(UITheme.FONT_BODY);
        chkWomenOnly.setForeground(UITheme.TEXT_PRIMARY);
        chkWomenOnly.setOpaque(false);

        chkSameDept = new JCheckBox("Same-Department Only (Students in your department only)");
        chkSameDept.setFont(UITheme.FONT_BODY);
        chkSameDept.setForeground(UITheme.TEXT_PRIMARY);
        chkSameDept.setOpaque(false);

        prefPanel.add(chkWomenOnly);
        prefPanel.add(chkSameDept);
        card.add(prefPanel, gbc);
        row++;

        // ROW 6: Optional Notes
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        card.add(createFieldLabel("Ride Notes (e.g., Pickup at Main Gate, luggage space)"), gbc);

        txtNotes = new JTextField();
        UITheme.styleTextField(txtNotes);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        card.add(txtNotes, gbc);

        // ROW 7: Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        actionPanel.setOpaque(false);

        ModernButton btnReset = new ModernButton("Reset Form", ModernButton.Variant.SECONDARY);
        btnReset.addActionListener(e -> resetForm());

        btnPublish = new ModernButton("Publish Ride Listing", ModernButton.Variant.PRIMARY);
        btnPublish.addActionListener(e -> publishRide());

        actionPanel.add(btnReset);
        actionPanel.add(btnPublish);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        card.add(actionPanel, gbc);

        JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        outer.setOpaque(false);
        outer.add(card);

        JScrollPane scroll = new JScrollPane(outer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        loadDestinations();
    }

    private JLabel createFieldLabel(String text) {
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
            updateSuggestedFare();
        } catch (Exception e) {
            lblError.setText("Could not load destinations: " + e.getMessage());
        }
    }

    private void onVehicleTypeChanged() {
        String selected = (String) cbVehicleType.getSelectedItem();
        if (selected == null) return;

        if (selected.startsWith("Cab")) {
            spinCapacity.setModel(new SpinnerNumberModel(4, 1, 4, 1));
        } else if (selected.startsWith("Auto")) {
            spinCapacity.setModel(new SpinnerNumberModel(3, 1, 3, 1));
        } else { // Bike
            spinCapacity.setModel(new SpinnerNumberModel(1, 1, 1, 1));
        }
        updateSuggestedFare();
    }

    private void updateSuggestedFare() {
        Destination orig = (Destination) cbOrigin.getSelectedItem();
        Destination dest = (Destination) cbDestination.getSelectedItem();
        if (orig == null || dest == null || orig.getDestinationId() == dest.getDestinationId()) {
            lblSuggestedFare.setText("Select two different locations to see suggested fare.");
            return;
        }

        String vType = getSelectedVehicleTypeCode();
        double distance = destinationDAO.getDistanceKm(orig.getDestinationId(), dest.getDestinationId());
        BigDecimal suggested = fareService.calculateSuggestedFare(vType, orig.getDestinationId(), dest.getDestinationId());

        txtTotalFare.setText(suggested.toPlainString());
        lblSuggestedFare.setText(String.format("Route distance: %.1f km · Suggested fare: %s",
                distance, FareService.formatINR(suggested)));
    }

    private String getSelectedVehicleTypeCode() {
        String selected = (String) cbVehicleType.getSelectedItem();
        if (selected != null && selected.startsWith("Auto")) return "AUTO";
        if (selected != null && selected.startsWith("Bike")) return "BIKE";
        return "CAB";
    }

    private void publishRide() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) {
            lblError.setText("Please log in first.");
            return;
        }

        Destination orig = (Destination) cbOrigin.getSelectedItem();
        Destination dest = (Destination) cbDestination.getSelectedItem();

        if (orig == null || dest == null) {
            lblError.setText("Please select origin and destination.");
            return;
        }
        if (orig.getDestinationId() == dest.getDestinationId()) {
            lblError.setText("Origin and destination must be different.");
            return;
        }

        // Construct Departure LocalDateTime
        java.util.Date utilDate = (java.util.Date) spinDate.getValue();
        LocalDate localDate = utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        int hour = Integer.parseInt((String) cbHour.getSelectedItem());
        int minute = Integer.parseInt((String) cbMinute.getSelectedItem());
        String amPm = (String) cbAmPm.getSelectedItem();
        if ("PM".equals(amPm) && hour < 12) hour += 12;
        if ("AM".equals(amPm) && hour == 12) hour = 0;

        LocalDateTime departure = LocalDateTime.of(localDate, LocalTime.of(hour, minute));
        if (departure.isBefore(LocalDateTime.now())) {
            lblError.setText("Departure date and time must be in the future.");
            return;
        }

        BigDecimal fare;
        try {
            fare = new BigDecimal(txtTotalFare.getText().trim());
            if (fare.compareTo(BigDecimal.ZERO) <= 0) {
                lblError.setText("Total estimated fare must be greater than ₹0.");
                return;
            }
        } catch (Exception ex) {
            lblError.setText("Invalid fare format. Please enter a valid amount.");
            return;
        }

        int capacity = (Integer) spinCapacity.getValue();
        String vehicleType = getSelectedVehicleTypeCode();
        boolean womenOnly = chkWomenOnly.isSelected();
        boolean sameDept = chkSameDept.isSelected();
        String notes = txtNotes.getText().trim();

        lblError.setText("");
        btnPublish.setEnabled(false);
        btnPublish.setText("Publishing...");

        SwingWorker<Ride, Void> worker = new SwingWorker<>() {
            @Override
            protected Ride doInBackground() throws Exception {
                return rideService.publishRide(
                    current, orig.getDestinationId(), dest.getDestinationId(),
                    departure, vehicleType, fare, capacity, womenOnly, sameDept, notes
                );
            }

            @Override
            protected void done() {
                btnPublish.setEnabled(true);
                btnPublish.setText("Publish Ride Listing");
                try {
                    Ride published = get();
                    JOptionPane.showMessageDialog(
                        CreateRidePanel.this,
                        "Ride #" + published.getRideId() + " successfully published!\n" +
                        "Route: " + orig.getDestinationName() + " → " + dest.getDestinationName() + "\n" +
                        "Departure: " + departure.toString().replace('T', ' ') + "\n" +
                        "Seats available: " + capacity,
                        "Ride Published",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    resetForm();
                    mainFrame.showCard(MainFrame.SCREEN_MY_RIDES);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ValidationException) {
                        lblError.setText(cause.getMessage());
                    } else {
                        lblError.setText("Failed to publish ride: " + cause.getMessage());
                    }
                }
            }
        };
        worker.execute();
    }

    private void resetForm() {
        if (cbOrigin.getItemCount() > 0) cbOrigin.setSelectedIndex(0);
        if (cbDestination.getItemCount() > 1) cbDestination.setSelectedIndex(1);
        cbVehicleType.setSelectedIndex(0);
        chkWomenOnly.setSelected(false);
        chkSameDept.setSelected(false);
        txtNotes.setText("");
        lblError.setText("");
        updateSuggestedFare();
    }
}
