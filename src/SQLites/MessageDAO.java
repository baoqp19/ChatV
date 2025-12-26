package SQLites;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Data Access Object for message storage in SQLite.
 * Java 21 compatible with improved error handling.
 */
public final class MessageDAO {

    private static final Logger LOGGER = Logger.getLogger(MessageDAO.class.getName());

    private MessageDAO() {
        // Utility class - prevent instantiation
    }

    /**
     * Saves a message to the database
     * 
     * @param sender   Sender username
     * @param receiver Receiver username
     * @param content  Message content
     */
    public static void save(String sender, String receiver, String content) {
        String sql = "INSERT INTO messages(sender, receiver, content) VALUES(?,?,?)";

        try (Connection con = SQLiteUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();

            LOGGER.log(Level.INFO, "Message saved: " + sender + " → " + receiver);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save message", e);
        }
    }

    /**
     * Deletes old messages (older than specified days)
     * 
     * @param days Number of days to keep
     * @return true if deletion successful
     */
    public static boolean deleteOldMessages(int days) {
        String sql = "DELETE FROM messages WHERE time < datetime('now', '-' || ? || ' days')";

        try (Connection con = SQLiteUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, days);
            ps.executeUpdate();
            LOGGER.log(Level.INFO, "Old messages deleted (older than " + days + " days)");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete old messages", e);
            return false;
        }
    }
}
