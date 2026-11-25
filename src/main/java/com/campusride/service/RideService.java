package com.campusride.service;

import com.campusride.dao.BookingDAO;
import com.campusride.dao.DestinationDAO;
import com.campusride.dao.ReportDAO;
import com.campusride.dao.RideDAO;
import com.campusride.db.DBConnection;
import com.campusride.model.Booking;
import com.campusride.model.Ride;
import com.campusride.model.RideMatch;
import com.campusride.model.Student;
import com.campusride.util.ValidationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing ride publishing, smart search, cancellations, and trip completion.
 */
public class RideService {
    private final RideDAO rideDAO;
    private final DestinationDAO destinationDAO;
    private final BookingDAO bookingDAO;
    private final ReportDAO reportDAO;
    private final FareService fareService;

    public RideService() {
        this.rideDAO = new RideDAO();
        this.destinationDAO = new DestinationDAO();
        this.bookingDAO = new BookingDAO();
        this.reportDAO = new ReportDAO();
        this.fareService = new FareService(destinationDAO);
    }

    public RideService(RideDAO rideDAO, DestinationDAO destinationDAO,
                       BookingDAO bookingDAO, ReportDAO reportDAO,
                       FareService fareService) {
        this.rideDAO = rideDAO;
        this.destinationDAO = destinationDAO;
        this.bookingDAO = bookingDAO;
        this.reportDAO = reportDAO;
        this.fareService = fareService;
    }

    /**
     * Publishes a new ride listing created by a student.
     */
    public Ride publishRide(Student driver, int originId, int destId,
                            LocalDateTime departureTime, String vehicleType,
                            BigDecimal totalFare, int capacity,
                            boolean womenOnly, boolean sameDeptOnly,
                            String notes) throws ValidationException, SQLException {
        if (driver == null) {
            throw new ValidationException("User session expired. Please log in again.");
        }

        // Subclass instantiation via polymorphic factory
        Ride ride = Ride.create(vehicleType);
        ride.setDriverId(driver.getStudentId());
        ride.setOriginId(originId);
        ride.setDestinationId(destId);
        ride.setDepartureTime(departureTime);
        ride.setTotalFare(totalFare);
        ride.setPassengerCapacity(capacity);
        ride.setAvailableSeats(capacity);
        ride.setWomenOnly(womenOnly);
        ride.setSameDepartmentOnly(sameDeptOnly);
        ride.setNotes(notes != null ? notes.trim() : null);
        ride.setStatus("OPEN");

        // Validate all OOP model constraints
        ride.validate();

        return rideDAO.create(ride);
    }

    /**
     * Performs smart 30-minute time-window ride search.
     */
    public List<RideMatch> searchRides(Student searchingStudent, int originId, int destId,
                                       LocalDateTime requestedTime) throws ValidationException, SQLException {
        if (originId <= 0 || destId <= 0) {
            throw new ValidationException("Please select valid origin and destination locations.");
        }
        if (originId == destId) {
            throw new ValidationException("Origin and destination must be different.");
        }
        if (requestedTime == null) {
            throw new ValidationException("Travel date and time are required.");
        }

        double distance = destinationDAO.getDistanceKm(originId, destId);
        return rideDAO.searchSmartMatches(originId, destId, requestedTime, searchingStudent, distance);
    }

    public Optional<Ride> getRideDetails(int rideId) throws SQLException {
        return rideDAO.findById(rideId);
    }

    public List<Ride> getRidesCreatedBy(int driverId) throws SQLException {
        return rideDAO.findByDriverId(driverId);
    }

    /**
     * Transaction-safe ride cancellation by creator.
     * Cancels the ride and all active passenger bookings atomically.
     */
    public void cancelRide(int rideId, Student currentStudent) throws ValidationException, SQLException {
        Optional<Ride> opt = rideDAO.findById(rideId);
        if (opt.isEmpty()) {
            throw new ValidationException("Ride not found.");
        }
        Ride ride = opt.get();
        if (ride.getDriverId() != currentStudent.getStudentId()) {
            throw new ValidationException("Authorization error: You can only cancel your own rides.");
        }
        if ("CANCELLED".equalsIgnoreCase(ride.getStatus())) {
            throw new ValidationException("This ride has already been cancelled.");
        }
        if ("COMPLETED".equalsIgnoreCase(ride.getStatus())) {
            throw new ValidationException("Completed rides cannot be cancelled.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock row
                rideDAO.lockRideForUpdate(conn, rideId);

                // Update ride status
                rideDAO.updateSeatsAndStatus(conn, rideId, 0, "CANCELLED");

                // Cancel all active bookings
                bookingDAO.cancelAllConfirmedForRide(conn, rideId);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Failed to cancel ride: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Completes an eligible ride, calculating final fare share allocations
     * and saving student savings snapshots in fare_records.
     */
    public void completeRide(int rideId, Student currentStudent) throws ValidationException, SQLException {
        Optional<Ride> opt = rideDAO.findById(rideId);
        if (opt.isEmpty()) {
            throw new ValidationException("Ride not found.");
        }
        Ride ride = opt.get();
        if (ride.getDriverId() != currentStudent.getStudentId()) {
            throw new ValidationException("Authorization error: Only the ride creator can complete this ride.");
        }
        if (!"OPEN".equalsIgnoreCase(ride.getStatus()) && !"FULL".equalsIgnoreCase(ride.getStatus())) {
            throw new ValidationException("Only OPEN or FULL rides can be marked as completed.");
        }

        List<Booking> bookings = bookingDAO.findByRideId(rideId);
        List<Integer> passengerIds = bookings.stream().map(Booking::getPassengerId).toList();

        int totalParticipants = 1 + passengerIds.size();
        BigDecimal sharedFarePerPerson = fareService.calculateShare(ride.getTotalFare(), totalParticipants);
        BigDecimal soloBaseline = fareService.calculateSoloBaseline(ride.getVehicleType(), ride.getOriginId(), ride.getDestinationId());

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock ride
                rideDAO.lockRideForUpdate(conn, rideId);

                // Update status to COMPLETED
                rideDAO.updateSeatsAndStatus(conn, rideId, ride.getAvailableSeats(), "COMPLETED");

                // Record fare shares and individual savings
                reportDAO.recordTripFares(conn, rideId, ride.getDriverId(), passengerIds, soloBaseline, sharedFarePerPerson);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Failed to complete ride: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
