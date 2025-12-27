package tags;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.Peer;

/**
 * Protocol message decoder for VKU Chat application.
 * Java 21 compatible with improved code quality.
 */
public final class Decode {

	private Decode() {
		// Utility class - prevent instantiation
	}

	// Compiled regex patterns
	private static final Pattern CREATE_ACCOUNT = Pattern.compile(
			Tags.SESSION_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG + ".*"
					+ Tags.PEER_NAME_CLOSE_TAG + Tags.PORT_OPEN_TAG + ".*"
					+ Tags.PORT_CLOSE_TAG + Tags.SESSION_CLOSE_TAG);

	private static final Pattern USERS = Pattern.compile(
			Tags.SESSION_ACCEPT_OPEN_TAG + "(" + Tags.PEER_OPEN_TAG
					+ Tags.PEER_NAME_OPEN_TAG + ".+" + Tags.PEER_NAME_CLOSE_TAG
					+ Tags.IP_OPEN_TAG + ".+" + Tags.IP_CLOSE_TAG
					+ Tags.PORT_OPEN_TAG + "[0-9]+" + Tags.PORT_CLOSE_TAG
					+ Tags.PEER_CLOSE_TAG + ")*" + Tags.SESSION_ACCEPT_CLOSE_TAG);

	private static final Pattern REQUEST = Pattern.compile(
			Tags.SESSION_KEEP_ALIVE_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG
					+ "[^<>]+" + Tags.PEER_NAME_CLOSE_TAG + Tags.STATUS_OPEN_TAG
					+ "(" + Tags.SERVER_ONLINE + "|" + Tags.SERVER_OFFLINE + ")"
					+ Tags.STATUS_CLOSE_TAG + Tags.SESSION_KEEP_ALIVE_CLOSE_TAG);

	private static final Pattern MESSAGE = Pattern.compile(
			Tags.CHAT_MSG_OPEN_TAG + ".*" + Tags.CHAT_MSG_CLOSE_TAG);

	private static final Pattern EDIT = Pattern.compile(
			Tags.CHAT_EDIT_OPEN_TAG + Tags.CHAT_EDIT_OLD_OPEN_TAG + ".*" + Tags.CHAT_EDIT_OLD_CLOSE_TAG
					+ Tags.CHAT_EDIT_NEW_OPEN_TAG + ".*" + Tags.CHAT_EDIT_NEW_CLOSE_TAG
					+ Tags.CHAT_EDIT_CLOSE_TAG);

	private static final Pattern DELETE = Pattern.compile(
			Tags.CHAT_DELETE_OPEN_TAG + Tags.CHAT_DELETE_BODY_OPEN_TAG + ".*" + Tags.CHAT_DELETE_BODY_CLOSE_TAG
					+ Tags.CHAT_DELETE_CLOSE_TAG);

	private static final Pattern TYPING = Pattern.compile(
			Tags.TYPING_OPEN_TAG + Tags.TYPING_STATE_OPEN_TAG + "(ON|OFF)" + Tags.TYPING_STATE_CLOSE_TAG
					+ Tags.TYPING_CLOSE_TAG);

	private static final Pattern REACTION = Pattern.compile(
			Tags.CHAT_REACTION_OPEN_TAG + Tags.CHAT_REACTION_TARGET_OPEN_TAG + ".*"
					+ Tags.CHAT_REACTION_TARGET_CLOSE_TAG
					+ Tags.CHAT_REACTION_EMOJI_OPEN_TAG + "[^<>]+" + Tags.CHAT_REACTION_EMOJI_CLOSE_TAG
					+ Tags.CHAT_REACTION_CLOSE_TAG);

	private static final Pattern FILE_NAME = Pattern.compile(
			Tags.FILE_REQ_OPEN_TAG + ".*" + Tags.FILE_REQ_CLOSE_TAG);

	private static final Pattern FEEDBACK = Pattern.compile(
			Tags.FILE_REQ_ACK_OPEN_TAG + ".*" + Tags.FILE_REQ_ACK_CLOSE_TAG);

	/**
	 * Extracts username and port from account creation message
	 * 
	 * @param msg Message to decode
	 * @return ArrayList with username at index 0 and port at index 1, or null if
	 *         invalid
	 */
	public static ArrayList<String> getUser(String msg) {
		ArrayList<String> user = new ArrayList<>();
		if (!CREATE_ACCOUNT.matcher(msg).matches()) {
			return null;
		}

		Pattern findName = Pattern.compile(Tags.PEER_NAME_OPEN_TAG + ".*" + Tags.PEER_NAME_CLOSE_TAG);
		Pattern findPort = Pattern.compile(Tags.PORT_OPEN_TAG + "[0-9]*" + Tags.PORT_CLOSE_TAG);

		Matcher find = findName.matcher(msg);
		if (find.find()) {
			String name = find.group(0);
			user.add(extractContent(name, Tags.PEER_NAME_OPEN_TAG, Tags.PEER_NAME_CLOSE_TAG));

			find = findPort.matcher(msg);
			if (find.find()) {
				String port = find.group(0);
				user.add(extractContent(port, Tags.PORT_OPEN_TAG, Tags.PORT_CLOSE_TAG));
				return user;
			}
		}
		return null;
	}

