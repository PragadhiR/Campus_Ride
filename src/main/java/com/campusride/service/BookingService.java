package com.campusride.service;

import com.campusride.dao.BookingDAO;
import com.campusride.dao.RideDAO;
import com.campusride.db.DBConnection;
import com.campusride.model.Booking;
import com.campusride.model.Ride;
import com.campusride.model.Student;
import com.campusride.util.BookingException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service managing transactional ride reservations and cancellations.
 * Guarantees ACID transaction semantics and row-level locking to prevent race conditions.
 */
public class BookingService {
    private final BookingDAO bookingDAO;
    private final RideDAO rideDAO;
    private final FareService fareService;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.rideDAO = new RideDAO();
        this.fareService = new FareService();
    }

    public BookingService(BookingDAO bookingDAO, RideDAO rideDAO, FareService fareService) {
        this.bookingDAO = bookingDAO;
        this.rideDAO = rideDAO;
        this.fareService = fareService;
    }

    /**
     * Transaction-safe Ride Reservation Workflow.
     * Prevents overbooking race conditions using SELECT ... FOR UPDATE.
     */
    public Booking joinRide(int rideId, Student passenger) throws BookingException, SQLException {
        if (passenger == null) {
            throw new BookingException("Authentication required. Please log in.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Row-level pessimistic locking
                Optional<Ride> lockedRideOpt = rideDAO.lockRideForUpdate(conn, rideId);
                if (lockedRideOpt.isEmpty()) {
                    throw new BookingException("The selected ride could not be found.");
                }
                Ride ride = lockedRideOpt.get();

                // 2. Validate current student is not the ride creator
                if (ride.getDriverId() == passenger.getStudentId()) {
                    throw new BookingException("Self-booking not allowed: You cannot join a ride you created.");
                }

                // 3. Verify ride status is OPEN
                if (!"OPEN".equalsIgnoreCase(ride.getStatus())) {
                    if ("FULL".equalsIgnoreCase(ride.getStatus())) {
                        throw new BookingException("This ride is already full. No seats are remaining.");
                    } else if ("CANCELLED".equalsIgnoreCase(ride.getStatus())) {
                        throw new BookingException("This ride has been cancelled by the creator.");
                    } else if ("COMPLETED".equalsIgnoreCase(ride.getStatus())) {
                        throw new BookingException("This ride has already been completed.");
                    } else {
                        throw new BookingException("This ride is not open for new bookings.");
                    }
                }

                // 4. Verify seat availability
                if (ride.getAvailableSeats() <= 0) {
                    throw new BookingException("No seats available on this ride.");
                }

                // 5. Verify ride restrictions (Women-only & Same Department)
                if (ride.isWomenOnly() && !passenger.isFemale()) {
                    throw new BookingException("Restriction: This ride is designated for women students only.");
                }
                if (ride.isSameDepartmentOnly() &&
                    !ride.getDriverDepartment().equalsIgnoreCase(passenger.getDepartment())) {
                    throw new BookingException("Restriction: This ride is reserved for " +
                            ride.getDriverDepartment() + " department students only.");
                }

                // 6. Check for duplicate booking
                Optional<Booking> existingBookingOpt = bookingDAO.findByRideAndPassenger(conn, rideId, passenger.getStudentId());
                if (existingBookingOpt.isPresent()) {
                    Booking existing = existingBookingOpt.get();
                    if ("CONFIRMED".equalsIgnoreCase(existing.getBookingStatus())) {
                        throw new BookingException("You have already booked a seat on this ride.");
                    }
                }

                // 7. Calculate fare share snapshot with this new passenger
                int currentConfirmed = rideDAO.getConfirmedPassengerCount(rideId);
                int newTotalParticipants = 1 + currentConfirmed + 1;
                BigDecimal fareSnapshot = fareService.calculateShare(ride.getTotalFare(), newTotalParticipants);

                // 8. Insert new booking or reactivate previously cancelled booking
                Booking booking;
                if (existingBookingOpt.isPresent()) {
                    booking = existingBookingOpt.get();
                    bookingDAO.reactivateBooking(conn, rideId, passenger.getStudentId(), fareSnapshot);
                    booking.setBookingStatus("CONFIRMED");
                    booking.setFareShareSnapshot(fareSnapshot);
                } else {
                    booking = new Booking(0, rideId, passenger.getStudentId(), null, "CONFIRMED", fareSnapshot);
                    booking = bookingDAO.create(conn, booking);
                }

                // 9. Decrement available seats and adjust status atomically
                int newAvailableSeats = ride.getAvailableSeats() - 1;
                String newStatus = (newAvailableSeats == 0) ? "FULL" : "OPEN";
                rideDAO.updateSeatsAndStatus(conn, rideId, newAvailableSeats, newStatus);

                // 10. Commit the transaction
                conn.commit();
                return booking;

            } catch (BookingException be) {
                conn.rollback();
                throw be;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Transaction error while booking ride: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Transaction-safe booking cancellation by a passenger.
     * Restores the seat and reopens the ride if it was FULL.
     */
    public void cancelBooking(int bookingId, Student passenger) throws BookingException, SQLException {
        if (passenger == null) {
            throw new BookingException("Authentication required.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Booking> opt = bookingDAO.findById(bookingId);
                if (opt.isEmpty()) {
                    throw new BookingException("Booking record not found.");
                }
                Booking booking = opt.get();

                if (booking.getPassengerId() != passenger.getStudentId()) {
                    throw new BookingException("Authorization error: You can only cancel your own bookings.");
                }
                if (!"CONFIRMED".equalsIgnoreCase(booking.getBookingStatus())) {
                    throw new BookingException("This booking is already cancelled.");
                }

                // Lock ride
                Optional<Ride> rideOpt = rideDAO.lockRideForUpdate(conn, booking.getRideId());
                if (rideOpt.isEmpty()) {
                    throw new BookingException("Associated ride not found.");
                }
                Ride ride = rideOpt.get();

                if ("COMPLETED".equalsIgnoreCase(ride.getStatus())) {
                    throw new BookingException("Cannot cancel booking for a completed ride.");
                }

                // Update booking status
                bookingDAO.updateStatus(conn, bookingId, "CANCELLED");

                // Restore seat if ride is not cancelled
                if (!"CANCELLED".equalsIgnoreCase(ride.getStatus())) {
                    int restoredSeats = Math.min(ride.getPassengerCapacity(), ride.getAvailableSeats() + 1);
                    String restoredStatus = "OPEN";
                    rideDAO.updateSeatsAndStatus(conn, ride.getRideId(), restoredSeats, restoredStatus);
                }

                conn.commit();
            } catch (BookingException be) {
                conn.rollback();
                throw be;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Failed to cancel booking: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Booking> getBookingsByPassenger(int passengerId) throws SQLException {
        return bookingDAO.findByPassengerId(passengerId);
    }

    public List<Booking> getBookingsForRide(int rideId) throws SQLException {
        return bookingDAO.findByRideId(rideId);
    }
}
