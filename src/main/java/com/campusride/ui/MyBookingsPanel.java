package com.campusride.ui;

import com.campusride.model.Booking;
import com.campusride.model.Student;
import com.campusride.service.BookingService;
import com.campusride.service.FareService;
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
 * Screen displaying rides joined by the currently logged-in student,
 * with options to view ride information and cancel active reservations.
 */
public class MyBookingsPanel extends JPanel {
    private final MainFrame mainFrame;
    private final BookingService bookingService;

    private JTable tblBookings;
    private DefaultTableModel modelBookings;
    private List<Booking> currentBookings = new ArrayList<>();

    private JPanel cardContainer;
    private CardLayout cardLayout;
    private JLabel lblEmpty;

    public MyBookingsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.bookingService = new BookingService();

        setLayout(new BorderLayout(16, 16));
        setBackground(UITheme.BG_DARKEST);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        initUI();
    }

    private void initUI() {
        // TOP HEADER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("My Booked Rides");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Track journeys where you reserved a passenger seat and split the fare.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // TABLE & EMPTY CONTAINER
        String[] cols = {"Booking ID", "Ride ID", "Driver", "Route", "Departure Date & Time", "Vehicle", "Your Share", "Booking Status", "Ride Status"};
        modelBookings = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBookings = new JTable(modelBookings);
        UITheme.styleTable(tblBookings);

        JScrollPane scrollTable = new JScrollPane(tblBookings);
        scrollTable.getViewport().setBackground(UITheme.BG_CARD);
        scrollTable.setBorder(UITheme.createCardBorder());

        lblEmpty = new JLabel("You have no ride reservations yet. Search and join a ride in 'Find a Ride'!", SwingConstants.CENTER);
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

        ModernButton btnCancel = new ModernButton("Cancel Booking", ModernButton.Variant.DANGER);
        btnCancel.addActionListener(e -> cancelSelectedBooking());

        bottomBar.add(btnRefresh);
        bottomBar.add(btnCancel);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void refreshData() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) return;

        SwingWorker<List<Booking>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Booking> doInBackground() throws Exception {
                return bookingService.getBookingsByPassenger(current.getStudentId());
            }

            @Override
            protected void done() {
                try {
                    currentBookings = get();
                    modelBookings.setRowCount(0);

                    if (currentBookings.isEmpty()) {
                        cardLayout.show(cardContainer, "EMPTY");
                    } else {
                        cardLayout.show(cardContainer, "TABLE");
                        for (Booking b : currentBookings) {
                            modelBookings.addRow(new Object[]{
                                "#" + b.getBookingId(),
                                "#" + b.getRideId(),
                                b.getDriverName(),
                                b.getOriginName() + " → " + b.getDestinationName(),
                                DateUtil.formatDateTime(b.getDepartureTime()),
                                b.getVehicleType(),
                                FareService.formatINR(b.getFareShareSnapshot()),
                                b.getBookingStatus(),
                                b.getRideStatus()
                            });
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error loading my bookings: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void cancelSelectedBooking() {
        int row = tblBookings.getSelectedRow();
        if (row < 0 || row >= currentBookings.size()) {
            JOptionPane.showMessageDialog(this, "Please select a booking to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Booking booking = currentBookings.get(row);
        if (!"CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())) {
            JOptionPane.showMessageDialog(this, "This booking is already cancelled.", "Already Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("COMPLETED".equalsIgnoreCase(booking.getRideStatus())) {
            JOptionPane.showMessageDialog(this, "Completed ride bookings cannot be cancelled.", "Ride Completed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to CANCEL your seat on Ride #" + booking.getRideId() + "?\n" +
            "Your seat will be released and returned to available inventory.",
            "Confirm Cancellation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                bookingService.cancelBooking(booking.getBookingId(), mainFrame.getCurrentStudent());
                JOptionPane.showMessageDialog(this, "Your booking #" + booking.getBookingId() + " has been cancelled.", "Seat Released", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
                mainFrame.getDashboardPanel().refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cancellation failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
