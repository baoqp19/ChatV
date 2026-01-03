package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Data Access Object for user management in VKU Chat.
 * Java 21 compatible with improved error handling.
 */
public final class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    private UserDAO() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if a user exists in the database
     * 
     * @param username Username to check
     * @return true if user exists, false otherwise
     */
    public static boolean isUserExist(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection con = DBUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error checking user existence: " + username, e);
        }
        return false;
    }

    /**
     * Registers a new user
     * 
     * @param username Username
     * @param password Password (⚠️ should be hashed in production)
     * @param fullName Full name
     * @param email    Email address
     * @return true if registration successful
     */
    public static boolean register(String username, String password, String fullName, String email) {
        String sql = "INSERT INTO users(username, password, full_name, email) VALUES (?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // ⚠️ TODO: Hash password with bcrypt or similar
            ps.setString(3, fullName);
            ps.setString(4, email);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error registering user: " + username, e);
        }
        return false;
    }

    /**
     * Updates user's online status
     * 
     * @param username Username
     * @param status   New status
     * @return true if update successful
     */
    public static boolean updateUserStatus(String username, String status) {
        String sql = "UPDATE peers SET status = ?, last_seen = NOW() WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, username);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error updating user status: " + username, e);
        }
        return false;
    }

    /**
     * Retrieves all users from the database
     * 
     * @return List of all usernames in the database
     */
    public static List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users ORDER BY username";

        try (Connection con = DBUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("username"));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error retrieving all users", e);
        }
        return users;
    }

    /**
     * Verifies user password
     * 
     * @param username Username
     * @param password Password to verify
     * @return true if password is correct, false otherwise
     */
    public static boolean verifyPassword(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ? LIMIT 1";
        try (Connection con = DBUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    // Simple string comparison (consider using bcrypt in production)
                    return storedPassword != null && storedPassword.equals(password);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error verifying password for user: " + username, e);
        }
        return false;
    }
}
