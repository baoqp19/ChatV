package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

import data.Peer;
import database.GroupDAO;
import database.GroupDAO.GroupInfo;
import tags.Tags;

public class MainFrame extends JFrame implements WindowListener {

	// Constants
	private static final Color HEADER_BG = new Color(0, 102, 204);
	private static final Color SELECTION_BG = new Color(0, 120, 215);
	private static final Color BORDER_COLOR = new Color(0, 102, 204);
	private static final Color SERVER_BORDER = new Color(0, 122, 255);
	private static final Color PORT_CLIENT_COLOR = new Color(255, 20, 147);
	private static final String SERVER_FILE = System.getProperty("user.dir") + "\\Server.txt";
	private static final int FRAME_WIDTH = 720;
	private static final int FRAME_HEIGHT = 600;
	private static final int HEADER_HEIGHT = 100;
	private static final int GAP = 20;

	// UI Components
	private JPanel contentPane;
	private JButton btnSaveServer;

	// Client Variables
	private Client clientNode;
	private String ipClient;
	private String nameUser;
	private String dataUser;
	private int portClient;
	private int portServer;
	private String selectedName = "";

	// Static Components
	private static final DefaultListModel<String> model = new DefaultListModel<>();
	private static JList<String> listActive;

	// Group chat components
	private DefaultListModel<String> groupsModel = new DefaultListModel<>();
	private JList<String> listGroups;
	private java.util.Map<Integer, GroupChatFrame> openGroupChats = new java.util.HashMap<>();

	public MainFrame(String ip, int portClient, String username, String rawUserList, int portServer) throws Exception {
		this.ipClient = ip;
		this.portClient = portClient;
		this.nameUser = username;
		this.dataUser = rawUserList;
		this.portServer = portServer;

		setTitle("VKU Client");
		addWindowListener(this);
		setResizable(false);

		clientNode = new Client(ipClient, portClient, nameUser, dataUser, portServer);

		logInitialization();
		initializeUI();
		setVisible(true);
	}

	private void logInitialization() {
		System.out.println("MainFrame Initialized:");
		System.out.println(" → IP      : " + ipClient);
		System.out.println(" → PortC   : " + portClient);
		System.out.println(" → User    : " + nameUser);
		System.out.println(" → RawUser : " + dataUser);
	}

	private void initializeUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		setLocationRelativeTo(null);

