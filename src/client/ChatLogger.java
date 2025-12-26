package client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Logs chat messages to file for persistence and audit trail.
 * Java 21 compatible with thread-safe operations and proper resource
 * management.
 */
public class ChatLogger implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ChatLogger.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BufferedWriter writer;
    private final String logFile;
    private boolean closed = false;

    /**
     * Creates a new ChatLogger for the given user pair
     * 
     * @param user  Current user
     * @param guest Remote user
     * @throws IOException If file creation fails
     */
    public ChatLogger(String user, String guest) throws IOException {
        this.logFile = "chat_" + sanitizeFileName(user) + "_" + sanitizeFileName(guest) + ".txt";
        this.writer = new BufferedWriter(new FileWriter(logFile, true));
        logSystemMessage("Chat session started");
    }

    /**
     * Logs a message with timestamp
     * 
     * @param sender  Message sender
     * @param message Message content
     */
    public synchronized void log(String sender, String message) {
        if (closed) {
            LOGGER.log(Level.WARNING, "Attempt to log on closed ChatLogger");
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            writer.write(String.format("[%s] %s: %s", timestamp, sender, message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write chat log", e);
        }
    }

    /**
     * Logs a system message
     * 
     * @param message System message
     */
    private synchronized void logSystemMessage(String message) {
        log("SYSTEM", message);
    }

    /**
     * Closes the log file
     */
    @Override
    public synchronized void close() {
        if (closed)
            return;

        try {
            logSystemMessage("Chat session ended");
            writer.close();
            closed = true;
            LOGGER.log(Level.INFO, "Chat log closed: " + logFile);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing chat log", e);
        }
    }

    /**
     * Checks if logger is closed
     * 
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Sanitizes filename by removing special characters
     * 
     * @param name Original name
     * @return Sanitized filename
     */
    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
