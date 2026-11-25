package com.campusride.ui;

import com.campusride.dao.BookingDAO;
import com.campusride.dao.DestinationDAO;
import com.campusride.model.Booking;
import com.campusride.model.Ride;
import com.campusride.model.Student;
import com.campusride.service.BookingService;
import com.campusride.service.FareService;
import com.campusride.ui.components.ModernButton;
import com.campusride.ui.components.StatusBadge;
import com.campusride.util.BookingException;
import com.campusride.util.DateUtil;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detailed modal dialog for a selected Ride showing route details,
 * live confirmed passengers, dynamic fare share preview, and transactional Join button.
 */
public class RideDetailsDialog extends JDialog {
    private final MainFrame mainFrame;
    private final Ride ride;
    private final Runnable onSuccessCallback;

    private final BookingService bookingService;
    private final BookingDAO bookingDAO;
    private final FareService fareService;
    private final DestinationDAO destinationDAO;

    private ModernButton btnJoin;

    public RideDetailsDialog(MainFrame parent, Ride ride, Runnable onSuccessCallback) {
        super(parent, "Ride Details · #" + ride.getRideId(), true);
        this.mainFrame = parent;
        this.ride = ride;
        this.onSuccessCallback = onSuccessCallback;

        this.bookingService = new BookingService();
        this.bookingDAO = new BookingDAO();
        this.fareService = new FareService();
        this.destinationDAO = new DestinationDAO();

        setSize(600, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_DARKEST);

        initUI();
    }

