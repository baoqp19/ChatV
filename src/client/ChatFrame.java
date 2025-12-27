package client;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import data.DataFile;
import database.MessageDAO;
import tags.Decode;
import tags.Encode;
import tags.Tags;

import static tags.Encode.sendMessage;

public class ChatFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String URL_DIR = System.getProperty("user.dir");
    private static final int EMOJI_BUTTON_WIDTH = 44;
    private static final int EMOJI_BUTTON_HEIGHT = 41;

    // UI Components
    private JPanel contentPane;
    private JTextPane txtDisplayMessage;
    private JTextField txtMessage;
    private JButton btnSend, btnSendFile;
    private JLabel lblReceive;
    private JProgressBar progressBar;

    // Connection & Data
    private final Socket socketChat;
    private final String nameUser;
    private final String nameGuest;
    private final ChatRoom chat;
    private final ControllerChatFrame frameChat = new ControllerChatFrame();

    private boolean isStop = false;
    private int port;
    private int portVoice;

    // Video call state
    private VideoSendThread videoSendThread;
    private VideoReceiveThread videoReceiveThread;
    private java.net.DatagramSocket videoSocket;
    private boolean isVideoCallActive = false;
    private JFrame videoFrame;
    private JLabel localVideoLabel;
    private JLabel remoteVideoLabel;
    private JLabel typingLabel;

    public ChatFrame(String user, String guest, Socket socket, int port) throws Exception {
        this(user, guest, socket, port, port);
    }

    public ChatFrame(String user, String guest, Socket socket, int port, int portVoice) throws Exception {
        this.nameUser = user;
        this.nameGuest = guest;
        this.socketChat = socket;
        this.port = port;
        this.portVoice = portVoice;

        this.chat = new ChatRoom(socketChat, nameUser, nameGuest);
        this.chat.start();

        EventQueue.invokeLater(() -> {
            initializeUI();
            loadHistory();
            this.setVisible(true);
        });
    }

    // Store reference to the messages panel
    private JPanel messagesPanel;

    // Track text message labels to support edits
    private final List<JLabel> messageLabels = new ArrayList<>();

    // Reaction tracking per message label
    private final Map<JLabel, JPanel> reactionStrips = new HashMap<>();
    private final Map<JLabel, Map<String, Integer>> reactionCounts = new HashMap<>();

    // ============ MESSAGE DISPLAY METHODS ============

    public void updateChat_receive(String msg) {
        // Check if message is emoji
        if (msg.startsWith("[emoji:") && msg.endsWith("]")) {
            String emojiName = msg.substring(7, msg.length() - 1);
            addEmojiMessageBubble(emojiName, "left");
        } else {
            addMessageBubble(msg, "left", new Color(255, 255, 255), Color.BLACK);
        }
    }

    public void updateChat_send(String msg) {
        addMessageBubble(msg, "right", new Color(0, 132, 255), Color.WHITE);
    }

    public void updateChat_notify(String msg) {
        addMessageBubble(msg, "right", new Color(241, 196, 15), Color.WHITE);
    }

    public void updateChat_send_Symbol(String msg) {
        // For emoji - msg is emoji name like "like32"
        addEmojiMessageBubble(msg, "right");
    }

    private void addMessageBubble(String msg, String align, Color bgColor, Color textColor) {
        JPanel bubbleContainer = new JPanel();
        bubbleContainer.setLayout(new FlowLayout("left".equals(align) ? FlowLayout.LEFT : FlowLayout.RIGHT));
        bubbleContainer.setBackground(new Color(54, 57, 63));
        bubbleContainer.setMaximumSize(new Dimension(500, 100));

        JPanel bubble = new JPanel();
        bubble.setBackground(bgColor);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        String time = String.format("%02d:%02d", LocalDateTime.now().getHour(), LocalDateTime.now().getMinute());

        JLabel messageLabel = new JLabel(msg);
        messageLabel.setForeground(textColor);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setMaximumSize(new Dimension(300, 50));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(messageLabel);

        trackMessageLabel(messageLabel);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(new Color(102, 102, 102));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(timeLabel);
        ensureReactionStrip(bubble, messageLabel);

        attachMessageEditor(bubble, messageLabel, "right".equals(align));

        bubbleContainer.add(bubble);
        messagesPanel.add(bubbleContainer);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    /**
     * Allows the sender to edit a message bubble in-place.
     */
    private void attachMessageEditor(JPanel bubble, JLabel messageLabel, boolean allowEdit) {
        if (!allowEdit) {
            return;
        }

        MouseAdapter editor = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String current = messageLabel.getText();

                Object[] options = { "Edit", "Delete", "React", "Copy", "Cancel" };
                int choice = JOptionPane.showOptionDialog(ChatFrame.this,
                        "Choose action",
                        "Message actions",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) {
                    String updated = JOptionPane.showInputDialog(ChatFrame.this, "Edit message",
                            stripEditedSuffix(current));
                    if (updated == null) {
                        return; // Cancel pressed
                    }

                    updated = updated.trim();
                    if (updated.isEmpty() || updated.equals(stripEditedSuffix(current))) {
                        return; // Nothing to change
                    }

                    String newDisplay = updated + " (edited)";
                    messageLabel.setText(newDisplay);

                    try {
                        chat.sendMessage(Encode.sendEdit(current, updated));
                        MessageDAO.updateMessage(nameUser, nameGuest, current, newDisplay);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (choice == 1) {
                    int confirm = JOptionPane.showConfirmDialog(ChatFrame.this,
                            "Delete this message?",
                            "Confirm delete",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }

                    deleteLocalMessage(messageLabel);
                } else if (choice == 2) {
                    String[] reactions = new String[] { "like32", "love32", "smile32", "sad32" };
                    int rChoice = JOptionPane.showOptionDialog(ChatFrame.this,
                            "Pick a reaction",
                            "React",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            reactions,
                            reactions[0]);
                    if (rChoice >= 0) {
                        String emojiName = reactions[rChoice];
                        applyReaction(messageLabel, emojiName);
                        try {
                            chat.sendMessage(Encode.sendReaction(stripEditedSuffix(current), emojiName));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else if (choice == 3) {
                    String textToCopy = stripEditedSuffix(current);
                    try {
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                                .setContents(new java.awt.datatransfer.StringSelection(textToCopy), null);
                        updateChat_notify("Copied to clipboard");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        bubble.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bubble.addMouseListener(editor);
        messageLabel.addMouseListener(editor);
    }

    private void trackMessageLabel(JLabel label) {
        messageLabels.add(label);
    }

    private String stripEditedSuffix(String text) {
        if (text == null) {
            return "";
        }
        String suffix = " (edited)";
        if (text.endsWith(suffix)) {
            return text.substring(0, text.length() - suffix.length());
        }
        return text;
    }

    private boolean updateLabelText(String oldDisplay, String newDisplay) {
        String normalizedOld = stripEditedSuffix(oldDisplay);
        for (JLabel label : new ArrayList<>(messageLabels)) {
            String currentNormalized = stripEditedSuffix(label.getText());
            if (currentNormalized.equals(normalizedOld)) {
                label.setText(newDisplay);
                label.revalidate();
                label.repaint();
                return true;
            }
        }
        return false;
    }

    private void deleteLocalMessage(JLabel label) {
        String display = label.getText();
        removeBubbleForLabel(label);
        try {
            chat.sendMessage(Encode.sendDelete(display));
            MessageDAO.deleteMessage(nameUser, nameGuest, display);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean deleteLabelText(String displayText) {
        String target = stripEditedSuffix(displayText);
        for (JLabel label : new ArrayList<>(messageLabels)) {
            String normalized = stripEditedSuffix(label.getText());
            if (normalized.equals(target)) {
                removeBubbleForLabel(label);
                return true;
            }
        }
        return false;
    }

    private void removeBubbleForLabel(JLabel label) {
        Container bubble = label.getParent();
        Container container = bubble != null ? bubble.getParent() : null;
        if (container != null && container.getParent() == messagesPanel) {
            messagesPanel.remove(container);
        }
        reactionStrips.remove(label);
        reactionCounts.remove(label);
        messageLabels.remove(label);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    // ============ REACTIONS HELPERS (outer class) ============

    private void ensureReactionStrip(JPanel bubble, JLabel messageLabel) {
        if (reactionStrips.containsKey(messageLabel)) {
            return;
        }
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        strip.setOpaque(false);
        strip.setVisible(false);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(strip);
        reactionStrips.put(messageLabel, strip);
        reactionCounts.put(messageLabel, new HashMap<>());
    }

    private void applyReaction(JLabel label, String emojiName) {
        JPanel strip = reactionStrips.get(label);
        if (strip == null) {
            // Attempt to attach if missing
            Container bubble = label.getParent();
            if (bubble instanceof JPanel) {
                ensureReactionStrip((JPanel) bubble, label);
                strip = reactionStrips.get(label);
            }
        }
        Map<String, Integer> counts = reactionCounts.computeIfAbsent(label, k -> new HashMap<>());
        counts.put(emojiName, counts.getOrDefault(emojiName, 0) + 1);
        updateReactionUI(label);
    }

    private void updateReactionUI(JLabel label) {
        JPanel strip = reactionStrips.get(label);
        if (strip == null)
            return;
        strip.removeAll();
        Map<String, Integer> counts = reactionCounts.get(label);
        if (counts == null || counts.isEmpty()) {
            strip.setVisible(false);
            strip.revalidate();
            strip.repaint();
            return;
        }
        strip.setVisible(true);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            ImageIcon icon = getEmojiImage(e.getKey());
            if (icon != null) {
                Image scaled = icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                JLabel chip = new JLabel(new ImageIcon(scaled));
                chip.setText(" " + e.getValue());
                chip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                chip.setForeground(new Color(60, 60, 65));
                chip.setOpaque(false);
                strip.add(chip);
            }
        }
        strip.revalidate();
        strip.repaint();
    }

    // ============ UI INITIALIZATION ============

    private void initializeUI() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setupWindowCloseListener();
        setupFrameProperties();

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        addHeaderPanel();
        addMessageDisplayPanel();
        addEmojiPanel();
        addMessageInputPanel();
        addReceiveLabel();
    }

    private void setupWindowCloseListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                closeChat();
            }
        });
    }

    private void closeChat() {
        try {
            isStop = true;
            dispose();
            chat.sendMessage(Tags.CHAT_CLOSE_TAG);
            chat.stopChat();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // hehe

    private void setupFrameProperties() {
        setResizable(false);
        setTitle("BOX CHAT");
        setBounds(100, 100, 576, 595);
    }

    private void addHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setBounds(0, 0, 573, 67);
        contentPane.add(panel);
        panel.setLayout(null);

        // Logo
        JLabel lblLogo = new JLabel();
        lblLogo.setIcon(new ImageIcon(Objects.requireNonNull(
                ChatFrame.class.getResource("/image/VKU64.png"))));
        lblLogo.setBounds(20, 0, 66, 67);
        panel.add(lblLogo);

        // Guest name
        JLabel nameLabel = new JLabel(nameGuest);
        nameLabel.setFont(new Font("Tahoma", Font.PLAIN, 32));
        nameLabel.setBounds(96, 10, 129, 38);
        panel.add(nameLabel);

        // Video call button
        addCallButton(panel, 474, "/image/videocall48.png", "Video call",
                e -> startVideoCall());
    }

    private void addCallButton(JPanel panel, int x, String iconPath, String tooltip, ActionListener action) {
        JButton btn = new JButton();
        btn.setToolTipText(tooltip);
        btn.setIcon(new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource(iconPath))));
        btn.setBounds(x, 16, 32, 32);
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.addActionListener(action);
        panel.add(btn);
    }

    private void addMessageDisplayPanel() {
        JPanel panel = new JPanel();
        panel.setBounds(0, 66, 562, 323);
        panel.setLayout(null);
        contentPane.add(panel);

        // Use a JPanel with BoxLayout for message display
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(new Color(54, 57, 63));

        JScrollPane scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBounds(0, 0, 562, 300);
        scrollPane.setBackground(new Color(54, 57, 63));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane);

        // Keep txtDisplayMessage for backward compatibility
        txtDisplayMessage = new JTextPane();

        // Typing indicator below message list
        typingLabel = new JLabel("");
        typingLabel.setForeground(new Color(0, 132, 255));
        typingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        typingLabel.setBounds(10, 303, 300, 20);
        typingLabel.setVisible(false);
        panel.add(typingLabel);
    }

    private void addEmojiPanel() {
        JPanel panel = new JPanel();
        panel.setBounds(0, 372, 573, 73);
        contentPane.add(panel);
        panel.setLayout(null);

        addEmojiButton(panel, 31, "/image/like32.png", "like32");
        addEmojiButton(panel, 144, "/image/love32.png", "love32");
        addEmojiButton(panel, 265, "/image/smile32.png", "smile32");
        addEmojiButton(panel, 378, "/image/sad32.png", "sad32");
        addEmojiButton(panel, 495, "/image/img2.png", "img2");

        progressBar = new JProgressBar();
        progressBar.setBounds(10, 22, 540, 41);
        progressBar.setVisible(false);
        panel.add(progressBar);
    }

    private void addEmojiButton(JPanel panel, int x, String iconPath, String imageName) {
        JButton btn = new JButton();
        btn.setIcon(new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource(iconPath))));
        btn.setBounds(x, 22, EMOJI_BUTTON_WIDTH, EMOJI_BUTTON_HEIGHT);
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.addActionListener(e -> sendEmoji(imageName));
        panel.add(btn);
    }

    private void sendEmoji(String imageName) {
        try {
            // Send emoji name through network
            String emojiMsg = "[emoji:" + imageName + "]";
            chat.sendMessage(sendMessage(emojiMsg));

            // Display emoji locally
            addEmojiMessageBubble(imageName, "right");

            // Save emoji to database
            MessageDAO.saveMessage(nameUser, nameGuest, emojiMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMessageInputPanel() {
        JPanel panel = new JPanel();
        panel.setBounds(0, 446, 562, 73);
        contentPane.add(panel);
        panel.setLayout(null);

        // Message input field
        txtMessage = new JTextField();
        txtMessage.setBounds(0, 5, 433, 45);
        txtMessage.setColumns(10);
        txtMessage.setFont(new Font("Courier New", Font.PLAIN, 18));
        txtMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendTextMessage();
                    return; // Don't send typing indicator when sending message
                }
                // Only send typing indicator for actual text input
                if (!e.isActionKey() && !e.isControlDown() && !e.isAltDown()) {
                    handleTypingKeyPress();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                    scheduleTypingStop();
                }
            }
        });
        panel.add(txtMessage);

        // Send file button
        btnSendFile = new JButton();
        btnSendFile.setIcon(new ImageIcon(Objects.requireNonNull(
                ChatFrame.class.getResource("/image/file32.png"))));
        btnSendFile.setBounds(440, 10, 50, 45);
        btnSendFile.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnSendFile.setContentAreaFilled(false);
        btnSendFile.addActionListener(e -> selectAndSendFile());
        panel.add(btnSendFile);

        // Send message button
        btnSend = new JButton();
        btnSend.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnSend.setContentAreaFilled(false);
        ImageIcon originalIcon = new ImageIcon(Objects.requireNonNull(
                ChatFrame.class.getResource("/image/send32.png")));
        Image scaledImage = originalIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
        btnSend.setIcon(new ImageIcon(scaledImage));
        btnSend.setBounds(500, 5, 60, 60);
        btnSend.addActionListener(e -> sendTextMessage());
        panel.add(btnSend);

        // Clear chat button
        JButton btnClear = new JButton("Clear");
        btnClear.setBounds(410, 52, 70, 20);
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClear.addActionListener(e -> clearConversation());
        panel.add(btnClear);
    }

    // Typing indicator: debounce logic
    private boolean typingOnSent = false;
    private javax.swing.Timer typingTimer;

    private void handleTypingKeyPress() {
        try {
            if (!typingOnSent) {
                chat.sendMessage(Encode.sendTyping(true));
                typingOnSent = true;
            }
            scheduleTypingStop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void scheduleTypingStop() {
        if (typingTimer != null && typingTimer.isRunning()) {
            typingTimer.restart();
            return;
        }
        typingTimer = new javax.swing.Timer(900, ev -> {
            try {
                chat.sendMessage(Encode.sendTyping(false));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            typingOnSent = false;
            typingTimer.stop();
        });
        typingTimer.setRepeats(false);
        typingTimer.start();
    }

    private void setPeerTyping(boolean on) {
        typingLabel.setText(on ? (nameGuest + " is typing...") : "");
        typingLabel.setVisible(on);
    }

    private void clearConversation() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear this conversation for you and notify peer?",
                "Clear Conversation",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        // Local UI clear
        messageLabels.clear();
        messagesPanel.removeAll();
        messagesPanel.revalidate();
        messagesPanel.repaint();
        // DB clear
        MessageDAO.deleteConversation(nameUser, nameGuest);
        // Notify peer
        try {
            chat.sendMessage(Encode.sendClearChat());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendTextMessage() {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty())
            return;

        txtMessage.setText("");
        try {
            // Send message through network
            chat.sendMessage(sendMessage(msg));

            // Display message locally
            updateChat_send(msg);

            // Save message to database
            MessageDAO.saveMessage(nameUser, nameGuest, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        new Thread(() -> {
            var history = MessageDAO.getConversation(nameUser, nameGuest, 50);
            if (history == null || history.isEmpty()) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                for (MessageDAO.ChatMessage m : history) {
                    String content = m.content();

                    // Check if message is emoji
                    if (content.startsWith("[emoji:") && content.endsWith("]")) {
                        String emojiName = content.substring(7, content.length() - 1);
                        addEmojiMessageBubbleWithTime(emojiName,
                                m.sender().equals(nameUser) ? "right" : "left",
                                m.createdAt());
                    } else {
                        // Display text message with timestamp
                        addMessageBubbleWithTime(content,
                                m.sender().equals(nameUser) ? "right" : "left",
                                m.sender().equals(nameUser) ? new Color(0, 132, 255) : new Color(255, 255, 255),
                                m.sender().equals(nameUser) ? Color.WHITE : Color.BLACK,
                                m.createdAt());
                    }
                }
            });
        }).start();
    }

    private void addMessageBubbleWithTime(String msg, String align, Color bgColor, Color textColor,
            java.sql.Timestamp ts) {
        java.time.LocalDateTime ldt = ts.toLocalDateTime();
        String time = String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());

        JPanel bubbleContainer = new JPanel();
        bubbleContainer.setLayout(new FlowLayout("left".equals(align) ? FlowLayout.LEFT : FlowLayout.RIGHT));
        bubbleContainer.setBackground(new Color(54, 57, 63));
        bubbleContainer.setMaximumSize(new Dimension(500, 100));

        JPanel bubble = new JPanel();
        bubble.setBackground(bgColor);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel messageLabel = new JLabel(msg);
        messageLabel.setForeground(textColor);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setMaximumSize(new Dimension(300, 50));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(messageLabel);

        trackMessageLabel(messageLabel);

        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(new Color(102, 102, 102));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(Box.createVerticalStrut(6));
        bubble.add(timeLabel);
        ensureReactionStrip(bubble, messageLabel);

        attachMessageEditor(bubble, messageLabel, "right".equals(align));

        bubbleContainer.add(bubble);
        messagesPanel.add(bubbleContainer);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    private ImageIcon getEmojiImage(String emojiName) {
        // Map emoji names to image paths
        String imagePath = null;
        switch (emojiName) {
            case "like32":
                imagePath = "/image/like32.png";
                break;
            case "love32":
                imagePath = "/image/love32.png";
                break;
            case "smile32":
                imagePath = "/image/smile32.png";
                break;
            case "sad32":
                imagePath = "/image/sad32.png";
                break;
            case "img2":
                imagePath = "/image/img2.png";
                break;
            default:
                return null;
        }

        try {
            return new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource(imagePath)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addEmojiMessageBubble(String emojiName, String align) {
        String time = String.format("%02d:%02d", LocalDateTime.now().getHour(), LocalDateTime.now().getMinute());

        // Determine colors based on alignment (right=sender/blue, left=recipient/white)
        boolean isSender = "right".equals(align);
        Color bgColor = isSender ? new Color(0, 132, 255) : new Color(255, 255, 255);
        Color timeColor = new Color(102, 102, 102);

        JPanel bubbleContainer = new JPanel();
        bubbleContainer.setLayout(new FlowLayout("left".equals(align) ? FlowLayout.LEFT : FlowLayout.RIGHT, 5, 5));
        bubbleContainer.setBackground(new Color(54, 57, 63));
        bubbleContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel bubble = new JPanel();
        bubble.setBackground(bgColor);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        bubble.setPreferredSize(new Dimension(100, 120));

        // Add emoji image (larger size)
        ImageIcon emoji = getEmojiImage(emojiName);
        if (emoji != null) {
            Image scaledImg = emoji.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            JLabel emojiLabel = new JLabel(new ImageIcon(scaledImg));
            emojiLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emojiLabel.setMaximumSize(new Dimension(64, 64));
            bubble.add(Box.createVerticalGlue());
            bubble.add(emojiLabel);
            bubble.add(Box.createVerticalGlue());
        }

        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(timeColor);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel.setMaximumSize(new Dimension(100, 16));
        bubble.add(timeLabel);

        bubbleContainer.add(bubble);
        messagesPanel.add(bubbleContainer);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    private void addEmojiMessageBubbleWithTime(String emojiName, String align, java.sql.Timestamp ts) {
        java.time.LocalDateTime ldt = ts.toLocalDateTime();
        String time = String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());

        // Determine colors based on alignment (right=sender/blue, left=recipient/white)
        boolean isSender = "right".equals(align);
        Color bgColor = isSender ? new Color(0, 132, 255) : new Color(255, 255, 255);
        Color timeColor = new Color(102, 102, 102);

        JPanel bubbleContainer = new JPanel();
        bubbleContainer.setLayout(new FlowLayout("left".equals(align) ? FlowLayout.LEFT : FlowLayout.RIGHT, 5, 5));
        bubbleContainer.setBackground(new Color(54, 57, 63));
        bubbleContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel bubble = new JPanel();
        bubble.setBackground(bgColor);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        bubble.setPreferredSize(new Dimension(100, 120));

        // Add emoji image (larger size)
        ImageIcon emoji = getEmojiImage(emojiName);
        if (emoji != null) {
            Image scaledImg = emoji.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            JLabel emojiLabel = new JLabel(new ImageIcon(scaledImg));
            emojiLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emojiLabel.setMaximumSize(new Dimension(64, 64));
            bubble.add(Box.createVerticalGlue());
            bubble.add(emojiLabel);
            bubble.add(Box.createVerticalGlue());
        }

        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(timeColor);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel.setMaximumSize(new Dimension(100, 16));
        bubble.add(timeLabel);

        bubbleContainer.add(bubble);
        messagesPanel.add(bubbleContainer);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                chat.sendMessage(Encode.sendFile(selectedFile.getName()));
                chat.sendFile(selectedFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addReceiveLabel() {
        lblReceive = new JLabel("");
        lblReceive.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblReceive.setBounds(47, 529, 465, 29);
        lblReceive.setVisible(false);
        contentPane.add(lblReceive);
    }

    // ============ VIDEO CALL METHODS ============

    private void startVideoCall() {
        if (isVideoCallActive) {
            stopVideoCall();
            return;
        }

        try {
            // Create video call window
            createVideoWindow();

            // Notify the peer that we're initiating a video call
            String videoCallMsg = "<VIDEO_CALL_START>";
            chat.sendMessage(videoCallMsg);

            // Initialize video socket (use different port than voice)
            int videoPort = portVoice + 1;
            videoSocket = new java.net.DatagramSocket(videoPort);

            // Start sending and receiving video
            videoSendThread = new VideoSendThread(videoSocket,
                    socketChat.getInetAddress(), videoPort, localVideoLabel);
            videoReceiveThread = new VideoReceiveThread(videoSocket, remoteVideoLabel);

            // Set error callbacks
            videoSendThread.setErrorCallback(error -> {
                SwingUtilities.invokeLater(() -> {
                    updateChat_notify("❌ Camera error: " + error);
                    stopVideoCall();
                });
            });

            videoReceiveThread.setErrorCallback(error -> {
                SwingUtilities.invokeLater(() -> {
                    updateChat_notify("❌ Video receive error: " + error);
                    stopVideoCall();
                });
            });

            videoSendThread.start();
            videoReceiveThread.start();

            isVideoCallActive = true;
            updateChat_notify("📹 Video call started with " + nameGuest);
            System.out.println("[ChatFrame] Video call initiated");
        } catch (Exception e) {
            e.printStackTrace();
            updateChat_notify("❌ Failed to start video call: " + e.getMessage());
            if (videoFrame != null) {
                videoFrame.dispose();
            }
        }
    }

    private void stopVideoCall() {
        try {
            if (videoSendThread != null) {
                videoSendThread.stopVideo();
                videoSendThread = null;
            }

            if (videoReceiveThread != null) {
                videoReceiveThread.stopVideo();
                videoReceiveThread = null;
            }

            if (videoSocket != null && !videoSocket.isClosed()) {
                videoSocket.close();
                videoSocket = null;
            }

            if (videoFrame != null) {
                videoFrame.dispose();
                videoFrame = null;
            }

            isVideoCallActive = false;
            updateChat_notify("📹 Video call ended");

            // Notify peer that call is ended
            chat.sendMessage("<VIDEO_CALL_END>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createVideoWindow() {
        videoFrame = new JFrame("Video Call with " + nameGuest);
        videoFrame.setSize(760, 560);
        videoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        videoFrame.setLayout(new BorderLayout());

        // Simple main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(35, 35, 40));
        videoFrame.add(mainPanel, BorderLayout.CENTER);

        // Remote video area
        JPanel remotePanel = new JPanel(null);
        remotePanel.setPreferredSize(new Dimension(740, 460));
        remotePanel.setBackground(new Color(25, 25, 30));
        remotePanel.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 80), 2));
        mainPanel.add(remotePanel, BorderLayout.CENTER);

        remoteVideoLabel = new JLabel();
        remoteVideoLabel.setBounds(0, 0, 740, 460);
        remoteVideoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        remoteVideoLabel.setVerticalAlignment(SwingConstants.CENTER);
        remoteVideoLabel.setForeground(new Color(200, 200, 210));
        remoteVideoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        remoteVideoLabel.setText("Connecting to " + nameGuest + "...");
        remotePanel.add(remoteVideoLabel);

        // Local preview at bottom-right
        localVideoLabel = new JLabel();
        localVideoLabel.setBounds(740 - 190, 460 - 140, 180, 120);
        localVideoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        localVideoLabel.setVerticalAlignment(SwingConstants.CENTER);
        localVideoLabel.setForeground(new Color(220, 220, 220));
        localVideoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        localVideoLabel.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 220), 2));
        localVideoLabel.setText("You");
        localVideoLabel.setOpaque(true);
        localVideoLabel.setBackground(new Color(30, 30, 35));
        remotePanel.add(localVideoLabel);

        // Bottom controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        controlPanel.setBackground(new Color(35, 35, 40));
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        JButton endCallButton = new JButton("End Call");
        endCallButton.setPreferredSize(new Dimension(140, 40));
        endCallButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        endCallButton.setBackground(new Color(210, 60, 60));
        endCallButton.setForeground(Color.WHITE);
        endCallButton.setFocusPainted(false);
        endCallButton.addActionListener(e -> stopVideoCall());
        controlPanel.add(endCallButton);

        videoFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopVideoCall();
            }
        });

        videoFrame.setLocationRelativeTo(this);
        videoFrame.setVisible(true);
    }

    // ============ CHAT ROOM THREAD ============

    public class ChatRoom extends Thread {
        private Socket connect;
        private ObjectOutputStream outPeer;
        private ObjectInputStream inPeer;
        private boolean finishReceive = false;
        private long sizeReceiveBytes = 0;
        private String nameFileReceive = "";
        private FileOutputStream fileReceiveStream;
        private File fileReceiveTemp;

        public ChatRoom(Socket connection, String name, String guest) {
            this.connect = connection;
        }

        @Override
        public void run() {
            System.out.println("Chat Room start");

            try {
                // Initialize both streams once at the start
                // Important: Output stream must be created first to write stream header
                outPeer = new ObjectOutputStream(connect.getOutputStream());
                outPeer.flush();

                inPeer = new ObjectInputStream(connect.getInputStream());

                while (!isStop) {
                    try {
                        handleIncomingMessage();
                    } catch (Exception e) {
                        e.printStackTrace();
                        cleanupIncompleteFile();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleIncomingMessage() throws Exception {
            Object obj = inPeer.readObject();

            if (obj instanceof String) {
                String msgObj = obj.toString();
                handleStringMessage(msgObj);
            } else if (obj instanceof DataFile) {
                DataFile data = (DataFile) obj;
                writeIncomingData(data);
            }
        }

        private void writeIncomingData(DataFile data) throws IOException {
            if (fileReceiveStream == null) {
                // No active file transfer; ignore stray data
                return;
            }

            fileReceiveStream.write(data.data, 0, data.data.length);
            sizeReceiveBytes += data.data.length;
            if (lblReceive != null) {
                lblReceive.setText("Receiving... " + Math.max(1, sizeReceiveBytes / 1024) + " KB");
            }
        }

        private void handleStringMessage(String msgObj) throws Exception {
            if (msgObj.startsWith("<SESSION_ACCEPT>")) {
                handleSessionAccept(msgObj);
            } else if (msgObj.equals(Tags.CHAT_CLOSE_TAG)) {
                handleChatClose();
            } else if (Decode.isEdit(msgObj)) {
                handleEditMessage(msgObj);
            } else if (Decode.isTyping(msgObj)) {
                handleTypingMessage(msgObj);
            } else if (Decode.isReaction(msgObj)) {
                handleReactionMessage(msgObj);
            } else if (Decode.isDelete(msgObj)) {
                handleDeleteMessage(msgObj);
            } else if (msgObj.equals(Tags.CHAT_CLEAR_TAG)) {
                handleClearChat();
            } else if (msgObj.equals("<VIDEO_CALL_START>")) {
                handleVideoCallStart();
            } else if (msgObj.equals("<VIDEO_CALL_END>")) {
                handleVideoCallEnd();
            } else if (Decode.checkFile(msgObj)) {
                handleFileRequest(msgObj);
            } else if (Decode.checkFeedBack(msgObj)) {
                handleFileFeedback();
            } else if (msgObj.equals(Tags.FILE_DATA_BEGIN_TAG)) {
                handleFileDataBegin();
            } else if (msgObj.equals(Tags.FILE_DATA_CLOSE_TAG)) {
                handleFileDataClose();
            } else {
                String message = Decode.getMessage(msgObj);
                if (message != null && !message.isEmpty()) {
                    updateChat_receive(message);
                    MessageDAO.saveMessage(nameGuest, nameUser, message);
                } else {
                    System.out.println("Failed to decode message: " + msgObj);
                }
            }
        }

        private void handleSessionAccept(String msgObj) throws Exception {
            System.out.println("SESSION_ACCEPT received");
            // Parse peers from message
            String[] peers = msgObj.split("<PEER>");
            for (String p : peers) {
                if (p.contains("<PEER_NAME>") && p.contains("<IP>") && p.contains("<PORT>")) {
                    String name = extractXmlValue(p, "PEER_NAME");
                    String ip = extractXmlValue(p, "IP").replace("/", "").trim();
                    int port = Integer.parseInt(extractXmlValue(p, "PORT"));
                    System.out.println("Peer found: " + name + " @ " + ip + ":" + port);
                }
            }
        }

        private String extractXmlValue(String xml, String tag) {
            return xml.split("<" + tag + ">")[1].split("</" + tag + ">")[0];
        }

        private void handleVideoCallStart() {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (!isVideoCallActive) {
                        // Auto-accept video call from peer
                        updateChat_notify("📹 " + nameGuest + " is calling with video...");

                        // Create video call window
                        createVideoWindow();

                        // Initialize video socket
                        int videoPort = portVoice + 1;
                        videoSocket = new java.net.DatagramSocket(videoPort);

                        // Start sending and receiving video
                        videoSendThread = new VideoSendThread(videoSocket,
                                socketChat.getInetAddress(), videoPort, localVideoLabel);
                        videoReceiveThread = new VideoReceiveThread(videoSocket, remoteVideoLabel);

                        // Set error callbacks
                        videoSendThread.setErrorCallback(error -> {
                            SwingUtilities.invokeLater(() -> {
                                updateChat_notify("❌ Camera error: " + error);
                                stopVideoCall();
                            });
                        });

                        videoReceiveThread.setErrorCallback(error -> {
                            SwingUtilities.invokeLater(() -> {
                                updateChat_notify("❌ Video receive error: " + error);
                                stopVideoCall();
                            });
                        });

                        videoSendThread.start();
                        videoReceiveThread.start();

                        isVideoCallActive = true;
                        updateChat_notify("📹 Video call connected");
                        System.out.println("[ChatFrame] Video call accepted");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    updateChat_notify("❌ Failed to accept video call: " + e.getMessage());
                    if (videoFrame != null) {
                        videoFrame.dispose();
                    }
                }
            });
        }

        private void handleVideoCallEnd() {
            SwingUtilities.invokeLater(() -> {
                if (isVideoCallActive) {
                    stopVideoCall();
                    updateChat_notify("📹 " + nameGuest + " ended the video call");
                }
            });
        }

        private void handleEditMessage(String msgObj) {
            Decode.EditPayload payload = Decode.getEditPayload(msgObj);
            if (payload == null) {
                return;
            }

            String newDisplay = payload.newText() + " (edited)";

            SwingUtilities.invokeLater(() -> {
                boolean updated = updateLabelText(payload.oldText(), newDisplay);
                if (!updated) {
                    updateChat_notify(nameGuest + " edited a message: " + payload.newText());
                }
            });

            MessageDAO.updateMessage(nameGuest, nameUser, payload.oldText(), newDisplay);
        }

        private void handleDeleteMessage(String msgObj) {
            Decode.DeletePayload payload = Decode.getDeletePayload(msgObj);
            if (payload == null) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                boolean removed = deleteLabelText(payload.text());
                if (!removed) {
                    updateChat_notify(nameGuest + " deleted a message");
                }
            });

            MessageDAO.deleteMessage(nameGuest, nameUser, payload.text());
        }

        private void handleReactionMessage(String msgObj) {
            Decode.ReactionPayload payload = Decode.getReactionPayload(msgObj);
            if (payload == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> {
                // Find label by target text
                for (JLabel label : new ArrayList<>(messageLabels)) {
                    String normalized = stripEditedSuffix(label.getText());
                    if (normalized.equals(payload.target())) {
                        applyReaction(label, payload.emoji());
                        return;
                    }
                }
                updateChat_notify(nameGuest + " reacted: " + payload.emoji());
            });
        }

        private void handleClearChat() {
            SwingUtilities.invokeLater(() -> {
                messageLabels.clear();
                messagesPanel.removeAll();
                messagesPanel.revalidate();
                messagesPanel.repaint();
            });
            MessageDAO.deleteConversation(nameGuest, nameUser);
        }

        private void handleTypingMessage(String msgObj) {
            Decode.TypingPayload payload = Decode.getTypingPayload(msgObj);
            if (payload == null) {
                return;
            }
            SwingUtilities.invokeLater(() -> setPeerTyping(payload.on()));
        }

        // (moved) reaction helpers live in outer class

        private void handleChatClose() throws Exception {
            isStop = true;
            Tags.show(ChatFrame.this, nameGuest + " This window will close.", false);
            dispose();
            chat.stopChat();
        }

        private void handleFileRequest(String msgObj) throws Exception {
            nameFileReceive = msgObj.substring(10, msgObj.length() - 11);
            finishReceive = false;
            sizeReceiveBytes = 0;

            fileReceiveTemp = new File(URL_DIR, nameFileReceive);
            if (fileReceiveTemp.getParentFile() != null && !fileReceiveTemp.getParentFile().exists()) {
                fileReceiveTemp.getParentFile().mkdirs();
            }

            try {
                fileReceiveStream = new FileOutputStream(fileReceiveTemp, false);
            } catch (IOException io) {
                Tags.show(ChatFrame.this, "Cannot prepare file for receiving: " + io.getMessage(), true);
                return;
            }

            String ack = Tags.FILE_REQ_ACK_OPEN_TAG + Integer.toBinaryString(port) +
                    Tags.FILE_REQ_ACK_CLOSE_TAG;
            sendMessage(ack);
        }

        private void handleFileFeedback() throws Exception {
            btnSendFile.setEnabled(false);
            new Thread(() -> {
                try {
                    sendMessage(Tags.FILE_DATA_BEGIN_TAG);
                    updateChat_notify("Sending file: " + nameFileReceive);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void handleFileDataBegin() throws Exception {
            finishReceive = false;
            lblReceive.setVisible(true);
            lblReceive.setText("Receiving...");
        }

        private void handleFileDataClose() throws Exception {
            closeFileReceiveStream();
            long sizeKb = Math.max(1, sizeReceiveBytes / 1024);
            updateChat_receive("File received: " + nameFileReceive + " (" + sizeKb + " KB)");
            sizeReceiveBytes = 0;
            lblReceive.setVisible(false);

            new Thread(this::showSaveFileDialog).start();
            finishReceive = true;
        }

        private void closeFileReceiveStream() {
            if (fileReceiveStream != null) {
                try {
                    fileReceiveStream.flush();
                    fileReceiveStream.close();
                } catch (IOException ignored) {
                    // Ignore cleanup exceptions
                }
                fileReceiveStream = null;
            }
        }

        private void cleanupIncompleteFile() {
            closeFileReceiveStream();
            if (fileReceiveTemp != null && fileReceiveTemp.exists() && !finishReceive) {
                fileReceiveTemp.delete();
            }
        }

        public void sendFile(File file) throws Exception {
            btnSendFile.setEnabled(false);
            lblReceive.setVisible(true);
            progressBar.setVisible(true);

            long fileSize = file.length();
            int chunks = (int) ((fileSize + 1023) / 1024); // Ceiling division

            if (chunks > Tags.MAX_MSG_SIZE / 1024) {
                lblReceive.setText("File is too large...");
                sendMessage(Tags.FILE_DATA_CLOSE_TAG);
                btnSendFile.setEnabled(true);
                lblReceive.setVisible(false);
                return;
            }

            progressBar.setValue(0);
            lblReceive.setText("Sending...");

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int chunkCount = 0;

                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    DataFile dataFile = new DataFile(bytesRead);
                    System.arraycopy(buffer, 0, dataFile.data, 0, bytesRead);
                    sendMessage(dataFile);

                    chunkCount++;
                    progressBar.setValue((chunkCount * 100) / chunks);
                }

                sendMessage(Tags.FILE_DATA_CLOSE_TAG);
                updateChat_notify("File sent successfully");
            } finally {
                progressBar.setVisible(false);
                lblReceive.setVisible(false);
                btnSendFile.setEnabled(true);
            }
        }

        private void showSaveFileDialog() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = fileChooser.showSaveDialog(ChatFrame.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File destinationFile = new File(fileChooser.getSelectedFile().getAbsolutePath() +
                        "/" + nameFileReceive);
                if (!destinationFile.exists()) {
                    try {
                        Thread.sleep(1000);
                        frameChat.copyFileReceive(
                                new FileInputStream(fileReceiveTemp.getAbsolutePath()),
                                new FileOutputStream(destinationFile.getAbsolutePath()),
                                fileReceiveTemp.getAbsolutePath());
                    } catch (Exception e) {
                        Tags.show(ChatFrame.this, "Error receiving file!", false);
                    }
                } else {
                    int overwrite = Tags.show(ChatFrame.this, "File exists. Overwrite?", true);
                    if (overwrite != 0)
                        showSaveFileDialog(); // Retry if not confirmed
                }
            }
        }

        public synchronized void sendMessage(Object obj) throws Exception {
            if (outPeer != null) {
                outPeer.writeObject(obj);
                outPeer.flush();
            }
        }

        public void stopChat() {
            try {
                connect.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}