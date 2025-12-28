package client;

import database.GroupDAO;
import database.GroupDAO.GroupMessage;
import database.UserDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupChatFrame extends JFrame {

    private static final Color CHAT_BACKGROUND = new Color(228, 231, 237);
    private static final Color BUBBLE_ME = new Color(62, 133, 247);
    private static final Color BUBBLE_PEER = Color.WHITE;
    private static final Color BUBBLE_BORDER = new Color(200, 210, 225);
    private static final Color TEXT_PRIMARY = new Color(33, 37, 43);
    private static final Color TEXT_MUTED = new Color(122, 134, 150);

    private JPanel contentPane;
    private JPanel messagesPanel;
    private JTextArea txtMessage;
    private JLabel lblGroupName;
    private JList<String> membersList;
    private DefaultListModel<String> membersModel;
    private JScrollPane scrollPane;

    private int groupId;
    private String groupName;
    private String currentUser;
    private String groupCreator; // Track group creator for permission checks
    private int lastMessageId = 0; // Track last seen message
    private javax.swing.Timer pollTimer; // Poll for new messages

    // Message tracking for edit/delete/react functionality
    private final List<JLabel> messageLabels = new ArrayList<>();
    private final Map<JLabel, Integer> messageLabelIds = new HashMap<>(); // Maps JLabel to messageId
    private final Map<JLabel, JPanel> reactionStrips = new HashMap<>();
    private final Map<JLabel, Map<String, Integer>> reactionCounts = new HashMap<>();
    private final Map<JLabel, String> messageSenders = new HashMap<>(); // Track who sent each message
    private final Map<Integer, JLabel> statusLabels = new HashMap<>(); // messageId -> status label
    private final Map<Integer, String> statusCache = new HashMap<>(); // messageId -> status text
    private final Map<String, java.sql.Timestamp> memberActivity = new HashMap<>();
    private javax.swing.Timer activityTimer;
    private boolean muted = false;
    private long snoozeUntil = 0L;
    private static final java.util.Map<Integer, Long> groupSnoozeStore = new java.util.HashMap<>();

    // Typing indicator
    private JLabel typingLabel;
    private boolean typingOnSent = false;
    private javax.swing.Timer typingTimer;
    private final Set<String> usersTyping = new HashSet<>();
    private JButton muteButton;

    public GroupChatFrame(int groupId, String groupName, String currentUser, Client client) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.currentUser = currentUser;
        this.snoozeUntil = groupSnoozeStore.getOrDefault(groupId, 0L);

        // Load group creator from database
        loadGroupCreator();

        initializeUI();
        updateMuteState();
        loadGroupHistory();
        loadGroupMembers();
        startMessagePolling(); // Start polling for new messages
        startActivityPolling(); // Start polling member activity
        setVisible(true);
    }

    private void initializeUI() {

        setTitle("Group: " + groupName);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 800, 650);
        setLocationRelativeTo(null);

        contentPane = new JPanel();
        contentPane.setBackground(CHAT_BACKGROUND);
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setLayout(new BorderLayout(10, 10));
        setContentPane(contentPane);

        // Top panel - group name
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.WHITE);
        topPanel.setPreferredSize(new Dimension(800, 50));
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BUBBLE_BORDER));

        lblGroupName = new JLabel("🗨️ " + groupName);
        lblGroupName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblGroupName.setForeground(TEXT_PRIMARY);
        topPanel.add(lblGroupName);

        muteButton = new JButton("🔔");
        muteButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        muteButton.setBackground(new Color(243, 246, 250));
        muteButton.setForeground(TEXT_PRIMARY);
        muteButton.setFocusPainted(false);
        muteButton.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        muteButton.addActionListener(e -> showMuteMenu(muteButton));
        topPanel.add(Box.createHorizontalStrut(8));
        topPanel.add(muteButton);
        contentPane.add(topPanel, BorderLayout.NORTH);

        // Main panel - messages + members
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0));
        mainPanel.setBackground(CHAT_BACKGROUND);

        // Messages panel
        messagesPanel = new JPanel();
        messagesPanel.setBackground(CHAT_BACKGROUND);
        messagesPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CHAT_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Typing indicator label
        typingLabel = new JLabel("");
        typingLabel.setForeground(new Color(62, 133, 247));
        typingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        typingLabel.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        typingLabel.setVisible(false);
        mainPanel.add(typingLabel, BorderLayout.SOUTH);

        // Members panel (right side)
        JPanel membersPanel = new JPanel(new BorderLayout());
        membersPanel.setBackground(new Color(47, 49, 54));
        membersPanel.setPreferredSize(new Dimension(180, 0));
        membersPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(32, 34, 37), 1),
                "Members",
                0,
                0,
                new Font("Segoe UI", Font.BOLD, 12),
                Color.LIGHT_GRAY));

        membersModel = new DefaultListModel<>();
        membersList = new JList<>(membersModel);
        membersList.setBackground(new Color(47, 49, 54));
        membersList.setForeground(Color.WHITE);
        membersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        membersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        membersList.setCellRenderer(new MemberStatusRenderer());

        JScrollPane membersScroll = new JScrollPane(membersList);
        membersScroll.setBorder(null);
        membersPanel.add(membersScroll, BorderLayout.CENTER);

        // Add Members button
        JButton btnAddMember = new JButton("+ Add");
        btnAddMember.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnAddMember.setBackground(new Color(88, 101, 242));
        btnAddMember.setForeground(Color.WHITE);
        btnAddMember.setFocusPainted(false);
        btnAddMember.addActionListener(e -> showAddMemberDialog());

        // Remove Member button (only visible to creator)
        JButton btnRemoveMember = new JButton("- Remove");
        btnRemoveMember.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRemoveMember.setBackground(new Color(210, 60, 60));
        btnRemoveMember.setForeground(Color.WHITE);
        btnRemoveMember.setFocusPainted(false);
        btnRemoveMember.addActionListener(e -> removeSelectedMember());
        btnRemoveMember.setVisible(false); // Initially hidden until creator is loaded

        // Delete Group button (only for creator)
        JButton btnDeleteGroup = new JButton("Delete Group");
        btnDeleteGroup.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDeleteGroup.setBackground(new Color(180, 50, 50));
        btnDeleteGroup.setForeground(Color.WHITE);
        btnDeleteGroup.setFocusPainted(false);
        btnDeleteGroup.addActionListener(e -> deleteGroup());
        btnDeleteGroup.setVisible(false);

        JPanel buttonsRow = new JPanel(new BorderLayout());
        buttonsRow.setBackground(new Color(47, 49, 54));
        buttonsRow.add(btnAddMember, BorderLayout.WEST);
        buttonsRow.add(btnRemoveMember, BorderLayout.EAST);

        JPanel addButtonPanel = new JPanel();
        addButtonPanel.setLayout(new BoxLayout(addButtonPanel, BoxLayout.Y_AXIS));
        addButtonPanel.setBackground(new Color(47, 49, 54));
        addButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        addButtonPanel.add(buttonsRow);
        addButtonPanel.add(Box.createVerticalStrut(6));
        addButtonPanel.add(btnDeleteGroup);

        // Store reference to remove/delete buttons so we can show/hide based on
        // permissions
        membersList.putClientProperty("removeButton", btnRemoveMember);
        membersList.putClientProperty("deleteGroupButton", btnDeleteGroup);

        membersPanel.add(addButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(membersPanel, BorderLayout.EAST);
        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Bottom panel - input
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(CHAT_BACKGROUND);
        inputPanel.setPreferredSize(new Dimension(800, 80));

        txtMessage = new JTextArea(3, 30);
        txtMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        txtMessage.setBackground(Color.WHITE);
        txtMessage.setForeground(TEXT_PRIMARY);
        txtMessage.setCaretColor(TEXT_PRIMARY);
        txtMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUBBLE_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JScrollPane txtScroll = new JScrollPane(txtMessage);
        txtScroll.setBorder(null);
        inputPanel.add(txtScroll, BorderLayout.CENTER);

        JButton btnSend = new JButton("Send");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.setBackground(new Color(62, 133, 247));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setPreferredSize(new Dimension(100, 80));
        btnSend.addActionListener(e -> sendGroupMessage());
        inputPanel.add(btnSend, BorderLayout.EAST);

        // Enter key to send
        txtMessage.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && !evt.isShiftDown()) {
                    evt.consume();
                    sendGroupMessage();
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                handleTypingKeyPress();
            }
        });

        contentPane.add(inputPanel, BorderLayout.SOUTH);
    }

    private void loadGroupHistory() {
        new Thread(() -> {
            List<GroupMessage> messages = GroupDAO.loadGroupMessages(groupId);
            SwingUtilities.invokeLater(() -> {
                for (GroupMessage msg : messages) {
                    boolean isSender = msg.sender().equals(currentUser);
                    addMessageBubbleWithTime(
                            msg.messageId(),
                            msg.sender(),
                            msg.content(),
                            isSender ? "right" : "left",
                            isSender ? new Color(0, 132, 255) : new Color(255, 255, 255),
                            isSender ? Color.WHITE : Color.BLACK,
                            msg.createdAt(),
                            isSender);

                    // Track the last message ID
                    if (msg.messageId() > lastMessageId) {
                        lastMessageId = msg.messageId();
                    }
                }
                scrollToBottom();
                refreshOwnMessageStatuses();
            });

            // Mark displayed messages from others as read and advance read pointer
            new Thread(() -> {
                for (GroupMessage msg : messages) {
                    if (!msg.sender().equals(currentUser)) {
                        GroupDAO.saveMessageStatus(msg.messageId(), currentUser, "read");
                    }
                }
                if (!messages.isEmpty()) {
                    GroupDAO.setReadPointer(groupId, currentUser, lastMessageId);
                }
            }).start();
        }).start();
    }

    /**
     * Loads the group creator from the database and shows/hides remove button
     * accordingly.
     */
    private void loadGroupCreator() {
        new Thread(() -> {
            GroupDAO.GroupInfo groupInfo = GroupDAO.getGroupInfo(groupId);
            if (groupInfo != null) {
                groupCreator = groupInfo.creator();

                // Show creator-only controls
                SwingUtilities.invokeLater(() -> {
                    boolean isCreator = currentUser.equals(groupCreator);
                    JButton removeButton = (JButton) membersList.getClientProperty("removeButton");
                    if (removeButton != null) {
                        removeButton.setVisible(isCreator);
                    }
                    JButton deleteGroupButton = (JButton) membersList.getClientProperty("deleteGroupButton");
                    if (deleteGroupButton != null) {
                        deleteGroupButton.setVisible(isCreator);
                    }
                });
            }
        }).start();
    }

    private void startMessagePolling() {
        // Poll for new messages every 2 seconds
        pollTimer = new javax.swing.Timer(2000, e -> checkForNewMessages());
        pollTimer.start();

        // Stop polling when window closes
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (pollTimer != null) {
                    pollTimer.stop();
                }
            }
        });
    }

    private void checkForNewMessages() {
        new Thread(() -> {
            List<GroupMessage> allMessages = GroupDAO.loadGroupMessages(groupId);

            // Find messages newer than lastMessageId
            List<GroupMessage> newMessages = new ArrayList<>();
            for (GroupMessage msg : allMessages) {
                if (msg.messageId() > lastMessageId && !msg.sender().equals(currentUser)) {
                    newMessages.add(msg);
                }
            }

            if (!newMessages.isEmpty()) {
                List<GroupMessage> mentionHits = new ArrayList<>();
                for (GroupMessage msg : newMessages) {
                    if (containsMentionForCurrentUser(msg.content())) {
                        mentionHits.add(msg);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    for (GroupMessage msg : newMessages) {
                        addMessageBubbleWithTime(
                                msg.messageId(),
                                msg.sender(),
                                msg.content(),
                                "left",
                                new Color(255, 255, 255),
                                Color.BLACK,
                                msg.createdAt(),
                                false);

                        if (msg.messageId() > lastMessageId) {
                            lastMessageId = msg.messageId();
                        }
                    }
                    scrollToBottom();
                });

                // Notify for mentions only
                for (GroupMessage msg : mentionHits) {
                    if (!isMutedNow()) {
                        showMentionAlert(msg.sender(), msg.content());
                    }
                }

                // Mark as read and refresh statuses in background
                new Thread(() -> {
                    for (GroupMessage msg : newMessages) {
                        GroupDAO.saveMessageStatus(msg.messageId(), currentUser, "read");
                        GroupDAO.setReadPointer(groupId, currentUser, msg.messageId());
                    }
                    refreshOwnMessageStatuses();
                }).start();
            }
        }).start();
    }

    private void loadGroupMembers() {
        new Thread(() -> {
            List<String> members = GroupDAO.getGroupMembers(groupId);
            Map<String, java.sql.Timestamp> activity = GroupDAO.getMemberActivity(groupId);
            SwingUtilities.invokeLater(() -> {
                membersModel.clear();
                for (String member : members) {
                    membersModel.addElement(member);
                }
                memberActivity.clear();
                if (activity != null) {
                    memberActivity.putAll(activity);
                }
                membersList.repaint();
            });
        }).start();
    }

    private void sendGroupMessage() {
        String content = txtMessage.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        txtMessage.setText("");

        try {
            // Get all group members
            List<String> members = GroupDAO.getGroupMembers(groupId);

            // Note: Group messages are stored in database and distributed via polling
            // (not sent via P2P in this simplified implementation)

            for (String member : members) {
                if (!member.equals(currentUser)) {
                    // In production, you would send to each member's P2P connection
                    // For now, polling handles distribution via database
                }
            }

            // Save to database and get message id
            int messageId = GroupDAO.saveGroupMessageAndGetId(groupId, currentUser, content);

            if (messageId > 0) {
                // Set initial statuses
                GroupDAO.saveMessageStatus(messageId, currentUser, "sent");
                for (String member : members) {
                    if (!member.equals(currentUser)) {
                        GroupDAO.saveMessageStatus(messageId, member, "delivered");
                    }
                }
                GroupDAO.setReadPointer(groupId, currentUser, messageId);

                // Display locally with status tracking
                java.sql.Timestamp nowTs = new java.sql.Timestamp(System.currentTimeMillis());
                addMessageBubbleWithTime(
                        messageId,
                        currentUser,
                        content,
                        "right",
                        new Color(0, 132, 255),
                        Color.WHITE,
                        nowTs,
                        true);
                statusCache.put(messageId, "sent");
                updateStatusLabelForMessage(messageId, "sent");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to send message: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        ;
    }

    public void notifyMemberJoined(String member) {
        SwingUtilities.invokeLater(() -> {
            if (!membersModel.contains(member)) {
                membersModel.addElement(member);
            }
            addSystemMessage(member + " joined the group");
        });
    }

    private void addMessageBubble(String sender, String content, String align, Color bgColor, Color textColor) {
        LocalDateTime now = LocalDateTime.now();
        String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
        boolean fromSender = "right".equals(align);
        addStyledBubble(-1, sender, content, fromSender, time, containsMentionForCurrentUser(content));
    }

    private void addMessageBubbleWithTime(int messageId, String sender, String content, String align,
            Color bgColor, Color textColor, java.sql.Timestamp ts, boolean isSender) {
        java.time.LocalDateTime ldt = ts.toLocalDateTime();
        String time = String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());
        addStyledBubble(messageId, sender, content, isSender, time, containsMentionForCurrentUser(content));
    }

    private void addStyledBubble(int messageId, String sender, String content, boolean fromSender, String time,
            boolean mentionHit) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBackground(CHAT_BACKGROUND);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));

        JPanel alignPanel = new JPanel(new FlowLayout(fromSender ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 0));
        alignPanel.setOpaque(false);
        row.add(alignPanel, BorderLayout.CENTER);

        Color fill = fromSender ? BUBBLE_ME : BUBBLE_PEER;
        Color stroke = mentionHit ? new Color(255, 209, 102) : (fromSender ? fill.darker() : BUBBLE_BORDER);
        RoundedPanel bubble = new RoundedPanel(18, fill, stroke);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));
        bubble.setOpaque(false);
        bubble.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));

        if (!fromSender) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setForeground(new Color(62, 133, 247));
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            senderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(senderLabel);
            bubble.add(Box.createVerticalStrut(4));
        }

        JLabel messageLabel = new JLabel(buildMessageHtml(content));
        messageLabel.setForeground(fromSender ? Color.WHITE : TEXT_PRIMARY);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));
        trackMessageLabel(messageLabel, messageId, sender);
        bubble.add(messageLabel);

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        meta.setOpaque(false);
        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(fromSender ? new Color(220, 232, 252) : TEXT_MUTED);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        meta.add(timeLabel);

        if (messageId > 0) {
            JLabel status = new JLabel("");
            status.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            status.setForeground(TEXT_MUTED);
            statusLabels.put(messageId, status);
            meta.add(status);
        }

        bubble.add(Box.createVerticalStrut(6));
        bubble.add(meta);

        ensureReactionStrip(bubble, messageLabel);
        attachMessageEditor(bubble, messageLabel, fromSender, sender);

        alignPanel.add(bubble);
        messagesPanel.add(row);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    private void addSystemMessage(String message) {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER));
        container.setBackground(CHAT_BACKGROUND);
        container.setMaximumSize(new Dimension(700, 40));

        JLabel label = new JLabel("• " + message);
        label.setForeground(TEXT_MUTED);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        container.add(label);

        messagesPanel.add(container);
        messagesPanel.revalidate();
        messagesPanel.repaint();
        scrollToBottom();
    }

    private String buildMessageHtml(String raw) {
        String safe = escapeHtml(raw);
        safe = safe.replace("\n", "<br>");
        safe = highlightMentions(safe);
        return "<html><body style='width: 320px'>" + safe + "</body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String highlightMentions(String safeHtml) {
        Pattern p = Pattern.compile("(^|\\s)@([A-Za-z0-9_.-]+)");
        Matcher m = p.matcher(safeHtml);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(2);
            boolean isMe = name.equalsIgnoreCase(currentUser);
            String style = isMe
                    ? "background:#FFD166;color:#1B1B1B;padding:1px 4px;border-radius:6px;"
                    : "background:#2f3136;color:#f5f5f5;padding:1px 4px;border-radius:6px;";
            String replacement = m.group(1) + "<span style='" + style + "'>@" + name + "</span>";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean containsMentionForCurrentUser(String text) {
        if (text == null || currentUser == null)
            return false;
        Pattern p = Pattern.compile("(^|\\s)@" + Pattern.quote(currentUser) + "(?=\\b)", Pattern.CASE_INSENSITIVE);
        return p.matcher(text).find();
    }

    private void showMentionAlert(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            if (isMutedNow())
                return;
            String preview = content;
            if (preview.length() > 140) {
                preview = preview.substring(0, 140) + "...";
            }
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    sender + " mentioned you:\n" + preview,
                    "Mention",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // ============ MUTE / SNOOZE ============

    private void showMuteMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem toggle = new JMenuItem(muted ? "Unmute" : "Mute");
        toggle.addActionListener(e -> {
            muted = !muted;
            if (!muted) {
                snoozeUntil = 0L;
                groupSnoozeStore.remove(groupId);
            }
            updateMuteState();
        });
        menu.add(toggle);

        JMenu snooze = new JMenu("Snooze");
        addSnoozeItem(snooze, "1 hour", 60);
        addSnoozeItem(snooze, "8 hours", 8 * 60);
        addSnoozeItem(snooze, "24 hours", 24 * 60);
        menu.add(snooze);

        menu.addSeparator();
        JMenuItem clear = new JMenuItem("Clear snooze");
        clear.addActionListener(e -> {
            snoozeUntil = 0L;
            groupSnoozeStore.remove(groupId);
            updateMuteState();
        });
        menu.add(clear);

        menu.show(invoker, 0, invoker.getHeight());
    }

    private void addSnoozeItem(JMenu parent, String label, int minutes) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            muted = false;
            snoozeUntil = System.currentTimeMillis() + minutes * 60_000L;
            groupSnoozeStore.put(groupId, snoozeUntil);
            updateMuteState();
        });
        parent.add(item);
    }

    private boolean isMutedNow() {
        long now = System.currentTimeMillis();
        if (muted)
            return true;
        if (snoozeUntil > 0 && now < snoozeUntil)
            return true;
        if (snoozeUntil > 0 && now >= snoozeUntil) {
            snoozeUntil = 0L;
            groupSnoozeStore.remove(groupId);
        }
        return false;
    }

    private void updateMuteState() {
        boolean mutedState = isMutedNow();
        if (muteButton != null) {
            if (mutedState) {
                muteButton.setText("🔕");
                String tip;
                long remainingMs = snoozeUntil - System.currentTimeMillis();
                if (!muted && remainingMs > 0) {
                    long mins = Math.max(1, remainingMs / 60000);
                    tip = "Snoozed (" + mins + "m remaining)";
                } else if (muted) {
                    tip = "Muted";
                } else {
                    tip = "Notifications off";
                }
                muteButton.setToolTipText(tip);
            } else {
                muteButton.setText("🔔");
                muteButton.setToolTipText("Notifications on");
            }
        }
        this.repaint();
    }

    // ============ ACTIVITY (ONLINE / LAST SEEN) ============

    private void startActivityPolling() {
        // Refresh every 30 seconds
        activityTimer = new javax.swing.Timer(30_000, e -> refreshActivity());
        activityTimer.start();
    }

    private void refreshActivity() {
        new Thread(() -> {
            Map<String, java.sql.Timestamp> activity = GroupDAO.getMemberActivity(groupId);
            if (activity == null)
                return;
            SwingUtilities.invokeLater(() -> {
                memberActivity.clear();
                memberActivity.putAll(activity);
                membersList.repaint();
            });
        }).start();
    }

    private String formatLastSeen(java.sql.Timestamp ts) {
        if (ts == null)
            return "Offline";
        long diffMs = System.currentTimeMillis() - ts.getTime();
        long minutes = diffMs / 60000;
        if (minutes < 2)
            return "Online";
        if (minutes < 60)
            return "Last seen " + minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24)
            return "Last seen " + hours + "h ago";
        java.time.LocalDateTime ldt = ts.toLocalDateTime();
        return String.format("Last seen %02d:%02d", ldt.getHour(), ldt.getMinute());
    }

    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color fill;
        private final Color stroke;

        RoundedPanel(int arc, Color fill, Color stroke) {
            this.arc = arc;
            this.fill = fill;
            this.stroke = stroke;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (stroke != null) {
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class MemberStatusRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String user = value == null ? "" : value.toString();
            java.sql.Timestamp ts = memberActivity.get(user);
            String statusText = formatLastSeen(ts);

            boolean online = "Online".equals(statusText);
            String dotColor = online ? "#3BA55D" : "#8A8A8A";
            label.setText("<html><body><span style='color:" + dotColor + ";'>&#9679;</span> " +
                    user + "<br><span style='font-size:10px;color:#BBBBBB;'>" + statusText + "</span></body></html>");
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            if (!isSelected) {
                label.setBackground(new Color(47, 49, 54));
                label.setForeground(Color.WHITE);
            }
            return label;
        }
    }

    // ============ TYPING INDICATOR HELPERS ============

    private void handleTypingKeyPress() {
        try {
            if (!typingOnSent) {
                // Send typing notification to other users (hook into network layer if needed)
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
            typingOnSent = false;
            typingTimer.stop();
        });
        typingTimer.setRepeats(false);
        typingTimer.start();
    }

    /**
     * Called when a user starts typing
     */
    public void notifyUserTyping(String username) {
        usersTyping.add(username);
        updateTypingDisplay();
    }

    /**
     * Called when a user stops typing
     */
    public void notifyUserStoppedTyping(String username) {
        usersTyping.remove(username);
        updateTypingDisplay();
    }

    /**
     * Updates the typing indicator display
     */
    private void updateTypingDisplay() {
        SwingUtilities.invokeLater(() -> {
            if (usersTyping.isEmpty()) {
                typingLabel.setVisible(false);
                typingLabel.setText("");
            } else {
                String typingText;
                if (usersTyping.size() == 1) {
                    typingText = usersTyping.iterator().next() + " is typing...";
                } else if (usersTyping.size() == 2) {
                    String[] users = usersTyping.toArray(new String[0]);
                    typingText = users[0] + " and " + users[1] + " are typing...";
                } else {
                    typingText = usersTyping.size() + " users are typing...";
                }
                typingLabel.setText(typingText);
                typingLabel.setVisible(true);
            }
        });
    }

    // ============ MESSAGE EDITING & REACTION HELPERS ============

    private void trackMessageLabel(JLabel label, int messageId, String sender) {
        messageLabels.add(label);
        messageLabelIds.put(label, messageId);
        messageSenders.put(label, sender);
    }

    private void attachMessageEditor(JPanel bubble, JLabel messageLabel, boolean allowEdit, String sender) {
        if (!allowEdit) {
            return;
        }

        MouseAdapter editor = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String current = messageLabel.getText();

                Object[] options = { "Edit", "Delete", "React", "Copy", "Cancel" };
                int choice = JOptionPane.showOptionDialog(GroupChatFrame.this,
                        "Choose action",
                        "Message actions",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) {
                    // Edit
                    String updated = JOptionPane.showInputDialog(GroupChatFrame.this, "Edit message",
                            stripEditedSuffix(current));
                    if (updated == null) {
                        return;
                    }

                    updated = updated.trim();
                    if (updated.isEmpty() || updated.equals(stripEditedSuffix(current))) {
                        return;
                    }

                    String newDisplay = updated + " (edited)";
                    messageLabel.setText(buildMessageHtml(newDisplay));

                    // Update database
                    int msgId = messageLabelIds.getOrDefault(messageLabel, -1);
                    if (msgId != -1) {
                        new Thread(() -> {
                            GroupDAO.updateGroupMessage(msgId, newDisplay);
                        }).start();
                    }
                } else if (choice == 1) {
                    // Delete
                    int confirm = JOptionPane.showConfirmDialog(GroupChatFrame.this,
                            "Delete this message?",
                            "Confirm delete",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }

                    deleteLocalMessage(messageLabel);
                } else if (choice == 2) {
                    // React
                    String[] reactions = new String[] { "like32", "love32", "smile32", "sad32" };
                    int rChoice = JOptionPane.showOptionDialog(GroupChatFrame.this,
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
                    }
                } else if (choice == 3) {
                    // Copy
                    String textToCopy = stripEditedSuffix(current);
                    // Remove HTML tags for clipboard
                    textToCopy = textToCopy.replaceAll("<[^>]*>", "");
                    try {
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                                .setContents(new StringSelection(textToCopy), null);
                        addSystemMessage("✓ Copied to clipboard");
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

    private String stripEditedSuffix(String text) {
        if (text == null) {
            return "";
        }
        // Remove HTML tags
        String plain = text.replaceAll("<[^>]*>", "");
        String suffix = " (edited)";
        if (plain.endsWith(suffix)) {
            return plain.substring(0, plain.length() - suffix.length());
        }
        return plain;
    }

    private void deleteLocalMessage(JLabel label) {
        String display = label.getText();
        removeBubbleForLabel(label);

        // Delete from database
        int msgId = messageLabelIds.getOrDefault(label, -1);
        if (msgId != -1) {
            new Thread(() -> {
                GroupDAO.deleteGroupMessage(msgId);
            }).start();
        }
    }

    private void removeBubbleForLabel(JLabel label) {
        Container bubble = label.getParent();
        Container container = bubble != null ? bubble.getParent() : null;
        if (container != null && container.getParent() == messagesPanel) {
            messagesPanel.remove(container);
        }
        reactionStrips.remove(label);
        reactionCounts.remove(label);
        messageLabelIds.remove(label);
        messageSenders.remove(label);
        messageLabels.remove(label);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

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

    // ============ MESSAGE STATUS (SENT/DELIVERED/READ) ============

    private void updateStatusLabelForMessage(int messageId, String status) {
        if (messageId <= 0) {
            return;
        }
        statusCache.put(messageId, status);
        JLabel statusLabel = statusLabels.get(messageId);
        if (statusLabel == null) {
            return;
        }

        String text;
        Color color = new Color(150, 150, 150);
        switch (status) {
            case "read":
                text = "✓✓";
                color = new Color(0, 132, 255);
                break;
            case "delivered":
                text = "✓✓";
                break;
            case "sent":
            default:
                text = "✓";
                break;
        }
        statusLabel.setText(text);
        statusLabel.setForeground(color);
        statusLabel.revalidate();
        statusLabel.repaint();
    }

    private void refreshOwnMessageStatuses() {
        new Thread(() -> {
            try {
                // Collect own message IDs
                List<Integer> ownIds = new ArrayList<>();
                for (Map.Entry<JLabel, Integer> e : messageLabelIds.entrySet()) {
                    if (currentUser.equals(messageSenders.get(e.getKey()))) {
                        if (e.getValue() != null && e.getValue() > 0) {
                            ownIds.add(e.getValue());
                        }
                    }
                }
                if (ownIds.isEmpty())
                    return;

                // Load read pointers
                Map<String, Integer> pointers = GroupDAO.getReadPointers(groupId);

                // Compute status per message based on other members' read pointers
                List<String> members = new ArrayList<>();
                for (int i = 0; i < membersModel.size(); i++) {
                    members.add(membersModel.getElementAt(i));
                }

                Map<Integer, String> computed = new HashMap<>();
                for (int msgId : ownIds) {
                    boolean allRead = true;
                    for (String m : members) {
                        if (m.equals(currentUser))
                            continue;
                        int ptr = pointers.getOrDefault(m, 0);
                        if (ptr < msgId) {
                            allRead = false;
                            break;
                        }
                    }
                    if (allRead) {
                        computed.put(msgId, "read");
                    } else {
                        computed.put(msgId, "delivered");
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    for (Map.Entry<Integer, String> entry : computed.entrySet()) {
                        updateStatusLabelForMessage(entry.getKey(), entry.getValue());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private ImageIcon getEmojiImage(String emojiName) {
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
            return new ImageIcon(GroupChatFrame.class.getResource(imagePath));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getGroupId() {
        return groupId;
    }

    /**
     * Opens a dialog to add members to the group from the database.
     */
    private void showAddMemberDialog() {
        new Thread(() -> {
            // Get all users from database
            List<String> allUsers = UserDAO.getAllUsers();

            // Get current group members
            List<String> currentMembers = GroupDAO.getGroupMembers(groupId);

            // Filter out users who are already members
            List<String> availableUsers = new ArrayList<>();
            for (String user : allUsers) {
                if (!currentMembers.contains(user)) {
                    availableUsers.add(user);
                }
            }

            SwingUtilities.invokeLater(() -> {
                if (availableUsers.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "All users are already members of this group.",
                            "No Available Users",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // Create dialog with checkboxes for available users
                JDialog addMemberDialog = new JDialog(this, "Add Members to Group", true);
                addMemberDialog.setSize(400, 500);
                addMemberDialog.setLocationRelativeTo(this);
                addMemberDialog.setLayout(new BorderLayout(10, 10));

                // Title
                JPanel titlePanel = new JPanel();
                titlePanel.setBackground(new Color(47, 49, 54));
                JLabel titleLabel = new JLabel("Select users to add to " + groupName);
                titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                titleLabel.setForeground(Color.WHITE);
                titlePanel.add(titleLabel);
                addMemberDialog.add(titlePanel, BorderLayout.NORTH);

                // User list with checkboxes
                JPanel userListPanel = new JPanel();
                userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
                userListPanel.setBackground(new Color(54, 57, 63));

                List<JCheckBox> checkboxes = new ArrayList<>();
                for (String user : availableUsers) {
                    JCheckBox checkbox = new JCheckBox(user);
                    checkbox.setBackground(new Color(54, 57, 63));
                    checkbox.setForeground(Color.WHITE);
                    checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    checkboxes.add(checkbox);
                    userListPanel.add(checkbox);
                }

                JScrollPane scrollPane = new JScrollPane(userListPanel);
                scrollPane.setBackground(new Color(54, 57, 63));
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                addMemberDialog.add(scrollPane, BorderLayout.CENTER);

                // Button panel
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
                buttonPanel.setBackground(new Color(47, 49, 54));

                JButton btnAdd = new JButton("Add Selected");
                btnAdd.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                btnAdd.setBackground(new Color(88, 101, 242));
                btnAdd.setForeground(Color.WHITE);
                btnAdd.setFocusPainted(false);
                btnAdd.addActionListener(e -> {
                    List<String> selectedUsers = new ArrayList<>();
                    for (int i = 0; i < checkboxes.size(); i++) {
                        if (checkboxes.get(i).isSelected()) {
                            selectedUsers.add(availableUsers.get(i));
                        }
                    }

                    if (selectedUsers.isEmpty()) {
                        JOptionPane.showMessageDialog(addMemberDialog,
                                "Please select at least one user.",
                                "No Selection",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Add selected users to group
                    new Thread(() -> {
                        int successCount = 0;
                        for (String user : selectedUsers) {
                            if (GroupDAO.addMember(groupId, user)) {
                                successCount++;
                                // Notify all current members
                                SwingUtilities.invokeLater(() -> {
                                    notifyMemberJoined(user);
                                });
                            }
                        }

                        final int finalCount = successCount;
                        SwingUtilities.invokeLater(() -> {
                            addMemberDialog.dispose();
                            JOptionPane.showMessageDialog(this,
                                    "Successfully added " + finalCount + " member(s) to the group.",
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                            loadGroupMembers(); // Refresh the members list
                        });
                    }).start();
                });
                buttonPanel.add(btnAdd);

                JButton btnCancel = new JButton("Cancel");
                btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                btnCancel.setBackground(new Color(100, 100, 100));
                btnCancel.setForeground(Color.WHITE);
                btnCancel.setFocusPainted(false);
                btnCancel.addActionListener(e -> addMemberDialog.dispose());
                buttonPanel.add(btnCancel);

                addMemberDialog.add(buttonPanel, BorderLayout.SOUTH);
                addMemberDialog.setVisible(true);
            });
        }).start();
    }

    /**
     * Removes the selected member from the group.
     * Only accessible by the group creator.
     */
    private void removeSelectedMember() {
        // Verify user is the creator
        if (groupCreator == null || !currentUser.equals(groupCreator)) {
            JOptionPane.showMessageDialog(this,
                    "Only the group creator can remove members.",
                    "Permission Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the selected member
        String selectedMember = membersList.getSelectedValue();
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a member to remove.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Prevent creator from removing themselves
        if (selectedMember.equals(groupCreator)) {
            JOptionPane.showMessageDialog(this,
                    "You cannot remove yourself as the group creator.",
                    "Cannot Remove",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm removal
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove " + selectedMember + " from the group?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Remove member in background thread
        new Thread(() -> {
            if (GroupDAO.removeMember(groupId, selectedMember)) {
                SwingUtilities.invokeLater(() -> {
                    // Remove from UI
                    membersModel.removeElement(selectedMember);
                    // Show notification
                    addSystemMessage(selectedMember + " was removed from the group");
                    JOptionPane.showMessageDialog(this,
                            selectedMember + " has been removed from the group.",
                            "Member Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Failed to remove " + selectedMember + " from the group.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * Deletes the entire group (creator only), removes from DB and closes window.
     */
    private void deleteGroup() {
        if (groupCreator == null || !currentUser.equals(groupCreator)) {
            JOptionPane.showMessageDialog(this,
                    "Only the group creator can delete this group.",
                    "Permission Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this group and all its messages for everyone?",
                "Delete Group",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new Thread(() -> {
            boolean ok = GroupDAO.deleteGroup(groupId);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    JOptionPane.showMessageDialog(this,
                            "Group deleted successfully.",
                            "Deleted",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to delete group.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }
}
