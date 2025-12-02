package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import javax.swing.*;
import javax.swing.border.*;

import data.Peer;
import tags.Tags;

public class MainFrame extends JFrame implements WindowListener {

	private JPanel contentPane;
	private Client clientNode;
	private static String IPClient = "", nameUser = "", dataUser = "";
	private static int portClient = 0;
	private static int portServer = 0;
	private static DefaultListModel<String> model = new DefaultListModel<>();
	private static JList<String> listActive;
	private String name;
	private JButton btnSaveServer;
	private String file = System.getProperty("user.dir") + "\\Server.txt";

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				MainFrame frame = new MainFrame();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// Constructor chính – giữ nguyên logic của bạn
	public MainFrame(String ip, int portClientArg, String userName, String msg, int portServerArg) throws Exception {
		IPClient = ip;
		portClient = portClientArg;
		nameUser = userName;
		dataUser = msg;
		portServer = portServerArg;
		System.out.println("Port Server Main UI: " + portServer);

		EventQueue.invokeLater(() -> {
			try {
				new MainFrame().setVisible(true);   // vẫn tạo frame mới như cũ
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// Constructor mặc định – CHỈ THAY GIAO DIỆN Ở ĐÂY
	public MainFrame() throws Exception {
		setTitle("VKU Client");
		this.addWindowListener(this);
		setResizable(false);

		// Tạo client đúng như cũ
		clientNode = new Client(IPClient, portClient, nameUser, dataUser, portServer);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(720, 600);
		setLocationRelativeTo(null);

		contentPane = new JPanel();
		contentPane.setBackground(new Color(245, 250, 255));
		contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
		contentPane.setLayout(new BorderLayout(20, 20));
		setContentPane(contentPane);

		// === HEADER ===
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(0, 102, 204));
		header.setPreferredSize(new Dimension(0, 100));

		JLabel lblTitle = new JLabel("VKU CHAT CLIENT", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 38));
		lblTitle.setForeground(Color.WHITE);
		header.add(lblTitle, BorderLayout.CENTER);

		JLabel lblUser = new JLabel("  Username: " + nameUser);
		lblUser.setFont(new Font("Segoe UI", Font.BOLD, 20));
		lblUser.setForeground(new Color(200, 255, 255));
		lblUser.setIconTextGap(12);
		header.add(lblUser, BorderLayout.WEST);

		contentPane.add(header, BorderLayout.NORTH);

		// === MAIN CONTENT (CENTER + EAST) ===
		JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
		centerPanel.setOpaque(false);

		// Danh sách người online
		JPanel friendsPanel = new JPanel(new BorderLayout());
		friendsPanel.setBackground(Color.WHITE);
		friendsPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 102, 204), 2, true),
				"  Online Users  ", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16), new Color(0, 102, 204)));

		listActive = new JList<>(model);
		listActive.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		listActive.setBackground(Color.WHITE);
		listActive.setSelectionBackground(new Color(0, 120, 215));
		listActive.setSelectionForeground(Color.WHITE);
		listActive.setFixedCellHeight(50);

		JScrollPane scrollPane = new JScrollPane(listActive);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		friendsPanel.add(scrollPane, BorderLayout.CENTER);
		centerPanel.add(friendsPanel, BorderLayout.CENTER);

		// Click để chat (giữ nguyên logic cũ)
		listActive.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int index = listActive.locationToIndex(e.getPoint());
				if (index >= 0) {
					name = listActive.getModel().getElementAt(index);
					connectChat();
				}
			}
		});

		// === PANEL SERVER INFO (bên phải) ===
		JPanel serverPanel = new JPanel(new GridBagLayout());
		serverPanel.setBackground(Color.WHITE);
		serverPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 122, 255), 2, true),
				" VKU Server ", TitledBorder.CENTER, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 15), new Color(0, 122, 255)));
		serverPanel.setPreferredSize(new Dimension(260, 0));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 10, 8, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		String localIP = "127.0.0.1";
		try {
			localIP = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException ignored) {}

		addServerRow(serverPanel, gbc, 0, "IP Address", localIP, Color.GREEN.darker());
		addServerRow(serverPanel, gbc, 1, "Port Server", String.valueOf(portServer), Color.RED);
		addServerRow(serverPanel, gbc, 2, "Port Client", String.valueOf(portClient), new Color(255, 20, 147));

		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(20, 10, 10, 10);

		btnSaveServer = new JButton("Save Server");
		btnSaveServer.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btnSaveServer.setBackground(new Color(0, 170, 0));
		btnSaveServer.setForeground(Color.WHITE);
		btnSaveServer.setFocusPainted(false);
		btnSaveServer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnSaveServer.addActionListener(e -> SaveServer());
		serverPanel.add(btnSaveServer, gbc);

		centerPanel.add(serverPanel, BorderLayout.EAST);
		contentPane.add(centerPanel, BorderLayout.CENTER);

		// Footer nhẹ
		JLabel footer = new JLabel("Click vào tên để bắt đầu chat • VKU Chat Client 2025", SwingConstants.CENTER);
		footer.setFont(new Font("Segoe UI", Font.ITALIC, 13));
		footer.setForeground(new Color(100, 100, 100));
		contentPane.add(footer, BorderLayout.SOUTH);
	}

	private void addServerRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value, Color valueColor) {
		gbc.gridy = row;
		gbc.gridx = 0;
		gbc.weightx = 0.4;
		JLabel l1 = new JLabel(label);
		l1.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panel.add(l1, gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.6;
		JLabel l2 = new JLabel(value);
		l2.setFont(new Font("Consolas", Font.BOLD, 16));
		l2.setForeground(valueColor);
		panel.add(l2, gbc);
	}

	// ============== TẤT CẢ LOGIC CỦA BẠN GIỮ NGUYÊN HOÀN TOÀN ==============
	private void connectChat() {
		int n = JOptionPane.showConfirmDialog(this, "Would you like to connect to this account?", "Connect",
				JOptionPane.YES_NO_OPTION);
		if (n != JOptionPane.YES_OPTION) return;

		if (name.equals("") || Client.clientarray == null) {
			Tags.show(this, "Invalid username", false);
			return;
		}
		if (name.equals(nameUser)) {
			Tags.show(this, "Cannot chat with yourself", false);
			return;
		}

		Peer peer = Client.clientarray.stream()
				.filter(p -> p.getName().equals(name))
				.findFirst().orElse(null);

		if (peer != null) {
			try {
				clientNode.intialNewChat(peer.getHost(), peer.getPort(), name);
			} catch (Exception e) {
				e.printStackTrace();
				Tags.show(this, "Unable to connect. Peer may be offline.", false);
			}
		} else {
			Tags.show(this, "Friend is not found. Waiting for update...", false);
		}
	}

	void SaveServer() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
			bw.write(IPClient + " " + portServer);
			bw.newLine();
			JOptionPane.showMessageDialog(this, "Server has been saved.");
			btnSaveServer.setVisible(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateFriendMainFrame(String msg) {
		if (!model.contains(msg)) model.addElement(msg);
	}

	public static void resetList() {
		model.clear();
	}

	public static int request(String msg, boolean type) {
		JFrame frameMessage = new JFrame();
		return Tags.show(frameMessage, msg, type);
	}

	@Override public void windowOpened(WindowEvent e) {}
	@Override public void windowClosing(WindowEvent e) {
		try { clientNode.exit(); } catch (Exception ex) { ex.printStackTrace(); }
	}
	@Override public void windowClosed(WindowEvent e) {}
	@Override public void windowIconified(WindowEvent e) {}
	@Override public void windowDeiconified(WindowEvent e) {}
	@Override public void windowActivated(WindowEvent e) {}
	@Override public void windowDeactivated(WindowEvent e) {}
}