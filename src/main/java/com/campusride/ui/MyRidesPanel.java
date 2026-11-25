package com.campusride.ui;

import com.campusride.dao.BookingDAO;
import com.campusride.model.Booking;
import com.campusride.model.Ride;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Screen displaying rides published by the currently logged-in student,
 * with options to view passengers, cancel the ride, or mark the trip as completed.
 */
public class MyRidesPanel extends JPanel {
    private final MainFrame mainFrame;
    private final RideService rideService;
    private final BookingDAO bookingDAO;

    private JTable tblRides;
    private DefaultTableModel modelRides;
    private List<Ride> currentRides = new ArrayList<>();

    private JPanel cardContainer;
    private CardLayout cardLayout;
    private JLabel lblEmpty;

    public MyRidesPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.rideService = new RideService();
        this.bookingDAO = new BookingDAO();

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // TOP HEADER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("My Published Rides");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Manage rides where you are the driver. Track passengers and complete journeys.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // TABLE & EMPTY CONTAINER
        String[] cols = {"Ride ID", "Route", "Departure Date & Time", "Vehicle", "Total Fare", "Capacity", "Seats Left", "Status"};
        modelRides = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblRides = new JTable(modelRides);
        UITheme.styleTable(tblRides);

        JScrollPane scrollTable = new JScrollPane(tblRides);
        scrollTable.getViewport().setBackground(UITheme.BG_CARD);
        scrollTable.setBorder(UITheme.createCardBorder());

        lblEmpty = new JLabel("You have not published any rides yet. Click '+ Create a Ride' to share a journey!", SwingConstants.CENTER);
        lblEmpty.setFont(UITheme.FONT_BODY);
        lblEmpty.setForeground(UITheme.TEXT_MUTED);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);
        cardContainer.add(scrollTable, "TABLE");
        cardContainer.add(lblEmpty, "EMPTY");
        add(cardContainer, BorderLayout.CENTER);

        // BOTTOM ACTION TOOLBAR
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottomBar.setOpaque(false);

        ModernButton btnRefresh = new ModernButton("⟳ Refresh", ModernButton.Variant.SECONDARY);
        btnRefresh.addActionListener(e -> refreshData());

        ModernButton btnViewPassengers = new ModernButton("👥 View Passengers", ModernButton.Variant.OUTLINE);
        btnViewPassengers.addActionListener(e -> viewPassengers());

        ModernButton btnCancel = new ModernButton("Cancel Ride", ModernButton.Variant.DANGER);
        btnCancel.addActionListener(e -> cancelSelectedRide());

        ModernButton btnComplete = new ModernButton("✓ Mark Trip Completed", ModernButton.Variant.SUCCESS);
        btnComplete.addActionListener(e -> completeSelectedRide());

        bottomBar.add(btnRefresh);
        bottomBar.add(btnViewPassengers);
        bottomBar.add(btnCancel);
        bottomBar.add(btnComplete);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void refreshData() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) return;

        SwingWorker<List<Ride>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Ride> doInBackground() throws Exception {
                return rideService.getRidesCreatedBy(current.getStudentId());
            }

            @Override
            protected void done() {
                try {
                    currentRides = get();
                    modelRides.setRowCount(0);

                    if (currentRides.isEmpty()) {
                        cardLayout.show(cardContainer, "EMPTY");
                    } else {
                        cardLayout.show(cardContainer, "TABLE");
                        for (Ride r : currentRides) {
                            modelRides.addRow(new Object[]{
                                "#" + r.getRideId(),
                                r.getOriginName() + " → " + r.getDestinationName(),
                                DateUtil.formatDateTime(r.getDepartureTime()),
                                r.getVehicleType(),
                                FareService.formatINR(r.getTotalFare()),
                                r.getPassengerCapacity(),
                                r.getAvailableSeats(),
                                r.getStatus()
                            });
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error loading my rides: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void viewPassengers() {
        int row = tblRides.getSelectedRow();
        if (row < 0 || row >= currentRides.size()) {
            JOptionPane.showMessageDialog(this, "Please select a ride from the table first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Ride ride = currentRides.get(row);
        try {
            List<Booking> passengers = bookingDAO.findByRideId(ride.getRideId());
            StringBuilder sb = new StringBuilder();
            sb.append("Confirmed Passengers for Ride #").append(ride.getRideId()).append("\n");
            sb.append("Route: ").append(ride.getOriginName()).append(" → ").append(ride.getDestinationName()).append("\n");
            sb.append("--------------------------------------------------\n");

            if (passengers.isEmpty()) {
                sb.append("No passengers have joined this ride yet.");
            } else {
                for (int i = 0; i < passengers.size(); i++) {
                    Booking b = passengers.get(i);
                    sb.append(i + 1).append(". ")
                      .append(b.getPassengerName()).append(" (")
                      .append(b.getPassengerDepartment()).append(", ")
                      .append(b.getPassengerGender()).append(")\n")
                      .append("   Booked at: ").append(DateUtil.formatDateTime(b.getBookingTime())).append("\n");
                }
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Passenger Roster", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching passengers: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedRide() {
        int row = tblRides.getSelectedRow();
        if (row < 0 || row >= currentRides.size()) {
            JOptionPane.showMessageDialog(this, "Please select a ride to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Ride ride = currentRides.get(row);
        if ("CANCELLED".equalsIgnoreCase(ride.getStatus())) {
            JOptionPane.showMessageDialog(this, "This ride has already been cancelled.", "Already Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("COMPLETED".equalsIgnoreCase(ride.getStatus())) {
            JOptionPane.showMessageDialog(this, "Completed rides cannot be cancelled.", "Already Completed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to CANCEL Ride #" + ride.getRideId() + "?\n" +
            "This will notify and cancel all active passenger bookings.",
            "Confirm Ride Cancellation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                rideService.cancelRide(ride.getRideId(), mainFrame.getCurrentStudent());
                JOptionPane.showMessageDialog(this, "Ride #" + ride.getRideId() + " has been cancelled.", "Ride Cancelled", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                mainFrame.getDashboardPanel().refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Cancellation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void completeSelectedRide() {
        int row = tblRides.getSelectedRow();
        if (row < 0 || row >= currentRides.size()) {
            JOptionPane.showMessageDialog(this, "Please select a ride to mark as completed.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Ride ride = currentRides.get(row);
        if (!"OPEN".equalsIgnoreCase(ride.getStatus()) && !"FULL".equalsIgnoreCase(ride.getStatus())) {
            JOptionPane.showMessageDialog(this, "Only OPEN or FULL rides can be completed.", "Cannot Complete", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Confirm completion of Ride #" + ride.getRideId() + "?\n" +
            "This will finalize shared fare payments and update all student savings reports.",
            "Mark Trip Completed",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                rideService.completeRide(ride.getRideId(), mainFrame.getCurrentStudent());
                JOptionPane.showMessageDialog(this,
                    "Ride #" + ride.getRideId() + " successfully marked as COMPLETED!\n" +
                    "Fare sharing and savings records have been archived.",
                    "Trip Completed",
                    JOptionPane.INFORMATION_MESSAGE
                );
                refreshData();
                mainFrame.getDashboardPanel().refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Completion Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
