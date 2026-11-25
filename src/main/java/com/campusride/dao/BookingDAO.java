package com.campusride.dao;

import com.campusride.db.DBConnection;
import com.campusride.model.Booking;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for passenger Bookings.
 * Operates inside transactions when modifying bookings and seat counts.
 */
public class BookingDAO {

    public Booking create(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (ride_id, passenger_id, booking_status, fare_share_snapshot) " +
                     "VALUES (?, ?, 'CONFIRMED', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getRideId());
            ps.setInt(2, booking.getPassengerId());
            ps.setBigDecimal(3, booking.getFareShareSnapshot());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    booking.setBookingId(rs.getInt(1));
                }
            }
        }
        return booking;
    }

    public void reactivateBooking(Connection conn, int rideId, int passengerId, BigDecimal fareSnapshot) throws SQLException {
        String sql = "UPDATE bookings SET booking_status = 'CONFIRMED', fare_share_snapshot = ?, booking_time = CURRENT_TIMESTAMP " +
                     "WHERE ride_id = ? AND passenger_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, fareSnapshot);
            ps.setInt(2, rideId);
            ps.setInt(3, passengerId);
            ps.executeUpdate();
        }
    }

    public Optional<Booking> findByRideAndPassenger(Connection conn, int rideId, int passengerId) throws SQLException {
        String sql = "SELECT * FROM bookings WHERE ride_id = ? AND passenger_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            ps.setInt(2, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Booking> findById(int bookingId) throws SQLException {
        String sql = "SELECT b.*, s.full_name AS passenger_name, s.department AS passenger_department, s.gender AS passenger_gender, " +
                     "r.vehicle_type, r.total_fare, r.departure_time, r.status AS ride_status, " +
                     "orig.destination_name AS origin_name, dest.destination_name AS destination_name, " +
                     "drv.full_name AS driver_name " +
                     "FROM bookings b " +
                     "JOIN students s ON b.passenger_id = s.student_id " +
                     "JOIN rides r ON b.ride_id = r.ride_id " +
                     "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                     "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                     "JOIN students drv ON r.driver_id = drv.student_id " +
                     "WHERE b.booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDetailedRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Booking> findByPassengerId(int passengerId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, s.full_name AS passenger_name, s.department AS passenger_department, s.gender AS passenger_gender, " +
                     "r.vehicle_type, r.total_fare, r.departure_time, r.status AS ride_status, " +
                     "orig.destination_name AS origin_name, dest.destination_name AS destination_name, " +
                     "drv.full_name AS driver_name " +
                     "FROM bookings b " +
                     "JOIN students s ON b.passenger_id = s.student_id " +
                     "JOIN rides r ON b.ride_id = r.ride_id " +
                     "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                     "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                     "JOIN students drv ON r.driver_id = drv.student_id " +
                     "WHERE b.passenger_id = ? " +
                     "ORDER BY b.booking_time DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDetailedRow(rs));
                }
            }
        }
        return list;
    }

    public List<Booking> findByRideId(int rideId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.*, s.full_name AS passenger_name, s.department AS passenger_department, s.gender AS passenger_gender " +
                     "FROM bookings b " +
                     "JOIN students s ON b.passenger_id = s.student_id " +
                     "WHERE b.ride_id = ? AND b.booking_status = 'CONFIRMED' " +
                     "ORDER BY b.booking_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking b = mapRow(rs);
                    b.setPassengerName(rs.getString("passenger_name"));
                    b.setPassengerDepartment(rs.getString("passenger_department"));
                    b.setPassengerGender(rs.getString("passenger_gender"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public void updateStatus(Connection conn, int bookingId, String newStatus) throws SQLException {
        String sql = "UPDATE bookings SET booking_status = ? WHERE booking_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    public void cancelAllConfirmedForRide(Connection conn, int rideId) throws SQLException {
        String sql = "UPDATE bookings SET booking_status = 'CANCELLED' WHERE ride_id = ? AND booking_status = 'CONFIRMED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            ps.executeUpdate();
        }
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("booking_id"));
        b.setRideId(rs.getInt("ride_id"));
        b.setPassengerId(rs.getInt("passenger_id"));
        Timestamp ts = rs.getTimestamp("booking_time");
        if (ts != null) b.setBookingTime(ts.toLocalDateTime());
        b.setBookingStatus(rs.getString("booking_status"));
        b.setFareShareSnapshot(rs.getBigDecimal("fare_share_snapshot"));
        return b;
    }

    private Booking mapDetailedRow(ResultSet rs) throws SQLException {
        Booking b = mapRow(rs);
        b.setPassengerName(rs.getString("passenger_name"));
        b.setPassengerDepartment(rs.getString("passenger_department"));
        b.setPassengerGender(rs.getString("passenger_gender"));
        b.setVehicleType(rs.getString("vehicle_type"));
        b.setTotalFare(rs.getBigDecimal("total_fare"));
        Timestamp dep = rs.getTimestamp("departure_time");
        if (dep != null) b.setDepartureTime(dep.toLocalDateTime());
        b.setRideStatus(rs.getString("ride_status"));
        b.setOriginName(rs.getString("origin_name"));
        b.setDestinationName(rs.getString("destination_name"));
        b.setDriverName(rs.getString("driver_name"));
        return b;
    }
}
