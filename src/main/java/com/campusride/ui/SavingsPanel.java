package com.campusride.ui;

import com.campusride.dao.ReportDAO;
import com.campusride.model.Student;
import com.campusride.service.FareService;
import com.campusride.service.ReportService;
import com.campusride.ui.components.ModernButton;
import com.campusride.ui.components.StatCard;
import com.campusride.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Screen presenting personal student travel savings,
 * comparing shared travel costs against individual solo baselines.
 */
public class SavingsPanel extends JPanel {
    private final MainFrame mainFrame;
    private final ReportService reportService;

    private StatCard cardCompleted;
    private StatCard cardTotalSavings;
    private StatCard cardAvgPaid;
    private StatCard cardTopHotspot;

    private JTable tblHistory;
    private DefaultTableModel modelHistory;

    private JPanel cardContainer;
    private CardLayout cardLayout;
    private JLabel lblEmpty;

    public SavingsPanel(MainFrame mainFrame) {
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

        JLabel lblTitle = new JLabel("Savings & Travel Insights 💰");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Track your actual financial savings from completed shared trips versus travelling alone.");
        lblSub.setFont(UITheme.FONT_BODY);
        lblSub.setForeground(UITheme.TEXT_SECONDARY);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // CENTER CONTENT
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
        centerContent.setOpaque(false);

        // 1. STAT CARDS ROW
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        cardCompleted = new StatCard("Completed Trips", "0", "Archived shared rides", UITheme.STATUS_COMPLETED_FG);
        cardTotalSavings = new StatCard("Total Savings", "₹0.00", "Saved vs solo baseline", UITheme.STATUS_OPEN_FG);
        cardAvgPaid = new StatCard("Average Share Paid", "₹0.00", "Per completed trip", UITheme.PRIMARY);
        cardTopHotspot = new StatCard("Favorite Hotspot", "None", "Most frequent destination", UITheme.ACCENT_CYAN);

        statsRow.add(cardCompleted);
        statsRow.add(cardTotalSavings);
        statsRow.add(cardAvgPaid);
        statsRow.add(cardTopHotspot);

        centerContent.add(statsRow);
        centerContent.add(Box.createVerticalStrut(16));

        // 2. SAVINGS METHODOLOGY BANNER
        JPanel methodCard = new JPanel(new BorderLayout(8, 4));
        methodCard.setBackground(new Color(24, 38, 62));
        methodCard.setBorder(UITheme.createCardBorder());
        methodCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JLabel lblMethodTitle = new JLabel("CampusRide Fare-Sharing Formula & Methodology");
        lblMethodTitle.setFont(UITheme.FONT_SUBHEADER);
        lblMethodTitle.setForeground(UITheme.ACCENT_CYAN);

        JLabel lblMethodDesc = new JLabel(
            "<html>Estimated Individual Savings = <b>Solo Travel Baseline Fare − Shared Fare Paid</b>.<br/>" +
            "Savings are recorded upon ride completion and exclude cancelled rides to maintain academic integrity.</html>"
        );
        lblMethodDesc.setFont(UITheme.FONT_SMALL);
        lblMethodDesc.setForeground(UITheme.TEXT_SECONDARY);

        methodCard.add(lblMethodTitle, BorderLayout.NORTH);
        methodCard.add(lblMethodDesc, BorderLayout.CENTER);
        centerContent.add(methodCard);
        centerContent.add(Box.createVerticalStrut(16));

        // 3. COMPLETED TRIPS TABLE
        JPanel histCard = new JPanel(new BorderLayout(8, 8));
        histCard.setBackground(UITheme.BG_CARD);
        histCard.setBorder(UITheme.createCardBorder());

        JLabel lblHistTitle = new JLabel("Completed Shared Journey History");
        lblHistTitle.setFont(UITheme.FONT_HEADER);
        lblHistTitle.setForeground(UITheme.TEXT_PRIMARY);
        histCard.add(lblHistTitle, BorderLayout.NORTH);

        String[] cols = {"Ride ID", "Route", "Date", "Vehicle", "Your Role", "Solo Baseline", "Your Shared Fare", "You Saved"};
        modelHistory = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblHistory = new JTable(modelHistory);
        UITheme.styleTable(tblHistory);

        JScrollPane scrollTable = new JScrollPane(tblHistory);
        scrollTable.getViewport().setBackground(UITheme.BG_CARD);
        scrollTable.setBorder(BorderFactory.createEmptyBorder());

        lblEmpty = new JLabel("No completed shared trips recorded yet. Complete your first journey to begin tracking savings!", SwingConstants.CENTER);
        lblEmpty.setFont(UITheme.FONT_BODY);
        lblEmpty.setForeground(UITheme.TEXT_MUTED);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);
        cardContainer.add(scrollTable, "TABLE");
        cardContainer.add(lblEmpty, "EMPTY");
        histCard.add(cardContainer, BorderLayout.CENTER);

        centerContent.add(histCard);

        add(centerContent, BorderLayout.CENTER);

        // BOTTOM REFRESH
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);
        ModernButton btnRefresh = new ModernButton("⟳ Refresh Savings", ModernButton.Variant.SECONDARY);
        btnRefresh.addActionListener(e -> refreshData());
        bottomBar.add(btnRefresh);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void refreshData() {
        Student current = mainFrame.getCurrentStudent();
        if (current == null) return;

        SwingWorker<ReportDAO.StudentSavingsSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected ReportDAO.StudentSavingsSummary doInBackground() throws Exception {
                return reportService.getStudentSavings(current.getStudentId());
            }

            @Override
            protected void done() {
                try {
                    ReportDAO.StudentSavingsSummary summary = get();
                    cardCompleted.setValue(String.valueOf(summary.completedRidesCount()));
                    cardTotalSavings.setValue(FareService.formatINR(summary.totalSavings()));
                    cardAvgPaid.setValue(FareService.formatINR(summary.averageSharePaid()));
                    cardTopHotspot.setValue(summary.topDestination());

                    modelHistory.setRowCount(0);
                    if (summary.history().isEmpty()) {
                        cardLayout.show(cardContainer, "EMPTY");
                    } else {
                        cardLayout.show(cardContainer, "TABLE");
                        for (ReportDAO.FareRecordDetail d : summary.history()) {
                            modelHistory.addRow(new Object[]{
                                "#" + d.rideId(),
                                d.route(),
                                d.travelDate(),
                                d.vehicleType(),
                                d.role(),
                                FareService.formatINR(d.soloFare()),
                                FareService.formatINR(d.sharedFare()),
                                FareService.formatINR(d.savings())
                            });
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Savings panel error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
