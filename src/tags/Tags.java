package tags;

import client.ChatFrame;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Protocol tags and GUI utilities for VKU Chat application.
 * Java 21 compatible.
 */
public final class Tags {

	private Tags() {
		// Utility class - prevent instantiation
	}

	// Constants
	public static final int IN_VALID = -1;
	public static final int MAX_MSG_SIZE = 1024000; // ~1MB

	// Session tags
	public static final String SESSION_OPEN_TAG = "<SESSION_REQ>";
	public static final String SESSION_CLOSE_TAG = "</SESSION_REQ>";
	public static final String PEER_NAME_OPEN_TAG = "<PEER_NAME>";
	public static final String PEER_NAME_CLOSE_TAG = "</PEER_NAME>";
	public static final String PORT_OPEN_TAG = "<PORT>";
	public static final String PORT_CLOSE_TAG = "</PORT>";

	// Keep-alive tags
	public static final String SESSION_KEEP_ALIVE_OPEN_TAG = "<SESSION_KEEP_ALIVE>";
	public static final String SESSION_KEEP_ALIVE_CLOSE_TAG = "</SESSION_KEEP_ALIVE>";
	public static final String STATUS_OPEN_TAG = "<STATUS>";
	public static final String STATUS_CLOSE_TAG = "</STATUS>";

	// Response tags
	public static final String SESSION_DENY_TAG = "<SESSION_DENY />";
	public static final String SESSION_ACCEPT_OPEN_TAG = "<SESSION_ACCEPT>";
	public static final String SESSION_ACCEPT_CLOSE_TAG = "</SESSION_ACCEPT>";

	// Chat request tags
	public static final String CHAT_REQ_OPEN_TAG = "<CHAT_REQ>";
	public static final String CHAT_REQ_CLOSE_TAG = "</CHAT_REQ>";
	public static final String IP_OPEN_TAG = "<IP>";
	public static final String IP_CLOSE_TAG = "</IP>";
	public static final String CHAT_DENY_TAG = "<CHAT_DENY />";
	public static final String CHAT_ACCEPT_TAG = "<CHAT_ACCEPT />";

	// Chat message tags
	public static final String CHAT_MSG_OPEN_TAG = "<CHAT_MSG>";
	public static final String CHAT_MSG_CLOSE_TAG = "</CHAT_MSG>";
	public static final String PEER_OPEN_TAG = "<PEER>";
	public static final String PEER_CLOSE_TAG = "</PEER>";

	// File transfer tags
	public static final String FILE_REQ_OPEN_TAG = "<FILE_REQ>";
	public static final String FILE_REQ_CLOSE_TAG = "</FILE_REQ>";
	public static final String FILE_REQ_NOACK_TAG = "<FILE_REQ_NOACK />";
	public static final String FILE_REQ_ACK_OPEN_TAG = "<FILE_REQ_ACK>";
	public static final String FILE_REQ_ACK_CLOSE_TAG = "</FILE_REQ_ACK>";
	public static final String FILE_DATA_BEGIN_TAG = "<FILE_DATA_BEGIN />";
	public static final String FILE_DATA_OPEN_TAG = "<FILE_DATA>";
	public static final String FILE_DATA_CLOSE_TAG = "</FILE_DATA>";
	public static final String FILE_DATA_END_TAG = "<FILE_DATA_END />";
	public static final String CHAT_CLOSE_TAG = "<CHAT_CLOSE />";

	// Voice call tags
	public static final String VOICE_CALL_REQ = "<voice_call_req>";
	public static final String VOICE_CALL_REQ_END = "<voice_call_accept>";
	public static final String VOICE_CALL_REJECT = "<voice_call_reject>";
	public static final String VOICE_DATA = "<voice_data>";
	public static final String VOICE_CALL_END = "<voice_call_end>";
	public static final String VOICE_CALL_DENY = "<voice_call_end>";
	public static final String VOICE_CALL_REQUEST = "<VOICE_CALL_REQUEST>";
	public static final String VOICE_CALL_ACCEPTED = "<VOICE_CALL_ACCEPTED>";
	public static final String VOICE_CALL_REJECTED = "<VOICE_CALL_REJECTED>";
	public static final String VOICE_CALL_ENDED = "<VOICE_CALL_ENDED>";
	public static final String VOICE_REQUEST_OPEN = "<voice_request>";
	public static final String VOICE_REQUEST_CLOSE = "</voice_request>";
	public static final String VOICE_ACCEPT_OPEN = "<voice_accept>";
	public static final String VOICE_ACCEPT_CLOSE = "</voice_accept>";
	public static final String VOICE_REJECT_TAG = "<voice_reject/>";

	// Auth tags
	public static final String REGISTER_TAG = "REGISTER";
	public static final String REGISTER_OK = "REGISTER_OK";
	public static final String REGISTER_DENY = "REGISTER_DENY";
	public static final String LOGIN_TAG = "LOGIN";

	// Server status
	public static final String SERVER_ONLINE = "RUNNING";
	public static final String SERVER_OFFLINE = "STOP";

	// Message tags
	public static final String MSG_OPEN_TAG = "<msg>";
	public static final String MSG_CLOSE_TAG = "</msg>";

	// Edit message tags
	public static final String CHAT_EDIT_OPEN_TAG = "<CHAT_EDIT>";
	public static final String CHAT_EDIT_CLOSE_TAG = "</CHAT_EDIT>";
	public static final String CHAT_EDIT_OLD_OPEN_TAG = "<OLD>";
	public static final String CHAT_EDIT_OLD_CLOSE_TAG = "</OLD>";
	public static final String CHAT_EDIT_NEW_OPEN_TAG = "<NEW>";
	public static final String CHAT_EDIT_NEW_CLOSE_TAG = "</NEW>";

	/**
	 * Shows a yes/no confirmation dialog
	 * 
	 * @param frame   Parent frame (unused)
	 * @param message Message to display
	 * @return true if YES is clicked, false otherwise
	 */
	public static boolean showYN(ChatFrame frame, String message) {
		int result = JOptionPane.showConfirmDialog(
				null,
				message,
				"Confirm",
				JOptionPane.YES_NO_OPTION);
		return result == JOptionPane.YES_OPTION;
	}

	/**
	 * Shows a notification or error message
	 * 
	 * @param frame   Parent component (can be null)
	 * @param msg     Message to display
	 * @param isError true for error message, false for info message
	 */
	public static void show(Object frame, String msg, boolean isError) {
		JOptionPane.showMessageDialog(null, msg,
				isError ? "Error" : "Notification",
				isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Shows a message dialog with optional confirmation
	 * 
	 * @param frame Parent frame
	 * @param msg   Message to display
	 * @param type  true for yes/no confirmation, false for info message
	 * @return YES_OPTION if type is true and user clicks YES, otherwise IN_VALID
	 */
	public static int show(JFrame frame, String msg, boolean type) {
		if (type) {
			return JOptionPane.showConfirmDialog(frame, msg, null, JOptionPane.YES_NO_OPTION);
		}
		JOptionPane.showMessageDialog(frame, msg);
		return IN_VALID;
	}
}
