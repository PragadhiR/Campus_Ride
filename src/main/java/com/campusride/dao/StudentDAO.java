package com.campusride.dao;

import com.campusride.db.DBConnection;
import com.campusride.model.Student;

import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for Student entity operations.
 * Uses JDBC PreparedStatements for SQL injection prevention.
 */
public class StudentDAO {

    public Student create(Student student) throws SQLException {
        String sql = "INSERT INTO students (full_name, email, password_hash, department, year_of_study, gender) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, student.getFullName());
            ps.setString(2, student.getEmail().toLowerCase().trim());
            ps.setString(3, student.getPasswordHash());
            ps.setString(4, student.getDepartment().trim());
            ps.setInt(5, student.getYearOfStudy());
            ps.setString(6, student.getGender());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating student failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    student.setStudentId(rs.getInt(1));
                }
            }
            return student;
        }
    }

    public Optional<Student> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM students WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Student> findById(int studentId) throws SQLException {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Student mapResultSet(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setPasswordHash(rs.getString("password_hash"));
        s.setDepartment(rs.getString("department"));
        s.setYearOfStudy(rs.getInt("year_of_study"));
        s.setGender(rs.getString("gender"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            s.setCreatedAt(ts.toLocalDateTime());
        }
        return s;
    }
}
