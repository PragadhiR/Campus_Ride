package com.campusride.dao;

import com.campusride.db.DBConnection;
import com.campusride.model.Destination;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Destination and Distance queries.
 */
public class DestinationDAO {

    public List<Destination> getAllActive() throws SQLException {
        List<Destination> list = new ArrayList<>();
        String sql = "SELECT * FROM destinations WHERE active = TRUE ORDER BY destination_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Destination(
                    rs.getInt("destination_id"),
                    rs.getString("destination_name"),
                    rs.getString("category"),
                    rs.getBoolean("active")
                ));
            }
        }
        return list;
    }

    public Optional<Destination> findById(int id) throws SQLException {
        String sql = "SELECT * FROM destinations WHERE destination_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Destination(
                        rs.getInt("destination_id"),
                        rs.getString("destination_name"),
                        rs.getString("category"),
                        rs.getBoolean("active")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves distance between two destinations in kilometers from distance_matrix.
     * Returns default average 12.0 km if not explicitly configured.
     */
    public double getDistanceKm(int fromId, int toId) {
        if (fromId == toId) return 0.0;
        String sql = "SELECT distance_km FROM distance_matrix WHERE " +
                     "(from_destination_id = ? AND to_destination_id = ?) OR " +
                     "(from_destination_id = ? AND to_destination_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setInt(3, toId);
            ps.setInt(4, fromId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("distance_km");
                }
            }
        } catch (SQLException e) {
            System.err.println("Warning: Could not fetch distance: " + e.getMessage());
        }
        return 12.0; // fallback standard city distance
    }
}
