package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.*;

import data.Peer;
import database.DBUtil;
import database.UserDAO;
import tags.Tags;

public class MainFrame extends JFrame implements WindowListener {

	private JPanel contentPane;
	private Client clientNode;

	private String IPClient = "";
	private String nameUser = "";
	private String dataUser = "";
	private int portClient = 0;
	private int portServer = 0;
	private VoiceInfo voiceInfo;
	private static DefaultListModel<String> model = new DefaultListModel<>();
	private static JList<String> listActive;

	private String selectedName = "";

	private JButton btnSaveServer;
	private String file = System.getProperty("user.dir") + "\\Server.txt";

	// ==========================================================
	// MAINFRAME REAL CONSTRUCTOR (chỉ có 1)
	// ==========================================================
	public MainFrame(String ip, int portClient, String username, String rawUserList, int portServer) throws Exception {

		this.IPClient = ip;
		this.portClient = portClient;
		this.nameUser = username;
		this.dataUser = rawUserList;
		this.portServer = portServer;

		setTitle("VKU Client");
		addWindowListener(this);
		setResizable(false);

		// ========== KHỞI TẠO CLIENT ==========
		clientNode = new Client(IPClient, portClient, nameUser, dataUser, portServer);

		System.out.println("MainFrame Initialized:");
		System.out.println(" → IP      : " + IPClient);
		System.out.println(" → PortC   : " + portClient);
		System.out.println(" → User    : " + nameUser);
		System.out.println(" → RawUser : " + dataUser);

		// ========== UI ==========
		initUI();

		this.setVisible(true);
	}

	// ==========================================================
	// KHỞI TẠO UI
	// ==========================================================
	private void initUI() {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(720, 600);
		setLocationRelativeTo(null);

		contentPane = new JPanel();
		contentPane.setBackground(new Color(245, 250, 255));
		contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
		contentPane.setLayout(new BorderLayout(20, 20));
		setContentPane(contentPane);

		// HEADER
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(0, 102, 204));
		header.setPreferredSize(new Dimension(0, 100));

		JLabel lblTitle = new JLabel("VKU CHAT CLIENT", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 38));
		lblTitle.setForeground(Color.WHITE);

		JLabel lblUser = new JLabel("  Username: " + nameUser);
		lblUser.setFont(new Font("Segoe UI", Font.BOLD, 20));
		lblUser.setForeground(new Color(200, 255, 255));

		header.add(lblTitle, BorderLayout.CENTER);
		header.add(lblUser, BorderLayout.WEST);

		contentPane.add(header, BorderLayout.NORTH);

		// MAIN PANEL
		JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
		centerPanel.setOpaque(false);

		// LIST USER
		JPanel friendsPanel = new JPanel(new BorderLayout());
		friendsPanel.setBackground(Color.WHITE);
		friendsPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
				"Online Users", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16), new Color(0, 102, 204)));

		listActive = new JList<>(model);
		listActive.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		listActive.setSelectionBackground(new Color(0, 120, 215));

		listActive.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = listActive.locationToIndex(e.getPoint());
				if (index >= 0) {
					selectedName = model.getElementAt(index);

					updateVoiceInfo(selectedName);
					connectChat();
				}
			}
		});

		friendsPanel.add(new JScrollPane(listActive), BorderLayout.CENTER);
		centerPanel.add(friendsPanel, BorderLayout.CENTER);

		// SERVER INFO PANEL
		JPanel serverPanel = new JPanel(new GridBagLayout());
		serverPanel.setBackground(Color.WHITE);
		serverPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 122, 255), 2),
				" VKU Server ",
				TitledBorder.CENTER, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 15),
				new Color(0, 122, 255)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 10, 8, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		String localIP = "127.0.0.1";
		try { localIP = Inet4Address.getLocalHost().getHostAddress(); } catch (Exception ignored) {}

		addServerRow(serverPanel, gbc, 0, "IP Address", localIP, Color.GREEN.darker());
		addServerRow(serverPanel, gbc, 1, "Port Server", String.valueOf(portServer), Color.RED);
		addServerRow(serverPanel, gbc, 2, "Port Client", String.valueOf(portClient), new Color(255, 20, 147));

		btnSaveServer = new JButton("Save Server");
		btnSaveServer.addActionListener(e -> saveServer());

		gbc.gridy = 3;
		gbc.gridwidth = 2;
		serverPanel.add(btnSaveServer, gbc);

		centerPanel.add(serverPanel, BorderLayout.EAST);

		contentPane.add(centerPanel, BorderLayout.CENTER);
	}

	private void updateVoiceInfo(String peerName) {

		Peer peer = clientNode.clientList.stream()
				.filter(p -> p.getName().equals(peerName))
				.findFirst()
				.orElse(null);

		if (peer == null) {
			System.out.println("Không tìm thấy peer để setup voice!");
			return;
		}

		try {
			InetAddress address = InetAddress.getByName(peer.getHost());

			// QUY ƯỚC: port voice = port chat + 1
			int voicePort = peer.getPort() + 1;

			// GÁN VÀO voiceInfo
			voiceInfo = new VoiceInfo(address, voicePort, peerName);

			System.out.println("VoiceInfo updated:");
			System.out.println(" → Peer: " + peerName);
			System.out.println(" → Addr: " + address);
			System.out.println(" → VoicePort: " + voicePort);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void addServerRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value, Color color) {
		gbc.gridy = row;

		JLabel lbl = new JLabel(label);
		lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panel.add(lbl, gbc);

		gbc.gridx = 1;
		JLabel val = new JLabel(value);
		val.setFont(new Font("Consolas", Font.BOLD, 16));
		val.setForeground(color);
		panel.add(val, gbc);

		gbc.gridx = 0;
	}

	// ==========================================================
	// CONNECT CHAT
	// ==========================================================
	private void connectChat() {

		if (selectedName.equals(nameUser)) {
			Tags.show(this, "Cannot chat with yourself!", false);
			return;
		}

		Peer peer = clientNode.clientList.stream()
				.filter(p -> p.getName().equals(selectedName))
				.findFirst().orElse(null);

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

	// ==========================================================
	// SAVE SERVER BUTTON
	// ==========================================================
	private void saveServer() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
			bw.write(IPClient + " " + portServer);
			bw.newLine();
			JOptionPane.showMessageDialog(this, "Server saved.");
			btnSaveServer.setVisible(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// STATIC METHODS FOR Client.updateFriendList()
	// ==========================================================
	public static void updateFriendMainFrame(String name) {
		if (!model.contains(name)) model.addElement(name);
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
			ex.printStackTrace(); }
	}

	@Override public void windowOpened(WindowEvent e) {}
	@Override public void windowClosed(WindowEvent e) {}
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowActivated(WindowEvent e) {}
	@Override public void windowDeactivated(WindowEvent e) {}
}
