package com.campusride;

import com.campusride.db.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Diagnostic runner to safely check MySQL connectivity
 * without logging or exposing any credentials.
 */
public class DBVerificationRunner {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("DATABASE_CONNECTION_STATUS: SUCCESS");
            } else {
                System.out.println("DATABASE_CONNECTION_STATUS: FAILED (Null or closed connection)");
            }
        } catch (SQLException e) {
            System.out.println("DATABASE_CONNECTION_STATUS: FAILED (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        } catch (Exception e) {
            System.out.println("DATABASE_CONNECTION_STATUS: ERROR (" + e.getMessage() + ")");
        }
    }
}