	/**
	 * Extracts all online peers from message
	 * 
	 * @param msg Message containing peer list
	 * @return ArrayList of Peer objects, or null if invalid
	 */
	public static ArrayList<Peer> getAllUser(String msg) {
		ArrayList<Peer> users = new ArrayList<>();
		if (!USERS.matcher(msg).matches()) {
			return null;
		}

		Pattern findPeer = Pattern.compile(Tags.PEER_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG + "[^<>]*"
				+ Tags.PEER_NAME_CLOSE_TAG + Tags.IP_OPEN_TAG + "[^<>]*" + Tags.IP_CLOSE_TAG
				+ Tags.PORT_OPEN_TAG + "[0-9]*" + Tags.PORT_CLOSE_TAG + Tags.PEER_CLOSE_TAG);

		Matcher find = findPeer.matcher(msg);
		while (find.find()) {
			String peer = find.group(0);
			Peer dataPeer = new Peer();

			// Extract name
			Pattern findName = Pattern.compile(Tags.PEER_NAME_OPEN_TAG + ".*" + Tags.PEER_NAME_CLOSE_TAG);
			Matcher matcher = findName.matcher(peer);
			if (matcher.find()) {
				dataPeer.setName(extractContent(matcher.group(0), Tags.PEER_NAME_OPEN_TAG, Tags.PEER_NAME_CLOSE_TAG));
			}

			// Extract IP (with sanitization to remove leading slashes)
			Pattern findIP = Pattern.compile(Tags.IP_OPEN_TAG + ".+" + Tags.IP_CLOSE_TAG);
			matcher = findIP.matcher(peer);
			if (matcher.find()) {
				String ipAddress = extractContent(matcher.group(0), Tags.IP_OPEN_TAG, Tags.IP_CLOSE_TAG);
				dataPeer.setHost(sanitizeIPAddress(ipAddress));
			}

			// Extract port
			Pattern findPort = Pattern.compile(Tags.PORT_OPEN_TAG + "[0-9]*" + Tags.PORT_CLOSE_TAG);
			matcher = findPort.matcher(peer);
			if (matcher.find()) {
				String portStr = extractContent(matcher.group(0), Tags.PORT_OPEN_TAG, Tags.PORT_CLOSE_TAG);
				dataPeer.setPort(Integer.parseInt(portStr));
			}

			users.add(dataPeer);
		}
		return users;
	}

	/**
	 * Updates peer list based on online status
	 * 
	 * @param peerList Current peer list
	 * @param msg      Server message
	 * @return Updated peer list
	 */
	public static ArrayList<Peer> updatePeerOnline(ArrayList<Peer> peerList, String msg) {
		if (!REQUEST.matcher(msg).matches()) {
			return peerList;
		}

		Pattern alive = Pattern.compile(Tags.STATUS_OPEN_TAG + Tags.SERVER_ONLINE + Tags.STATUS_CLOSE_TAG);
		if (alive.matcher(msg).find()) {
			return peerList; // Peer is online, no changes
		}

		// Extract peer name to remove if offline
		Pattern killUser = Pattern.compile(Tags.PEER_NAME_OPEN_TAG + "[^<>]*" + Tags.PEER_NAME_CLOSE_TAG);
		Matcher findState = killUser.matcher(msg);
		if (findState.find()) {
			String peerName = extractContent(findState.group(0), Tags.PEER_NAME_OPEN_TAG, Tags.PEER_NAME_CLOSE_TAG);
			peerList.removeIf(p -> p.getName().equals(peerName));
		}

		return peerList;
	}

	/**
	 * Extracts message content from chat message
	 * 
	 * @param msg Full chat message
	 * @return Message content, or null if invalid
	 */
	public static String getMessage(String msg) {
		if (MESSAGE.matcher(msg).matches()) {
			return extractContent(msg, Tags.CHAT_MSG_OPEN_TAG, Tags.CHAT_MSG_CLOSE_TAG);
		}
		return null;
	}

	public static boolean isEdit(String msg) {
		return EDIT.matcher(msg).matches();
	}

	public static boolean isDelete(String msg) {
		return DELETE.matcher(msg).matches();
	}

