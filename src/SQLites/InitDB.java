package SQLites;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Database initialization for VKU Chat SQLite database.
 * Java 21 compatible using text blocks and improved error handling.
 */
public final class InitDB {

    private static final Logger LOGGER = Logger.getLogger(InitDB.class.getName());

    private InitDB() {
        // Utility class - prevent instantiation
    }

    /**
     * Initializes the SQLite database schema
     */
    public static void initialize() {
        String sql = """
                CREATE TABLE IF NOT EXISTS messages (
                	id INTEGER PRIMARY KEY AUTOINCREMENT,
                	sender TEXT NOT NULL,
                	receiver TEXT NOT NULL,
                	content TEXT NOT NULL,
                	time DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                """;

        try (Connection con = SQLiteUtil.getConnection();
                Statement st = con.createStatement()) {

            st.execute(sql);
            LOGGER.log(Level.INFO, "SQLite DB schema initialized");

            // Add sample data for testing
            MessageDAO.save("System", "Admin", "Database initialized successfully");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    /**
     * Main method for command-line initialization
     * 
     * @param args Command-line arguments (unused)
     */
    public static void main(String[] args) {
        initialize();
    }
}