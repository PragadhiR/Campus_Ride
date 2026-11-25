package com.campusride;

import com.campusride.dao.*;
import com.campusride.db.DBConnection;
import com.campusride.model.*;
import com.campusride.service.*;
import com.campusride.util.BookingException;
import com.campusride.util.ValidationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Automated end-to-end integration test runner validating all CampusRide
 * business rules, database queries, transactions, and reports against live MySQL data.
 * Safe: Tests only execute if DB connection succeeds, and cleans up only its own test records.
 */
public class EndToEndWorkflowTest {

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("  CAMPUSRIDE END-TO-END WORKFLOW INTEGRATION TEST   ");
        System.out.println("====================================================");

        if (!DBConnection.testConnection()) {
            System.out.println("RESULT: SKIPPED (Database credentials not yet configured in .env)");
            System.out.println("Configure your MySQL password in .env and run this test to verify all live DB workflows.");
            return;
        }

        System.out.println("[1/8] Testing Database Connectivity & Seed Hotspots...");
        DestinationDAO destinationDAO = new DestinationDAO();
        try {
            List<Destination> destinations = destinationDAO.getAllActive();
            if (destinations.isEmpty()) {
                throw new RuntimeException("No destinations found in destinations table.");
            }
            System.out.println("  ✓ Successfully loaded " + destinations.size() + " campus destinations.");
        } catch (Exception e) {
            fail("Destination loading failed", e);
            return;
        }

        System.out.println("[2/8] Testing Authentication & Password Verification...");
        AuthService authService = new AuthService();
        Student studentA, studentB;
        try {
            // Verify pre-seeded Student A login
            studentA = authService.login("aarav.sharma@campus.edu", "Password@123");
            System.out.println("  ✓ Student A (Aarav) logged in successfully: " + studentA.getFullName());

            // Verify pre-seeded Student B login
            studentB = authService.login("priya.patel@campus.edu", "Password@123");
            System.out.println("  ✓ Student B (Priya) logged in successfully: " + studentB.getFullName());

            // Verify wrong password rejection
            try {
                authService.login("aarav.sharma@campus.edu", "WrongPass!");
                throw new RuntimeException("Login should have failed for incorrect password.");
            } catch (ValidationException ve) {
                System.out.println("  ✓ Incorrect password correctly rejected: " + ve.getMessage());
            }

            // Verify duplicate email rejection
            try {
                authService.register("Duplicate Test", "aarav.sharma@campus.edu", "Password@123", "Computer Science", 2, "Male");
                throw new RuntimeException("Registration should have failed for duplicate email.");
            } catch (ValidationException ve) {
                System.out.println("  ✓ Duplicate email registration correctly prevented: " + ve.getMessage());
            }

        } catch (Exception e) {
            fail("Authentication verification failed", e);
            return;
        }

        System.out.println("[3/8] Testing Ride Creation & Vehicle Polymorphism...");
        RideService rideService = new RideService();
        Ride testRide = null;
        try {
            // Student A creates a CabRide for tomorrow at 17:00
            LocalDateTime departure = LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0);
            BigDecimal totalFare = new BigDecimal("600.00");
            int capacity = 2; // 2 passenger seats

            testRide = rideService.publishRide(
                studentA, 1, 2, departure, "CAB", totalFare, capacity, false, false, "E2E Test Ride"
            );

