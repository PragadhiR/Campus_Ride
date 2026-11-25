package com.campusride.dao;

import com.campusride.db.DBConnection;
import com.campusride.model.*;
import com.campusride.util.DateUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Rides.
 * Handles ride creation, search with smart 30-minute time window matching,
 * row-level locking for transactions, and status updates.
 */
public class RideDAO {

    public Ride create(Ride ride) throws SQLException {
        String sql = "INSERT INTO rides (driver_id, origin_id, destination_id, departure_time, " +
                     "vehicle_type, total_fare, passenger_capacity, available_seats, " +
                     "women_only, same_department_only, notes, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ride.getDriverId());
            ps.setInt(2, ride.getOriginId());
            ps.setInt(3, ride.getDestinationId());
            ps.setTimestamp(4, Timestamp.valueOf(ride.getDepartureTime()));
            ps.setString(5, ride.getVehicleType());
            ps.setBigDecimal(6, ride.getTotalFare());
            ps.setInt(7, ride.getPassengerCapacity());
            ps.setInt(8, ride.getAvailableSeats());
            ps.setBoolean(9, ride.isWomenOnly());
            ps.setBoolean(10, ride.isSameDepartmentOnly());
            ps.setString(11, ride.getNotes());
            ps.setString(12, ride.getStatus());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating ride failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    ride.setRideId(rs.getInt(1));
                }
            }
            return ride;
        }
    }

    public Optional<Ride> findById(int rideId) throws SQLException {
        String sql = "SELECT r.*, orig.destination_name AS origin_name, dest.destination_name AS destination_name, " +
                     "drv.full_name AS driver_name, drv.department AS driver_department, drv.gender AS driver_gender " +
                     "FROM rides r " +
                     "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                     "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                     "JOIN students drv ON r.driver_id = drv.student_id " +
                     "WHERE r.ride_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Ride> findByDriverId(int driverId) throws SQLException {
        List<Ride> list = new ArrayList<>();
        String sql = "SELECT r.*, orig.destination_name AS origin_name, dest.destination_name AS destination_name, " +
                     "drv.full_name AS driver_name, drv.department AS driver_department, drv.gender AS driver_gender " +
                     "FROM rides r " +
                     "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                     "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                     "JOIN students drv ON r.driver_id = drv.student_id " +
                     "WHERE r.driver_id = ? " +
                     "ORDER BY r.departure_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    /**
     * Executes the smart 30-minute time-window matching query.
     * Matches rides on the same route whose departure time is between
     * [requestedTime - 30 MINUTE] and [requestedTime + 30 MINUTE],
     * available seats > 0, status = 'OPEN', and driver != student.
     */
    public List<RideMatch> searchSmartMatches(int originId, int destId,
                                              LocalDateTime requestedTime,
                                              Student student,
                                              double distanceKm) throws SQLException {
        List<RideMatch> matches = new ArrayList<>();
        String sql = "SELECT r.*, " +
                     "orig.destination_name AS origin_name, " +
                     "dest.destination_name AS destination_name, " +
                     "drv.full_name AS driver_name, " +
                     "drv.department AS driver_department, " +
                     "drv.gender AS driver_gender, " +
                     "(SELECT COUNT(*) FROM bookings b WHERE b.ride_id = r.ride_id AND b.booking_status = 'CONFIRMED') AS confirmed_passengers " +
                     "FROM rides r " +
                     "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                     "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                     "JOIN students drv ON r.driver_id = drv.student_id " +
                     "WHERE r.origin_id = ? " +
                     "  AND r.destination_id = ? " +
                     "  AND r.departure_time BETWEEN DATE_SUB(?, INTERVAL 30 MINUTE) AND DATE_ADD(?, INTERVAL 30 MINUTE) " +
                     "  AND r.status = 'OPEN' " +
                     "  AND r.available_seats > 0 " +
                     "  AND r.driver_id <> ? " +
                     "ORDER BY ABS(TIMESTAMPDIFF(MINUTE, r.departure_time, ?)) ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp ts = Timestamp.valueOf(requestedTime);
            ps.setInt(1, originId);
            ps.setInt(2, destId);
            ps.setTimestamp(3, ts);
            ps.setTimestamp(4, ts);
            ps.setInt(5, student.getStudentId());
            ps.setTimestamp(6, ts);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ride ride = mapResultSet(rs);
                    int confirmedPassengers = rs.getInt("confirmed_passengers");

                    // Filter 1: Women-only ride restriction
                    if (ride.isWomenOnly() && !student.isFemale()) {
                        continue; // Hidden from or restricted for non-female students
                    }

                    // Filter 2: Same-department restriction
                    if (ride.isSameDepartmentOnly() &&
                        !ride.getDriverDepartment().equalsIgnoreCase(student.getDepartment())) {
                        continue;
                    }

                    // Explain the match reason (e.g. "Exact time match · 2 seats left" or "15 mins earlier · 2 seats left")
                    String timeReason = DateUtil.getRelativeTimeMatchReason(requestedTime, ride.getDepartureTime());
                    String matchReason = String.format("Same route · %s · %d seat(s) available",
                            timeReason, ride.getAvailableSeats());

                    matches.add(new RideMatch(ride, matchReason, distanceKm, confirmedPassengers));
                }
            }
        }
        return matches;
    }

    /**
     * Locks the ride row using SELECT ... FOR UPDATE inside a transaction.
     */
    public Optional<Ride> lockRideForUpdate(Connection conn, int rideId) throws SQLException {
        String sql = "SELECT r.*, orig.destination_name AS origin_name, dest.destination_name AS destination_name, " +
                     "drv.full_name AS driver_name, drv.department AS driver_department, drv.gender AS driver_gender " +
                     "FROM rides r " +
                     "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                     "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                     "JOIN students drv ON r.driver_id = drv.student_id " +
                     "WHERE r.ride_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Updates seat availability and status inside an existing transaction connection.
     */
    public void updateSeatsAndStatus(Connection conn, int rideId, int newSeats, String newStatus) throws SQLException {
        String sql = "UPDATE rides SET available_seats = ?, status = ? WHERE ride_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newSeats);
            ps.setString(2, newStatus);
            ps.setInt(3, rideId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(int rideId, String newStatus) throws SQLException {
        String sql = "UPDATE rides SET status = ? WHERE ride_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, rideId);
            ps.executeUpdate();
        }
    }

    public int getConfirmedPassengerCount(int rideId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE ride_id = ? AND booking_status = 'CONFIRMED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Ride mapResultSet(ResultSet rs) throws SQLException {
        String vehicleType = rs.getString("vehicle_type");
        Ride ride = Ride.create(vehicleType);

        ride.setRideId(rs.getInt("ride_id"));
        ride.setDriverId(rs.getInt("driver_id"));
        ride.setOriginId(rs.getInt("origin_id"));
        ride.setDestinationId(rs.getInt("destination_id"));

        Timestamp depTs = rs.getTimestamp("departure_time");
        if (depTs != null) {
            ride.setDepartureTime(depTs.toLocalDateTime());
        }

        ride.setTotalFare(rs.getBigDecimal("total_fare"));
        ride.setPassengerCapacity(rs.getInt("passenger_capacity"));
        ride.setAvailableSeats(rs.getInt("available_seats"));
        ride.setWomenOnly(rs.getBoolean("women_only"));
        ride.setSameDepartmentOnly(rs.getBoolean("same_department_only"));
        ride.setNotes(rs.getString("notes"));
        ride.setStatus(rs.getString("status"));

        Timestamp crTs = rs.getTimestamp("created_at");
        if (crTs != null) ride.setCreatedAt(crTs.toLocalDateTime());
        Timestamp upTs = rs.getTimestamp("updated_at");
        if (upTs != null) ride.setUpdatedAt(upTs.toLocalDateTime());

        // Contextual joined columns if present
        try {
            ride.setOriginName(rs.getString("origin_name"));
            ride.setDestinationName(rs.getString("destination_name"));
            ride.setDriverName(rs.getString("driver_name"));
            ride.setDriverDepartment(rs.getString("driver_department"));
            ride.setDriverGender(rs.getString("driver_gender"));
        } catch (SQLException ignored) {}

        return ride;
    }
}
