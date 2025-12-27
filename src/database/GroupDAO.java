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
 * Group chat persistence using MySQL.
 */
public final class GroupDAO {

    private static final Logger LOGGER = Logger.getLogger(GroupDAO.class.getName());
    private static final String GROUPS_TABLE = "chat_groups";
    private static final String MEMBERS_TABLE = "group_members";
    private static final String MESSAGES_TABLE = "group_messages";
    private static final String MESSAGE_STATUS_TABLE = "group_message_status";
    private static final String READ_POINTER_TABLE = "group_read_pointers";
    private static volatile boolean schemaReady = false;

    private GroupDAO() {
        // Utility class
    }

    public record GroupInfo(int groupId, String groupName, String creator, Timestamp createdAt) {
    }

    public record GroupMember(int groupId, String username, Timestamp joinedAt) {
    }

    public record GroupMessage(int messageId, int groupId, String sender, String content, Timestamp createdAt) {
    }

    /**
     * Creates the necessary tables for group chat if they don't exist.
     */
    private static void ensureSchema() {
        if (schemaReady)
            return;

        synchronized (GroupDAO.class) {
            if (schemaReady)
                return;

            try (Connection con = DBUtil.getConnection(); Statement stmt = con.createStatement()) {

                // Create groups table
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS " + GROUPS_TABLE + " (" +
                                "  group_id INT AUTO_INCREMENT PRIMARY KEY," +
                                "  group_name VARCHAR(100) NOT NULL," +
                                "  creator VARCHAR(50) NOT NULL," +
                                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ")");

                // Create group members table
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS " + MEMBERS_TABLE + " (" +
                                "  group_id INT NOT NULL," +
                                "  username VARCHAR(50) NOT NULL," +
                                "  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "  PRIMARY KEY (group_id, username)," +
                                "  FOREIGN KEY (group_id) REFERENCES " + GROUPS_TABLE + "(group_id) ON DELETE CASCADE" +
                                ")");

                // Create group messages table
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS " + MESSAGES_TABLE + " (" +
                                "  message_id INT AUTO_INCREMENT PRIMARY KEY," +
                                "  group_id INT NOT NULL," +
                                "  sender VARCHAR(50) NOT NULL," +
                                "  content TEXT NOT NULL," +
                                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "  FOREIGN KEY (group_id) REFERENCES " + GROUPS_TABLE + "(group_id) ON DELETE CASCADE" +
                                ")");

                        // Message status per user (sent/delivered/read)
                        stmt.execute(
                            "CREATE TABLE IF NOT EXISTS " + MESSAGE_STATUS_TABLE + " (" +
                                "  message_id INT NOT NULL," +
                                "  username VARCHAR(50) NOT NULL," +
                                "  status VARCHAR(20) NOT NULL," +
                                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                                "  PRIMARY KEY (message_id, username)," +
                                "  FOREIGN KEY (message_id) REFERENCES " + MESSAGES_TABLE + "(message_id) ON DELETE CASCADE" +
                                ")");

                        // Read pointers per user (latest read message)
                        stmt.execute(
                            "CREATE TABLE IF NOT EXISTS " + READ_POINTER_TABLE + " (" +
                                "  group_id INT NOT NULL," +
                                "  username VARCHAR(50) NOT NULL," +
                                "  last_read_message_id INT NOT NULL," +
                                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                                "  PRIMARY KEY (group_id, username)," +
                                "  FOREIGN KEY (group_id) REFERENCES " + GROUPS_TABLE + "(group_id) ON DELETE CASCADE" +
                                ")");

                schemaReady = true;
                LOGGER.info("Group chat schema verified/created successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create group chat schema", e);
            }
        }
    }