	public static boolean isTyping(String msg) {
		return TYPING.matcher(msg).matches();
	}

	public static boolean isReaction(String msg) {
		return REACTION.matcher(msg).matches();
	}

	public record EditPayload(String oldText, String newText) {
	}

	public record DeletePayload(String text) {
	}

	public record TypingPayload(boolean on) {
	}

	public record ReactionPayload(String target, String emoji) {
	}

	public static EditPayload getEditPayload(String msg) {
		if (!isEdit(msg)) {
			return null;
		}
		String oldText = extractContent(msg, Tags.CHAT_EDIT_OLD_OPEN_TAG, Tags.CHAT_EDIT_OLD_CLOSE_TAG);
		String newText = extractContent(msg, Tags.CHAT_EDIT_NEW_OPEN_TAG, Tags.CHAT_EDIT_NEW_CLOSE_TAG);
		return new EditPayload(oldText, newText);
	}

	public static DeletePayload getDeletePayload(String msg) {
		if (!isDelete(msg)) {
			return null;
		}
		String text = extractContent(msg, Tags.CHAT_DELETE_BODY_OPEN_TAG, Tags.CHAT_DELETE_BODY_CLOSE_TAG);
		return new DeletePayload(text);
	}

	public static TypingPayload getTypingPayload(String msg) {
		if (!isTyping(msg)) {
			return null;
		}
		String state = extractContent(msg, Tags.TYPING_STATE_OPEN_TAG, Tags.TYPING_STATE_CLOSE_TAG);
		return new TypingPayload("ON".equals(state));
	}

	public static ReactionPayload getReactionPayload(String msg) {
		if (!isReaction(msg)) {
			return null;
		}
		String target = extractContent(msg, Tags.CHAT_REACTION_TARGET_OPEN_TAG, Tags.CHAT_REACTION_TARGET_CLOSE_TAG);
		String emoji = extractContent(msg, Tags.CHAT_REACTION_EMOJI_OPEN_TAG, Tags.CHAT_REACTION_EMOJI_CLOSE_TAG);
		return new ReactionPayload(target, emoji);
	}

	/**
	 * Extracts peer name from chat request
	 * 
	 * @param msg Chat request message
	 * @return Peer name, or null if invalid
	 */
	public static String getNameRequestChat(String msg) {
		Pattern checkRequest = Pattern.compile(Tags.CHAT_REQ_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG + "[^<>]*"
				+ Tags.PEER_NAME_CLOSE_TAG + Tags.CHAT_REQ_CLOSE_TAG);
		if (checkRequest.matcher(msg).matches()) {
			return extractContent(msg,
					Tags.CHAT_REQ_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG,
					Tags.PEER_NAME_CLOSE_TAG + Tags.CHAT_REQ_CLOSE_TAG);
		}
		return null;
	}

	/**
	 * Checks if message is a valid file request
	 * 
	 * @param name File request message
	 * @return true if valid file request
	 */
	public static boolean checkFile(String name) {
		return FILE_NAME.matcher(name).matches();
	}

	/**
	 * Extracts content between two tags
	 * 
	 * @param src     Source string
	 * @param tagName Tag name (without brackets)
	 * @return Content between tags, or empty string if not found
	 */
	public static String getBetweenTags(String src, String tagName) {
		String openTag = "<" + tagName + ">";
		String closeTag = "</" + tagName + ">";
		return extractContent(src, openTag, closeTag);
	}

	/**
	 * Checks if message is a valid file feedback
	 * 
	 * @param msg Message to check
	 * @return true if valid feedback
	 */
	public static boolean checkFeedBack(String msg) {
		return FEEDBACK.matcher(msg).matches();
	}

	/**
	 * Helper method to extract content between two strings
	 * 
	 * @param src      Source string
	 * @param openTag  Opening tag
	 * @param closeTag Closing tag
	 * @return Content between tags, or empty string if not found
	 */
	private static String extractContent(String src, String openTag, String closeTag) {
		int start = src.indexOf(openTag);
		int end = src.indexOf(closeTag);

		if (start == -1 || end == -1 || end <= start) {
			return "";
		}

		start += openTag.length();
		return src.substring(start, end);
	}

	/**
	 * Sanitizes IP address by removing leading slashes and whitespace
	 * Handles formats like "/192.168.56.1" and converts to "192.168.56.1"
	 * 
	 * @param ipAddress Raw IP address from protocol
	 * @return Cleaned IP address suitable for InetAddress.getByName()
	 */
	private static String sanitizeIPAddress(String ipAddress) {
		if (ipAddress == null || ipAddress.isEmpty()) {
			return "";
		}
		// Remove leading slashes and whitespace
		return ipAddress.replaceAll("^/+", "").trim();
	}
}
