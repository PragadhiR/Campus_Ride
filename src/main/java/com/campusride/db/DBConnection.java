package com.campusride.db;

import com.campusride.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connection acquisition and validation.
 */
public class DBConnection {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Fatal: MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a new connection to the MySQL database.
     * Callers must close the connection using try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            AppConfig.getJdbcUrl(),
            AppConfig.getDbUser(),
            AppConfig.getDbPassword()
        );
    }

    /**
     * Tests current database credentials.
     * @return true if connection succeeded, false otherwise.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Tests connection with arbitrary host, port, database, user, password.
     */
    public static void validateCredentials(String host, int port, String dbName, String user, String password) throws SQLException {
        String testUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000",
            host, port, dbName
        );
        try (Connection conn = DriverManager.getConnection(testUrl, user, password)) {
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Failed to establish a valid connection.");
            }
        }
    }
}