    /**
     * Creates a new group and adds the creator as the first member.
     * 
     * @return The new group ID, or -1 on error
     */
    public static int createGroup(String groupName, String creator) {
        ensureSchema();

        String insertGroup = "INSERT INTO " + GROUPS_TABLE + "(group_name, creator) VALUES (?, ?)";
        String insertMember = "INSERT INTO " + MEMBERS_TABLE + "(group_id, username) VALUES (?, ?)";

        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement psGroup = con.prepareStatement(insertGroup, Statement.RETURN_GENERATED_KEYS)) {
                psGroup.setString(1, groupName);
                psGroup.setString(2, creator);
                psGroup.executeUpdate();

                ResultSet rs = psGroup.getGeneratedKeys();
                if (rs.next()) {
                    int groupId = rs.getInt(1);

                    // Add creator as first member
                    try (PreparedStatement psMember = con.prepareStatement(insertMember)) {
                        psMember.setInt(1, groupId);
                        psMember.setString(2, creator);
                        psMember.executeUpdate();
                    }

                    con.commit();
                    LOGGER.info("Created group: " + groupName + " (ID: " + groupId + ")");
                    return groupId;
                }
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create group", e);
        }
        return -1;
    }

    /**
     * Adds a member to an existing group.
     */
    public static boolean addMember(int groupId, String username) {
        ensureSchema();

        String sql = "INSERT IGNORE INTO " + MEMBERS_TABLE + "(group_id, username) VALUES (?, ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to add member to group", e);
            return false;
        }
    }

    /**
     * Removes a member from a group.
     */
    public static boolean removeMember(int groupId, String username) {
        ensureSchema();

        String sql = "DELETE FROM " + MEMBERS_TABLE + " WHERE group_id = ? AND username = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to remove member from group", e);
            return false;
        }
    }

    /**
     * Retrieves all members of a group.
     */
    public static List<String> getGroupMembers(int groupId) {
        ensureSchema();

        List<String> members = new ArrayList<>();
        String sql = "SELECT username FROM " + MEMBERS_TABLE + " WHERE group_id = ? ORDER BY joined_at";

        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.add(rs.getString("username"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch group members", e);
        }
        return members;
    }

    /**
     * Retrieves all groups that a user is a member of.
     */
    public static List<GroupInfo> getUserGroups(String username) {
        ensureSchema();

        List<GroupInfo> groups = new ArrayList<>();
        String sql = "SELECT g.group_id, g.group_name, g.creator, g.created_at " +
                "FROM " + GROUPS_TABLE + " g " +
                "INNER JOIN " + MEMBERS_TABLE + " m ON g.group_id = m.group_id " +
                "WHERE m.username = ? ORDER BY g.created_at DESC";

        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                groups.add(new GroupInfo(
                        rs.getInt("group_id"),
                        rs.getString("group_name"),
                        rs.getString("creator"),
                        rs.getTimestamp("created_at")));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch user groups", e);
        }
        return groups;
    }

    /**
     * Saves a group message.
     */
    public static boolean saveGroupMessage(int groupId, String sender, String content) {
        ensureSchema();

        String sql = "INSERT INTO " + MESSAGES_TABLE + "(group_id, sender, content) VALUES (?, ?, ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setString(2, sender);
            ps.setString(3, content);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save group message", e);
            return false;
        }
    }

    /**
     * Saves a group message and returns the generated message ID.
     * 
     * @param groupId The group ID
     * @param sender  The sender username
     * @param content The message content
     * @return The generated message ID, or -1 on error
     */
    public static int saveGroupMessageAndGetId(int groupId, String sender, String content) {
        ensureSchema();

        String sql = "INSERT INTO " + MESSAGES_TABLE + "(group_id, sender, content) VALUES (?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, groupId);
            ps.setString(2, sender);
            ps.setString(3, content);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save group message", e);
        }
        return -1;
    }

    /**
     * Upserts a message status for a given user (sent, delivered, read).
     */
    public static void saveMessageStatus(int messageId, String username, String status) {
        ensureSchema();
        String sql = "INSERT INTO " + MESSAGE_STATUS_TABLE + " (message_id, username, status) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = CURRENT_TIMESTAMP";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setString(2, username);
            ps.setString(3, status);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save message status", e);
        }
    }

    /**
     * Retrieves message statuses for a set of message IDs for the given user.
     */
    public static java.util.Map<Integer, String> getMessageStatusesForUser(List<Integer> messageIds, String username) {
        ensureSchema();
        java.util.Map<Integer, String> result = new java.util.HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return result;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(messageIds.size(), "?"));
        String sql = "SELECT message_id, status FROM " + MESSAGE_STATUS_TABLE + " WHERE username = ? AND message_id IN (" + placeholders + ")";

        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            int idx = 2;
            for (Integer id : messageIds) {
                ps.setInt(idx++, id);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("message_id"), rs.getString("status"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load message statuses", e);
        }
        return result;
    }

    /**
     * Upserts read pointer for a user in a group.
     */
    public static void setReadPointer(int groupId, String username, int lastReadMessageId) {
        ensureSchema();
        String sql = "INSERT INTO " + READ_POINTER_TABLE + " (group_id, username, last_read_message_id) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_read_message_id = VALUES(last_read_message_id), updated_at = CURRENT_TIMESTAMP";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setString(2, username);
            ps.setInt(3, lastReadMessageId);
            ps.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update read pointer", e);
        }
    }

    /**
     * Gets read pointers for all members of a group.
     */
    public static java.util.Map<String, Integer> getReadPointers(int groupId) {
        ensureSchema();
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        String sql = "SELECT username, last_read_message_id FROM " + READ_POINTER_TABLE + " WHERE group_id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("username"), rs.getInt("last_read_message_id"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load read pointers", e);
        }
        return result;
    }

    /**
     * Loads group message history (latest 50 messages).
     */
    public static List<GroupMessage> loadGroupMessages(int groupId) {
        ensureSchema();

        List<GroupMessage> messages = new ArrayList<>();
        String sql = "SELECT message_id, group_id, sender, content, created_at " +
                "FROM " + MESSAGES_TABLE + " WHERE group_id = ? " +
                "ORDER BY created_at DESC LIMIT 50";

        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();

            // Add in reverse order to get chronological
            List<GroupMessage> temp = new ArrayList<>();
            while (rs.next()) {
                temp.add(new GroupMessage(
                        rs.getInt("message_id"),
                        rs.getInt("group_id"),
                        rs.getString("sender"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at")));
            }
            for (int i = temp.size() - 1; i >= 0; i--) {
                messages.add(temp.get(i));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load group messages", e);
        }
        return messages;
    }

    /**
     * Gets group information by ID.
     */
    public static GroupInfo getGroupInfo(int groupId) {
        ensureSchema();

        String sql = "SELECT group_id, group_name, creator, created_at FROM " + GROUPS_TABLE + " WHERE group_id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new GroupInfo(
                        rs.getInt("group_id"),
                        rs.getString("group_name"),
                        rs.getString("creator"),
                        rs.getTimestamp("created_at"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get group info", e);
        }
        return null;
    }

    /**
     * Deletes a group and all its messages (CASCADE handles members).
     */
    public static boolean deleteGroup(int groupId) {
        ensureSchema();

        String sql = "DELETE FROM " + GROUPS_TABLE + " WHERE group_id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete group", e);
            return false;
        }
    }

    /**
     * Updates the content of a group message.
     * 
     * @param messageId  The ID of the message to update
     * @param newContent The new message content
     * @return true if update successful
     */
    public static boolean updateGroupMessage(int messageId, String newContent) {
        ensureSchema();

        String sql = "UPDATE " + MESSAGES_TABLE + " SET content = ? WHERE message_id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setInt(2, messageId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to update group message", e);
            return false;
        }
    }

    /**
     * Deletes a group message by ID.
     * 
     * @param messageId The ID of the message to delete
     * @return true if delete successful
     */
    public static boolean deleteGroupMessage(int messageId) {
        ensureSchema();

        String sql = "DELETE FROM " + MESSAGES_TABLE + " WHERE message_id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete group message", e);
            return false;
        }
    }

    /**
     * Retrieves a specific group message by ID.
     * 
     * @param messageId The ID of the message
     * @return The GroupMessage object, or null if not found
     */
    public static GroupMessage getGroupMessage(int messageId) {
        ensureSchema();

        String sql = "SELECT message_id, group_id, sender, content, created_at FROM " + MESSAGES_TABLE
                + " WHERE message_id = ?";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new GroupMessage(
                        rs.getInt("message_id"),
                        rs.getInt("group_id"),
                        rs.getString("sender"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get group message", e);
        }
        return null;
    }

}
