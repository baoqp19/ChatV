package SQLites;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * SQLite database utility for VKU Chat message storage.
 * Java 21 compatible with improved error handling.
 */
public final class SQLiteUtil {

    private static final Logger LOGGER = Logger.getLogger(SQLiteUtil.class.getName());

    private SQLiteUtil() {
        // Utility class - prevent instantiation
    }

    private static final String DB_PATH = System.getProperty("user.dir") + File.separator + "MasterChat"
            + File.separator + "data" + File.separator + "message.db";

    static {
        initializeDatabase();
    }

    /**
     * Initializes the SQLite database
     */
    private static void initializeDatabase() {
        try {
            // Create data directory if it doesn't exist
            File dataDir = new File(System.getProperty("user.dir") + File.separator
                    + "MasterChat" + File.separator + "data");
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                LOGGER.log(Level.WARNING, "Failed to create data directory");
            }

            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
            LOGGER.log(Level.INFO, "SQLite driver loaded successfully");
            LOGGER.log(Level.INFO, "DB path: " + DB_PATH);

        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "SQLite JDBC driver not found", e);
        }
    }

    /**
     * Gets a SQLite database connection
     * 
     * @return Database connection
     * @throws Exception If connection fails
     */
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    /**
     * Gets the database file path
     * 
     * @return Path to SQLite database file
     */
    public static String getDatabasePath() {
        return DB_PATH;
    }

    /**
     * Verifies database connectivity
     * 
     * @return true if connection successful
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "SQLite connection test failed", e);
            return false;
        }
    }
}