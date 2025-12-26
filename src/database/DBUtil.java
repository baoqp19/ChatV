package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database utility for MySQL connections in VKU Chat.
 * Java 21 compatible.
 * 
 * ⚠️ WARNING: Username and password are hardcoded.
 * For production, use environment variables or configuration files.
 */
public final class DBUtil {

    private DBUtil() {
        // Utility class - prevent instantiation
    }

    private static final String URL = "jdbc:mysql://localhost:3306/p2p_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    private static final int CONNECTION_TIMEOUT = 10000;

    /**
     * Gets a database connection
     * 
     * @return Database connection
     * @throws SQLException If connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            DriverManager.setLoginTimeout(CONNECTION_TIMEOUT / 1000);
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Verifies database connectivity
     * 
     * @return true if connection successful
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}