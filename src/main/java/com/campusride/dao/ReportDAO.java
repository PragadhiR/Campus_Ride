package com.campusride.dao;

import com.campusride.db.DBConnection;
import com.campusride.model.FareRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

/**
 * Data Access Object for DBMS Aggregate queries, Holiday Rush analytics,
 * and Student Savings calculations.
 */
public class ReportDAO {

    /**
     * DTO for Holiday Rush popular destination statistics.
     */
    public record PopularDestinationStat(
        int destinationId,
        String destinationName,
        String category,
        int totalRides,
        int availableSeats,
        int completedRides
    ) {}

    /**
     * DTO for Holiday Rush busiest departure hours.
     */
    public record TimeSlotStat(
        int hourOfDay,
        String displayTime,
        int rideCount
    ) {}

    /**
     * DTO for Holiday Rush vehicle distribution.
     */
    public record VehicleBreakdownStat(
        String vehicleType,
        int rideCount,
        BigDecimal averageFare
    ) {}

    /**
     * DTO for student individual savings summary.
     */
    public record StudentSavingsSummary(
        int completedRidesCount,
        BigDecimal totalSavings,
        BigDecimal averageSharePaid,
        String topDestination,
        List<FareRecordDetail> history
    ) {}

    public record FareRecordDetail(
        int rideId,
        String route,
        String travelDate,
        String vehicleType,
        String role,
        BigDecimal soloFare,
        BigDecimal sharedFare,
        BigDecimal savings
    ) {}