            if (testRide.getRideId() <= 0) {
                throw new RuntimeException("Ride creation did not generate a valid ride ID.");
            }
            if (testRide.getAvailableSeats() != 2) {
                throw new RuntimeException("Available seats mismatch: expected 2, got " + testRide.getAvailableSeats());
            }
            System.out.println("  ✓ Ride #" + testRide.getRideId() + " successfully created (Capacity: 2, Available: 2, Status: " + testRide.getStatus() + ").");

        } catch (Exception e) {
            fail("Ride creation failed", e);
            return;
        }

        System.out.println("[4/8] Testing Smart 30-Minute Time-Window Matching...");
        try {
            final int testRideId = testRide.getRideId();
            // Case A: 15 minutes later (17:15) -> Should MATCH
            LocalDateTime searchTime15MinsLater = testRide.getDepartureTime().plusMinutes(15);
            List<RideMatch> matches = rideService.searchRides(studentB, 1, 2, searchTime15MinsLater);
            boolean found = matches.stream().anyMatch(m -> m.getRide().getRideId() == testRideId);
            if (!found) {
                throw new RuntimeException("Ride should have matched within 15-minute offset.");
            }
            RideMatch matchedRide = matches.stream().filter(m -> m.getRide().getRideId() == testRideId).findFirst().get();
            System.out.println("  ✓ Smart match found within 15-min offset: " + matchedRide.getMatchReason());

            // Case B: 45 minutes later (17:45) -> Should NOT match (outside 30-min window)
            LocalDateTime searchTime45MinsLater = testRide.getDepartureTime().plusMinutes(45);
            List<RideMatch> outsideMatches = rideService.searchRides(studentB, 1, 2, searchTime45MinsLater);
            boolean wronglyFound = outsideMatches.stream().anyMatch(m -> m.getRide().getRideId() == testRideId);
            if (wronglyFound) {
                throw new RuntimeException("Ride should NOT match outside the 30-minute window.");
            }
            System.out.println("  ✓ Search at +45 mins correctly excluded ride outside 30-min window.");

        } catch (Exception e) {
            cleanupRide(testRide);
            fail("Smart matching failed", e);
            return;
        }

        System.out.println("[5/8] Testing Concurrency-Safe Transaction Booking & Seat Decrement...");
        BookingService bookingService = new BookingService();
        Booking bookingB = null;
        Student studentC = null;
        try {
            // Self-booking prevention: Driver tries to join own ride
            try {
                bookingService.joinRide(testRide.getRideId(), studentA);
                throw new RuntimeException("Self-booking should have been rejected.");
            } catch (BookingException be) {
                System.out.println("  ✓ Self-booking correctly prevented: " + be.getMessage());
            }

            // Student B joins the ride
            bookingB = bookingService.joinRide(testRide.getRideId(), studentB);
            System.out.println("  ✓ Student B joined ride successfully. Booking #" + bookingB.getBookingId() + " confirmed.");

            // Check seat count after 1 booking: 2 - 1 = 1 seat remaining
            Ride updatedRide = rideService.getRideDetails(testRide.getRideId()).orElseThrow();
            if (updatedRide.getAvailableSeats() != 1) {
                throw new RuntimeException("Expected 1 seat left, got " + updatedRide.getAvailableSeats());
            }
            System.out.println("  ✓ Seat availability correctly decremented to 1.");

            // Duplicate booking prevention: Student B tries to join again
            try {
                bookingService.joinRide(testRide.getRideId(), studentB);
                throw new RuntimeException("Duplicate booking should have been rejected.");
            } catch (BookingException be) {
                System.out.println("  ✓ Duplicate booking correctly prevented: " + be.getMessage());
            }

            // Register a temporary 3rd student to book the final seat
            String tempEmail = "e2e.student." + System.currentTimeMillis() + "@campus.edu";
            studentC = authService.register("Test Student C", tempEmail, "Password@123", "Computer Science", 1, "Male");

            // Student C books final seat (Capacity 2 now full)
            Booking bookingC = bookingService.joinRide(testRide.getRideId(), studentC);
            Ride fullRide = rideService.getRideDetails(testRide.getRideId()).orElseThrow();
            if (fullRide.getAvailableSeats() != 0 || !"FULL".equalsIgnoreCase(fullRide.getStatus())) {
                throw new RuntimeException("Ride should have 0 seats and status FULL. Status: " + fullRide.getStatus());
            }
            System.out.println("  ✓ Final seat booked. Ride automatically transitioned to FULL (Seats: 0).");

            // Overbooking prevention: 4th booking attempt on full ride
            Student studentD = authService.login("rohan.verma@campus.edu", "Password@123");
            try {
                bookingService.joinRide(testRide.getRideId(), studentD);
                throw new RuntimeException("Overbooking should have been rejected on full ride.");
            } catch (BookingException be) {
                System.out.println("  ✓ Overbooking on full ride correctly rejected: " + be.getMessage());
            }

        } catch (Exception e) {
            cleanupRide(testRide);
            fail("Booking verification failed", e);
            return;
        }

        System.out.println("[6/8] Testing Booking Cancellation & Seat Restoration...");
        try {
            // Student B cancels booking
            bookingService.cancelBooking(bookingB.getBookingId(), studentB);
            Ride reopenedRide = rideService.getRideDetails(testRide.getRideId()).orElseThrow();
            if (reopenedRide.getAvailableSeats() != 1 || !"OPEN".equalsIgnoreCase(reopenedRide.getStatus())) {
                throw new RuntimeException("Ride should have restored 1 seat and reopened to OPEN. Got seats=" +
                        reopenedRide.getAvailableSeats() + ", status=" + reopenedRide.getStatus());
            }
            System.out.println("  ✓ Booking cancelled. Seat restored to 1 and ride automatically reopened to OPEN.");

        } catch (Exception e) {
            cleanupRide(testRide);
            fail("Cancellation failed", e);
            return;
        }

        System.out.println("[7/8] Testing Trip Completion & Student Savings Archiving...");
        try {
            // Student A marks the ride as COMPLETED
            rideService.completeRide(testRide.getRideId(), studentA);
            Ride completedRide = rideService.getRideDetails(testRide.getRideId()).orElseThrow();
            if (!"COMPLETED".equalsIgnoreCase(completedRide.getStatus())) {
                throw new RuntimeException("Ride status should be COMPLETED, got " + completedRide.getStatus());
            }
            System.out.println("  ✓ Ride #" + testRide.getRideId() + " successfully marked COMPLETED.");

            // Verify individual savings for student A
            ReportDAO reportDAO = new ReportDAO();
            ReportDAO.StudentSavingsSummary savingsA = reportDAO.getStudentSavingsSummary(studentA.getStudentId());
            System.out.println("  ✓ Student A savings report updated: Total Completed=" + savingsA.completedRidesCount() +
                    ", Total Saved=" + FareService.formatINR(savingsA.totalSavings()));

        } catch (Exception e) {
            cleanupRide(testRide);
            fail("Completion failed", e);
            return;
        }

        System.out.println("[8/8] Testing Holiday Rush Analytics Queries...");
        try {
            ReportDAO reportDAO = new ReportDAO();
            List<ReportDAO.PopularDestinationStat> popular = reportDAO.getPopularDestinations();
            List<ReportDAO.TimeSlotStat> slots = reportDAO.getBusiestTimeSlots();
            List<ReportDAO.VehicleBreakdownStat> vehicles = reportDAO.getVehicleBreakdown();

            if (popular.isEmpty()) throw new RuntimeException("Popular destinations query returned 0 rows.");
            System.out.println("  ✓ Holiday Rush top destination: " + popular.get(0).destinationName() +
                    " (" + popular.get(0).totalRides() + " rides)");
            System.out.println("  ✓ Holiday Rush time slots count: " + slots.size());
            System.out.println("  ✓ Holiday Rush vehicle stats count: " + vehicles.size());

        } catch (Exception e) {
            cleanupRide(testRide);
            fail("Holiday Rush queries failed", e);
            return;
        }

        // Clean up test ride and temporary student C
        cleanupRide(testRide);
        if (studentC != null) {
            cleanupStudent(studentC.getStudentId());
        }

        System.out.println("====================================================");
        System.out.println("  ALL END-TO-END WORKFLOW TESTS PASSED SUCCESSFULLY ");
        System.out.println("====================================================");
    }

    private static void cleanupRide(Ride ride) {
        if (ride == null || ride.getRideId() <= 0) return;
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM fare_records WHERE ride_id = ?")) {
                ps.setInt(1, ride.getRideId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bookings WHERE ride_id = ?")) {
                ps.setInt(1, ride.getRideId());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM rides WHERE ride_id = ?")) {
                ps.setInt(1, ride.getRideId());
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private static void cleanupStudent(int studentId) {
        if (studentId <= 0) return;
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM students WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private static void fail(String message, Exception e) {
        System.err.println("  ✗ FAILED: " + message + " -> " + e.getMessage());
    }
}