    private void initUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG_DARKEST);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 1. TOP HEADER & STATUS
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        JLabel lblRoute = new JLabel(ride.getOriginName() + " → " + ride.getDestinationName());
        lblRoute.setFont(UITheme.FONT_TITLE);
        lblRoute.setForeground(UITheme.TEXT_PRIMARY);

        StatusBadge badge = new StatusBadge(ride.getStatus());
        topRow.add(lblRoute, BorderLayout.WEST);
        topRow.add(badge, BorderLayout.EAST);
        content.add(topRow);

        content.add(Box.createVerticalStrut(16));

        // 2. RIDE INFO CARD
        JPanel infoCard = new JPanel(new GridLayout(4, 2, 12, 10));
        infoCard.setBackground(UITheme.BG_CARD);
        infoCard.setBorder(UITheme.createCardBorder());

        infoCard.add(createPair("Creator / Driver", ride.getDriverName() + " (" + ride.getDriverDepartment() + ")"));
        infoCard.add(createPair("Departure Time", DateUtil.formatDateTime(ride.getDepartureTime())));
        infoCard.add(createPair("Vehicle Type", ride.getVehicleType()));
        infoCard.add(createPair("Seat Availability", ride.getAvailableSeats() + " of " + ride.getPassengerCapacity() + " seats left"));
        infoCard.add(createPair("Total Estimated Fare", FareService.formatINR(ride.getTotalFare())));

        double distance = destinationDAO.getDistanceKm(ride.getOriginId(), ride.getDestinationId());
        infoCard.add(createPair("Estimated Distance", String.format("%.1f km", distance)));

        String prefs = "";
        if (ride.isWomenOnly()) prefs += "[Women-Only] ";
        if (ride.isSameDepartmentOnly()) prefs += "[Same-Dept Only] ";
        if (prefs.isEmpty()) prefs = "None (Open to all)";
        infoCard.add(createPair("Special Preferences", prefs));

        String notes = ride.getNotes() != null && !ride.getNotes().isEmpty() ? ride.getNotes() : "No special notes";
        infoCard.add(createPair("Notes", notes));

        content.add(infoCard);
        content.add(Box.createVerticalStrut(16));

        // 3. FARE SHARE & SAVINGS PREVIEW CARD
        JPanel fareCard = new JPanel(new BorderLayout(8, 8));
        fareCard.setBackground(new Color(24, 38, 62));
        fareCard.setBorder(UITheme.createCardBorder());

        JLabel lblFareTitle = new JLabel("Smart Fare-Sharing & Estimated Savings");
        lblFareTitle.setFont(UITheme.FONT_SUBHEADER);
        lblFareTitle.setForeground(UITheme.ACCENT_CYAN);
        fareCard.add(lblFareTitle, BorderLayout.NORTH);

        // Fetch confirmed passengers
        List<Booking> passengers = List.of();
        try {
            passengers = bookingDAO.findByRideId(ride.getRideId());
        } catch (Exception ignored) {}

        int confirmedCount = passengers.size();
        // If the viewer joins, participants = 1 (driver) + confirmedCount + 1 (viewer)
        int projectedParticipants = 1 + confirmedCount + 1;
        BigDecimal projectedShare = fareService.calculateShare(ride.getTotalFare(), projectedParticipants);
        BigDecimal soloBaseline = fareService.calculateSoloBaseline(ride.getVehicleType(), ride.getOriginId(), ride.getDestinationId());
        BigDecimal projectedSavings = fareService.calculateSavings(soloBaseline, projectedShare);

        JPanel fareGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        fareGrid.setOpaque(false);

        fareGrid.add(createPair("Projected Participants", projectedParticipants + " people (1 driver + " + (confirmedCount + 1) + " passengers)"));
        fareGrid.add(createPair("Your Share if you join", FareService.formatINR(projectedShare)));
        fareGrid.add(createPair("Solo Travel Baseline", FareService.formatINR(soloBaseline)));
        fareGrid.add(createPair("Your Estimated Savings", FareService.formatINR(projectedSavings)));

        fareCard.add(fareGrid, BorderLayout.CENTER);
        content.add(fareCard);
        content.add(Box.createVerticalStrut(16));

        // 4. CONFIRMED PASSENGERS LIST
        JPanel passCard = new JPanel(new BorderLayout(8, 6));
        passCard.setBackground(UITheme.BG_CARD);
        passCard.setBorder(UITheme.createCardBorder());

        JLabel lblPassTitle = new JLabel("Confirmed Co-Passengers (" + confirmedCount + ")");
        lblPassTitle.setFont(UITheme.FONT_SUBHEADER);
        lblPassTitle.setForeground(UITheme.TEXT_PRIMARY);
        passCard.add(lblPassTitle, BorderLayout.NORTH);

        JPanel passList = new JPanel();
        passList.setLayout(new BoxLayout(passList, BoxLayout.Y_AXIS));
        passList.setOpaque(false);

        if (passengers.isEmpty()) {
            JLabel lblNoPass = new JLabel("No passengers have joined yet. Be the first to pool!", SwingConstants.LEFT);
            lblNoPass.setFont(UITheme.FONT_BODY);
            lblNoPass.setForeground(UITheme.TEXT_MUTED);
            passList.add(lblNoPass);
        } else {
            for (Booking b : passengers) {
                JLabel p = new JLabel("• " + b.getPassengerName() + " (" + b.getPassengerDepartment() + ", " + b.getPassengerGender() + ")");
                p.setFont(UITheme.FONT_BODY);
                p.setForeground(UITheme.TEXT_SECONDARY);
                passList.add(p);
            }
        }
        passCard.add(passList, BorderLayout.CENTER);
        content.add(passCard);

        content.add(Box.createVerticalStrut(20));

        // 5. ACTION BUTTONS
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        ModernButton btnClose = new ModernButton("Close", ModernButton.Variant.SECONDARY);
        btnClose.addActionListener(e -> dispose());

        Student current = mainFrame.getCurrentStudent();
        boolean isCreator = current != null && current.getStudentId() == ride.getDriverId();
        boolean isOpen = "OPEN".equalsIgnoreCase(ride.getStatus()) && ride.getAvailableSeats() > 0;

        btnJoin = new ModernButton("Join Ride & Split Fare", ModernButton.Variant.SUCCESS);
        if (isCreator) {
            btnJoin.setEnabled(false);
            btnJoin.setText("You are the Ride Creator");
        } else if (!isOpen) {
            btnJoin.setEnabled(false);
            btnJoin.setText("Ride Full or Closed");
        } else {
            btnJoin.addActionListener(e -> performJoinRide());
        }

        btnPanel.add(btnClose);
        btnPanel.add(btnJoin);
        content.add(btnPanel);

        getContentPane().add(content);
    }

    private JPanel createPair(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(4, 2));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(UITheme.FONT_BODY_BOLD);
        v.setForeground(UITheme.TEXT_PRIMARY);

        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private void performJoinRide() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) {
            JOptionPane.showMessageDialog(this, "Please log in to join this ride.", "Login Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to reserve a seat on Ride #" + ride.getRideId() + "?\n" +
            "Route: " + ride.getOriginName() + " → " + ride.getDestinationName() + "\n" +
            "Departure: " + DateUtil.formatDateTime(ride.getDepartureTime()),
            "Confirm Booking",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnJoin.setEnabled(false);
        btnJoin.setText("Securing Seat...");

        SwingWorker<Booking, Void> worker = new SwingWorker<>() {
            @Override
            protected Booking doInBackground() throws Exception {
                return bookingService.joinRide(ride.getRideId(), current);
            }

            @Override
            protected void done() {
                try {
                    Booking booking = get();
                    JOptionPane.showMessageDialog(
                        RideDetailsDialog.this,
                        "Booking Confirmed!\n" +
                        "Booking ID: #" + booking.getBookingId() + "\n" +
                        "Route: " + ride.getOriginName() + " → " + ride.getDestinationName() + "\n" +
                        "Your Fare Share: " + FareService.formatINR(booking.getFareShareSnapshot()),
                        "Seat Reserved Successfully",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    dispose();
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                } catch (Exception ex) {
                    btnJoin.setEnabled(true);
                    btnJoin.setText("Join Ride & Split Fare");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof BookingException) {
                        JOptionPane.showMessageDialog(
                            RideDetailsDialog.this,
                            cause.getMessage(),
                            "Cannot Join Ride",
                            JOptionPane.WARNING_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                            RideDetailsDialog.this,
                            "Booking transaction failed: " + cause.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        };
        worker.execute();
    }
}