		contentPane = new JPanel();
		contentPane.setBackground(new Color(245, 250, 255));
		contentPane.setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));
		contentPane.setLayout(new BorderLayout(GAP, GAP));
		setContentPane(contentPane);

		contentPane.add(createHeader(), BorderLayout.NORTH);
		contentPane.add(createCenterPanelWithGroups(), BorderLayout.CENTER);
	}

	private JPanel createCenterPanelWithGroups() {
		JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
		centerPanel.setOpaque(false);

		// Split pane for users and groups
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.5);
		splitPane.setDividerLocation(330);

		splitPane.setLeftComponent(createFriendsPanel());
		splitPane.setRightComponent(createGroupsPanel());

		centerPanel.add(splitPane, BorderLayout.CENTER);
		centerPanel.add(createServerPanel(), BorderLayout.SOUTH);
		return centerPanel;
	}

	private JPanel createGroupsPanel() {
		JPanel groupsPanel = new JPanel(new BorderLayout());
		groupsPanel.setBackground(Color.WHITE);
		groupsPanel.setBorder(createTitledBorder(new Color(0, 180, 100), "My Groups"));

		listGroups = new JList<>(groupsModel);
		listGroups.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		listGroups.setSelectionBackground(new Color(0, 180, 100));
		listGroups.addMouseListener(new GroupsListMouseListener());

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		buttonsPanel.setBackground(Color.WHITE);

		JButton btnCreateGroup = new JButton("➕ Create Group");
		btnCreateGroup.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnCreateGroup.setBackground(new Color(0, 180, 100));
		btnCreateGroup.setForeground(Color.WHITE);
		btnCreateGroup.setFocusPainted(false);
		btnCreateGroup.addActionListener(e -> showCreateGroupDialog());
		buttonsPanel.add(btnCreateGroup);

		JButton btnRefreshGroups = new JButton("🔄 Refresh");
		btnRefreshGroups.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnRefreshGroups.setBackground(new Color(100, 150, 200));
		btnRefreshGroups.setForeground(Color.WHITE);
		btnRefreshGroups.setFocusPainted(false);
		btnRefreshGroups.addActionListener(e -> loadUserGroups());
		buttonsPanel.add(btnRefreshGroups);

		groupsPanel.add(new JScrollPane(listGroups), BorderLayout.CENTER);
		groupsPanel.add(buttonsPanel, BorderLayout.SOUTH);

		// Load groups on startup
		loadUserGroups();

		return groupsPanel;
	}

	private void showCreateGroupDialog() {
		JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
		panel.add(new JLabel("Group Name:"));
		JTextField txtGroupName = new JTextField(20);
		panel.add(txtGroupName);

		panel.add(new JLabel("Invite Users (comma-separated):"));
		JTextField txtMembers = new JTextField(20);
		panel.add(txtMembers);

		int result = JOptionPane.showConfirmDialog(this, panel, "Create Group",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			String groupName = txtGroupName.getText().trim();
			String membersStr = txtMembers.getText().trim();

			if (groupName.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Group name cannot be empty!",
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Create group in database
			int groupId = GroupDAO.createGroup(groupName, nameUser);
			if (groupId > 0) {
				// Add invited members
				if (!membersStr.isEmpty()) {
					String[] members = membersStr.split(",");
					for (String member : members) {
						String trimmed = member.trim();
						if (!trimmed.isEmpty() && !trimmed.equals(nameUser)) {
							GroupDAO.addMember(groupId, trimmed);
						}
					}
				}

				JOptionPane.showMessageDialog(this, "Group created successfully!",
						"Success", JOptionPane.INFORMATION_MESSAGE);
				loadUserGroups();

				// Open the group chat window
				openGroupChat(groupId, groupName);
			} else {
				JOptionPane.showMessageDialog(this, "Failed to create group!",
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void loadUserGroups() {
		new Thread(() -> {
			List<GroupInfo> groups = GroupDAO.getUserGroups(nameUser);
			SwingUtilities.invokeLater(() -> {
				groupsModel.clear();
				for (GroupInfo group : groups) {
					groupsModel.addElement(group.groupName() + " (ID:" + group.groupId() + ")");
				}
			});
		}).start();
	}

	private void openGroupChat(int groupId, String groupName) {
		if (openGroupChats.containsKey(groupId)) {
			// Bring existing window to front
			GroupChatFrame existing = openGroupChats.get(groupId);
			existing.toFront();
			existing.requestFocus();
		} else {
			// Create new group chat window
			try {
				GroupChatFrame groupChat = new GroupChatFrame(
						groupId, groupName, nameUser,
						clientNode); // Pass client for connections
				openGroupChats.put(groupId, groupChat);

				// Remove from map when closed
				groupChat.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						openGroupChats.remove(groupId);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
						"Failed to open group chat: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private JPanel createHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(HEADER_BG);
		header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));

		JLabel lblTitle = new JLabel("VKU CHAT CLIENT", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 38));
		lblTitle.setForeground(Color.WHITE);

		JLabel lblUser = new JLabel("  Username: " + nameUser);
		lblUser.setFont(new Font("Segoe UI", Font.BOLD, 20));
		lblUser.setForeground(new Color(200, 255, 255));

		header.add(lblTitle, BorderLayout.CENTER);
		header.add(lblUser, BorderLayout.WEST);
		return header;
	}

	private JPanel createCenterPanel() {
		JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
		centerPanel.setOpaque(false);
		centerPanel.add(createFriendsPanel(), BorderLayout.CENTER);
		centerPanel.add(createServerPanel(), BorderLayout.EAST);
		return centerPanel;
	}

	private JPanel createFriendsPanel() {
		JPanel friendsPanel = new JPanel(new BorderLayout());
		friendsPanel.setBackground(Color.WHITE);
		friendsPanel.setBorder(createTitledBorder(BORDER_COLOR, "Online Users"));

		listActive = new JList<>(model);
		listActive.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		listActive.setSelectionBackground(SELECTION_BG);
		listActive.addMouseListener(new FriendsListMouseListener());

		friendsPanel.add(new JScrollPane(listActive), BorderLayout.CENTER);
		return friendsPanel;
	}

	private JPanel createServerPanel() {
		JPanel serverPanel = new JPanel(new GridBagLayout());
		serverPanel.setBackground(Color.WHITE);
		serverPanel.setBorder(createTitledBorder(SERVER_BORDER, " VKU Server "));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 10, 8, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;

		String localIP = getLocalIPAddress();
		addServerRow(serverPanel, gbc, 0, "IP Address", localIP, Color.GREEN.darker());
		addServerRow(serverPanel, gbc, 1, "Port Server", String.valueOf(portServer), Color.RED);
		addServerRow(serverPanel, gbc, 2, "Port Client", String.valueOf(portClient), PORT_CLIENT_COLOR);

		btnSaveServer = new JButton("Save Server");
		btnSaveServer.addActionListener(e -> saveServer());
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		serverPanel.add(btnSaveServer, gbc);

		return serverPanel;
	}

	private TitledBorder createTitledBorder(Color color, String title) {
		return BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(color, 2),
				title, TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16), color);
	}

	private String getLocalIPAddress() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (Exception ignored) {
			return "127.0.0.1";
		}
	}

	private void updateVoiceInfo(String peerName) {
		Peer peer = findPeerByName(peerName);
		if (peer == null) {
			System.out.println("Peer not found: " + peerName);
			return;
		}

		try {
			InetAddress address = InetAddress.getByName(peer.getHost());
			int voicePort = peer.getPort() + 1; // Voice port = Chat port + 1
			logVoiceInfo(peerName, address, voicePort);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Peer findPeerByName(String peerName) {
		return clientNode.clientList.stream()
				.filter(p -> p.getName().equals(peerName))
				.findFirst()
				.orElse(null);
	}

	private void logVoiceInfo(String peerName, InetAddress address, int voicePort) {
		System.out.println("VoiceInfo updated:");
		System.out.println(" → Peer: " + peerName);
		System.out.println(" → Addr: " + address);
		System.out.println(" → VoicePort: " + voicePort);
	}

	private void addServerRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value, Color color) {
		gbc.gridy = row;
		gbc.gridx = 0;

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panel.add(labelComponent, gbc);

		gbc.gridx = 1;
		JLabel valueComponent = new JLabel(value);
		valueComponent.setFont(new Font("Consolas", Font.BOLD, 16));
		valueComponent.setForeground(color);
		panel.add(valueComponent, gbc);
	}

	private void connectChat() {
		if (selectedName.equals(nameUser)) {
			Tags.show(this, "Cannot chat with yourself!", false);
			return;
		}

		Peer peer = findPeerByName(selectedName);
		if (peer == null) {
			Tags.show(this, "Friend not found!", false);
			return;
		}

		try {
			clientNode.startChat(peer.getHost(), peer.getPort(), selectedName);
		} catch (Exception e) {
			Tags.show(this, "Unable to connect. Peer may be offline.", false);
		}
	}

	private void saveServer() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(SERVER_FILE, true))) {
			writer.write(ipClient + " " + portServer);
			writer.newLine();
			JOptionPane.showMessageDialog(this, "Server saved.");
			btnSaveServer.setVisible(false);
		} catch (IOException e) {
			System.err.println("Failed to save server: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void updateFriendMainFrame(String name) {
		if (!model.contains(name)) {
			model.addElement(name);
		}
	}

	public static void resetList() {
		model.clear();
	}

	public static int request(String msg, boolean type) {
		return Tags.show(new JFrame(), msg, type);
	}

	@Override
	public void windowClosing(WindowEvent e) {
		try {
			clientNode.exit();
		} catch (Exception ex) {
			System.err.println("Error closing client: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	private class FriendsListMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			int index = listActive.locationToIndex(e.getPoint());
			if (index >= 0) {
				selectedName = model.getElementAt(index);
				updateVoiceInfo(selectedName);
				connectChat();
			}
		}
	}

	private class GroupsListMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) { // Double-click to open
				int index = listGroups.locationToIndex(e.getPoint());
				if (index >= 0) {
					String selected = groupsModel.getElementAt(index);
					// Extract group ID from "GroupName (ID:123)" format
					int idStart = selected.indexOf("(ID:") + 4;
					int idEnd = selected.indexOf(")", idStart);
					if (idStart > 3 && idEnd > idStart) {
						int groupId = Integer.parseInt(selected.substring(idStart, idEnd));
						String groupName = selected.substring(0, selected.indexOf(" (ID:"));
						openGroupChat(groupId, groupName);
					}
				}
			}
		}
	}
}
