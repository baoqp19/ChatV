package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message persistence using MySQL (via {@link DBUtil}).
 */
public final class MessageDAO {

    private static final Logger LOGGER = Logger.getLogger(MessageDAO.class.getName());
    private static final String TABLE = "messages_chat";
    private static volatile boolean schemaReady = false;

    private MessageDAO() {
        // Utility class - prevent instantiation
    }

    public record ChatMessage(String sender, String receiver, String content, Timestamp createdAt) {
    }

    /**
     * Saves a message to MySQL.
     */
    public static void saveMessage(String sender, String receiver, String content) {
        ensureSchema();

        String sql = "INSERT INTO " + TABLE + "(sender, receiver, content) VALUES (?, ?, ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save message to MySQL", e);
        }
    }

    /**
     * Loads the latest conversation messages between two users (ascending by time).
     *
     * @param userA first user
     * @param userB second user
     * @param limit max messages to return
     * @return list ordered by created_at asc
     */
    public static List<ChatMessage> getConversation(String userA, String userB, int limit) {
        ensureSchema();

        String sql = "SELECT sender, receiver, content, created_at " +
                "FROM " + TABLE + " " +
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                "ORDER BY created_at DESC LIMIT ?";

        List<ChatMessage> messages = new ArrayList<>();
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userA);
            ps.setString(2, userB);
            ps.setString(3, userB);
            ps.setString(4, userA);
            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new ChatMessage(
                            rs.getString("sender"),
                            rs.getString("receiver"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at")));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load conversation from MySQL", e);
            return messages;
        }

        // Reverse to ascending order (oldest first)
        java.util.Collections.reverse(messages);
        return messages;
    }

    /**
     * Ensures the message table exists with the expected schema (varchar
     * sender/receiver).
     */
    private static void ensureSchema() {
        if (schemaReady) {
            return;
        }

        synchronized (MessageDAO.class) {
            if (schemaReady) {
                return;
            }

            String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "sender VARCHAR(255) NOT NULL," +
                    "receiver VARCHAR(255) NOT NULL," +
                    "content TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_sender_receiver (sender, receiver)," +
                    "INDEX idx_created_at (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

            try (Connection con = DBUtil.getConnection(); Statement st = con.createStatement()) {
                st.executeUpdate(ddl);
                schemaReady = true;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to ensure message table schema", e);
            }
        }
    }
}
