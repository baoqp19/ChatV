package tags;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Protocol message encoder for VKU Chat application.
 * Java 21 compatible with improved code quality.
 */
public final class Encode {

	private Encode() {
		// Utility class - prevent instantiation
	}

	private static final Pattern CHECK_MESSAGE = Pattern.compile("[^<>]*[<>]");

	/**
	 * Creates an account creation message
	 * 
	 * @param name Username
	 * @param port Client port
	 * @return Encoded message
	 */
	public static String getCreateAccount(String name, String port) {
		return Tags.SESSION_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG + name
				+ Tags.PEER_NAME_CLOSE_TAG + Tags.PORT_OPEN_TAG + port
				+ Tags.PORT_CLOSE_TAG + Tags.SESSION_CLOSE_TAG;
	}

	/**
	 * Creates a keep-alive/status request message
	 * 
	 * @param name Username
	 * @return Encoded message with online status
	 */
	public static String sendRequest(String name) {
		return Tags.SESSION_KEEP_ALIVE_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG
				+ name + Tags.PEER_NAME_CLOSE_TAG + Tags.STATUS_OPEN_TAG
				+ Tags.SERVER_ONLINE + Tags.STATUS_CLOSE_TAG
				+ Tags.SESSION_KEEP_ALIVE_CLOSE_TAG;
	}

	/**
	 * Creates a registration message
	 * 
	 * @param username Username to register
	 * @param password Password for account
	 * @return Encoded message
	 */
	public static String getRegister(String username, String password) {
		return Tags.REGISTER_TAG + "|" + username + "|" + password;
	}

	/**
	 * Encodes a chat message with special character handling
	 * 
	 * @param message Message to encode
	 * @return Encoded message wrapped in tags
	 */
	public static String sendMessage(String message) {
		StringBuilder result = new StringBuilder();
		Matcher findMessage = CHECK_MESSAGE.matcher(message);

		int lastEnd = 0;
		while (findMessage.find()) {
			result.append(message, lastEnd, findMessage.start()).append(findMessage.group(0));
			lastEnd = findMessage.end();
		}
		result.append(message.substring(lastEnd));

		return Tags.CHAT_MSG_OPEN_TAG + result + Tags.CHAT_MSG_CLOSE_TAG;
	}

	/**
	 * Encodes an edit message containing old displayed text and new raw text.
	 */
	public static String sendEdit(String oldDisplayedText, String newText) {
		return Tags.CHAT_EDIT_OPEN_TAG +
				Tags.CHAT_EDIT_OLD_OPEN_TAG + oldDisplayedText + Tags.CHAT_EDIT_OLD_CLOSE_TAG +
				Tags.CHAT_EDIT_NEW_OPEN_TAG + newText + Tags.CHAT_EDIT_NEW_CLOSE_TAG +
				Tags.CHAT_EDIT_CLOSE_TAG;
	}

	/**
	 * Encodes a delete message for a given displayed text.
	 */
	public static String sendDelete(String displayedText) {
		return Tags.CHAT_DELETE_OPEN_TAG +
				Tags.CHAT_DELETE_BODY_OPEN_TAG + displayedText + Tags.CHAT_DELETE_BODY_CLOSE_TAG +
				Tags.CHAT_DELETE_CLOSE_TAG;
	}

	/**
	 * Encodes typing indicator state.
	 */
	public static String sendTyping(boolean on) {
		return Tags.TYPING_OPEN_TAG +
				Tags.TYPING_STATE_OPEN_TAG + (on ? "ON" : "OFF") + Tags.TYPING_STATE_CLOSE_TAG +
				Tags.TYPING_CLOSE_TAG;
	}

	/**
	 * Encodes a clear conversation event.
	 */
	public static String sendClearChat() {
		return Tags.CHAT_CLEAR_TAG;
	}