    /**
     * Holiday Rush: Aggregates destination popularity with COUNT(), GROUP BY, and ORDER BY.
     */
    public List<PopularDestinationStat> getPopularDestinations() throws SQLException {
        List<PopularDestinationStat> list = new ArrayList<>();
        String sql = "SELECT d.destination_id, d.destination_name, d.category, " +
                     "       COUNT(r.ride_id) AS total_rides, " +
                     "       COALESCE(SUM(CASE WHEN r.status = 'OPEN' THEN r.available_seats ELSE 0 END), 0) AS open_seats, " +
                     "       COALESCE(SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_rides " +
                     "FROM destinations d " +
                     "LEFT JOIN rides r ON d.destination_id = r.destination_id AND r.status IN ('OPEN', 'FULL', 'COMPLETED') " +
                     "GROUP BY d.destination_id, d.destination_name, d.category " +
                     "ORDER BY total_rides DESC, open_seats DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PopularDestinationStat(
                    rs.getInt("destination_id"),
                    rs.getString("destination_name"),
                    rs.getString("category"),
                    rs.getInt("total_rides"),
                    rs.getInt("open_seats"),
                    rs.getInt("completed_rides")
                ));
            }
        }
        return list;
    }

    /**
     * Holiday Rush: Busiest departure hours using HOUR(departure_time), COUNT(), GROUP BY.
     */
    public List<TimeSlotStat> getBusiestTimeSlots() throws SQLException {
        List<TimeSlotStat> list = new ArrayList<>();
        String sql = "SELECT HOUR(departure_time) AS dep_hour, COUNT(*) AS ride_count " +
                     "FROM rides " +
                     "WHERE status IN ('OPEN', 'FULL', 'COMPLETED') " +
                     "GROUP BY dep_hour " +
                     "ORDER BY ride_count DESC, dep_hour ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int hour = rs.getInt("dep_hour");
                String display = formatHour(hour);
                list.add(new TimeSlotStat(hour, display, rs.getInt("ride_count")));
            }
        }
        return list;
    }

    /**
     * Holiday Rush: Vehicle distribution using vehicle_type, COUNT(), AVG(total_fare).
     */
    public List<VehicleBreakdownStat> getVehicleBreakdown() throws SQLException {
        List<VehicleBreakdownStat> list = new ArrayList<>();
        String sql = "SELECT vehicle_type, COUNT(*) AS ride_count, AVG(total_fare) AS avg_fare " +
                     "FROM rides " +
                     "WHERE status IN ('OPEN', 'FULL', 'COMPLETED') " +
                     "GROUP BY vehicle_type " +
                     "ORDER BY ride_count DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal avgFare = rs.getBigDecimal("avg_fare");
                if (avgFare == null) avgFare = BigDecimal.ZERO;
                list.add(new VehicleBreakdownStat(
                    rs.getString("vehicle_type"),
                    rs.getInt("ride_count"),
                    avgFare.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }
        return list;
    }

    /**
     * Retrieves genuine completed trip savings statistics for a student.
     */
    public StudentSavingsSummary getStudentSavingsSummary(int studentId) throws SQLException {
        int completedCount = 0;
        BigDecimal totalSavings = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        String topDestination = "None";
        List<FareRecordDetail> history = new ArrayList<>();

        // 1. Aggregates from fare_records
        String aggSql = "SELECT COUNT(*) AS total_trips, " +
                        "       COALESCE(SUM(individual_savings), 0) AS total_savings, " +
                        "       COALESCE(AVG(shared_fare_paid), 0) AS avg_paid " +
                        "FROM fare_records WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(aggSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    completedCount = rs.getInt("total_trips");
                    totalSavings = rs.getBigDecimal("total_savings").setScale(2, RoundingMode.HALF_UP);
                    totalPaid = rs.getBigDecimal("avg_paid").setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        // 2. Favorite destination for student
        String topDestSql = "SELECT d.destination_name, COUNT(*) AS visit_count " +
                            "FROM fare_records fr " +
                            "JOIN rides r ON fr.ride_id = r.ride_id " +
                            "JOIN destinations d ON r.destination_id = d.destination_id " +
                            "WHERE fr.student_id = ? " +
                            "GROUP BY d.destination_id, d.destination_name " +
                            "ORDER BY visit_count DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(topDestSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    topDestination = rs.getString("destination_name");
                }
            }
        }

        // 3. Detailed history of completed trips
        String histSql = "SELECT fr.*, r.vehicle_type, r.departure_time, " +
                         "       orig.destination_name AS orig_name, dest.destination_name AS dest_name " +
                         "FROM fare_records fr " +
                         "JOIN rides r ON fr.ride_id = r.ride_id " +
                         "JOIN destinations orig ON r.origin_id = orig.destination_id " +
                         "JOIN destinations dest ON r.destination_id = dest.destination_id " +
                         "WHERE fr.student_id = ? " +
                         "ORDER BY fr.recorded_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(histSql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("departure_time");
                    String dateStr = ts != null ? ts.toLocalDateTime().toLocalDate().toString() : "N/A";
                    String route = rs.getString("orig_name") + " -> " + rs.getString("dest_name");
                    history.add(new FareRecordDetail(
                        rs.getInt("ride_id"),
                        route,
                        dateStr,
                        rs.getString("vehicle_type"),
                        rs.getString("student_role"),
                        rs.getBigDecimal("solo_baseline_fare"),
                        rs.getBigDecimal("shared_fare_paid"),
                        rs.getBigDecimal("individual_savings")
                    ));
                }
            }
        }

        return new StudentSavingsSummary(completedCount, totalSavings, totalPaid, topDestination, history);
    }

    /**
     * Records fare shares and savings in fare_records inside an active completion transaction.
     */
    public void recordTripFares(Connection conn, int rideId, int driverId,
                                List<Integer> passengerIds,
                                BigDecimal soloBaselineFare,
                                BigDecimal sharedFarePerPerson) throws SQLException {
        BigDecimal savingsPerPerson = soloBaselineFare.subtract(sharedFarePerPerson);
        if (savingsPerPerson.compareTo(BigDecimal.ZERO) < 0) {
            savingsPerPerson = BigDecimal.ZERO;
        }

        String insertSql = "INSERT INTO fare_records (ride_id, student_id, student_role, " +
                           "solo_baseline_fare, shared_fare_paid, individual_savings) " +
                           "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            // Driver record
            ps.setInt(1, rideId);
            ps.setInt(2, driverId);
            ps.setString(3, "DRIVER");
            ps.setBigDecimal(4, soloBaselineFare);
            ps.setBigDecimal(5, sharedFarePerPerson);
            ps.setBigDecimal(6, savingsPerPerson);
            ps.executeUpdate();

            // Passenger records
            for (int pId : passengerIds) {
                ps.setInt(1, rideId);
                ps.setInt(2, pId);
                ps.setString(3, "PASSENGER");
                ps.setBigDecimal(4, soloBaselineFare);
                ps.setBigDecimal(5, sharedFarePerPerson);
                ps.setBigDecimal(6, savingsPerPerson);
                ps.executeUpdate();
            }
        }

        // Update snapshot on confirmed bookings
        String updateBookingSql = "UPDATE bookings SET fare_share_snapshot = ? WHERE ride_id = ? AND booking_status = 'CONFIRMED'";
        try (PreparedStatement ps = conn.prepareStatement(updateBookingSql)) {
            ps.setBigDecimal(1, sharedFarePerPerson);
            ps.setInt(2, rideId);
            ps.executeUpdate();
        }
    }

    private static String formatHour(int hour) {
        if (hour == 0) return "12 AM - 1 AM";
        if (hour < 12) return hour + " AM - " + (hour + 1) + " AM";
        if (hour == 12) return "12 PM - 1 PM";
        return (hour - 12) + " PM - " + (hour - 11) + " PM";
    }
}