	/**
	 * Encodes a reaction targeting a specific displayed text.
	 */
	public static String sendReaction(String targetDisplayText, String emojiName) {
		return Tags.CHAT_REACTION_OPEN_TAG +
				Tags.CHAT_REACTION_TARGET_OPEN_TAG + targetDisplayText + Tags.CHAT_REACTION_TARGET_CLOSE_TAG +
				Tags.CHAT_REACTION_EMOJI_OPEN_TAG + emojiName + Tags.CHAT_REACTION_EMOJI_CLOSE_TAG +
				Tags.CHAT_REACTION_CLOSE_TAG;
	}

	/**
	 * Encodes a group creation request.
	 */
	public static String sendGroupCreate(String groupName, String creator) {
		return Tags.GROUP_CREATE_OPEN_TAG +
				Tags.GROUP_NAME_OPEN_TAG + groupName + Tags.GROUP_NAME_CLOSE_TAG +
				Tags.GROUP_CREATOR_OPEN_TAG + creator + Tags.GROUP_CREATOR_CLOSE_TAG +
				Tags.GROUP_CREATE_CLOSE_TAG;
	}

	/**
	 * Encodes a group invite message.
	 */
	public static String sendGroupInvite(int groupId, String groupName, String invitee) {
		return Tags.GROUP_INVITE_OPEN_TAG +
				Tags.GROUP_ID_OPEN_TAG + groupId + Tags.GROUP_ID_CLOSE_TAG +
				Tags.GROUP_NAME_OPEN_TAG + groupName + Tags.GROUP_NAME_CLOSE_TAG +
				Tags.INVITEE_OPEN_TAG + invitee + Tags.INVITEE_CLOSE_TAG +
				Tags.GROUP_INVITE_CLOSE_TAG;
	}

	/**
	 * Encodes a group message.
	 */
	public static String sendGroupMessage(int groupId, String sender, String content) {
		return Tags.GROUP_MSG_OPEN_TAG +
				Tags.GROUP_ID_OPEN_TAG + groupId + Tags.GROUP_ID_CLOSE_TAG +
				Tags.GROUP_SENDER_OPEN_TAG + sender + Tags.GROUP_SENDER_CLOSE_TAG +
				Tags.GROUP_CONTENT_OPEN_TAG + content + Tags.GROUP_CONTENT_CLOSE_TAG +
				Tags.GROUP_MSG_CLOSE_TAG;
	}

	/**
	 * Encodes a group join notification.
	 */
	public static String sendGroupJoin(int groupId, String member) {
		return Tags.GROUP_JOIN_OPEN_TAG +
				Tags.GROUP_ID_OPEN_TAG + groupId + Tags.GROUP_ID_CLOSE_TAG +
				Tags.GROUP_MEMBER_OPEN_TAG + member + Tags.GROUP_MEMBER_CLOSE_TAG +
				Tags.GROUP_JOIN_CLOSE_TAG;
	}

	/**
	 * Creates a chat request message
	 * 
	 * @param name Target peer name
	 * @return Encoded chat request message
	 */
	public static String sendRequestChat(String name) {
		return Tags.CHAT_REQ_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG + name
				+ Tags.PEER_NAME_CLOSE_TAG + Tags.CHAT_REQ_CLOSE_TAG;
	}

	/**
	 * Creates a file transfer request message
	 * 
	 * @param name Filename
	 * @return Encoded file request message
	 */
	public static String sendFile(String name) {
		return Tags.FILE_REQ_OPEN_TAG + name + Tags.FILE_REQ_CLOSE_TAG;
	}

	/**
	 * Creates an exit/disconnect message
	 * 
	 * @param name Username
	 * @return Encoded message with offline status
	 */
	public static String exit(String name) {
		return Tags.SESSION_KEEP_ALIVE_OPEN_TAG + Tags.PEER_NAME_OPEN_TAG
				+ name + Tags.PEER_NAME_CLOSE_TAG + Tags.STATUS_OPEN_TAG
				+ Tags.SERVER_OFFLINE + Tags.STATUS_CLOSE_TAG
				+ Tags.SESSION_KEEP_ALIVE_CLOSE_TAG;
	}
}
