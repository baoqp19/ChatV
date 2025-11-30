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

	public MainFrame(String ip, int portClientArg, String userName, String msg, int portServerArg) throws Exception {
		IPClient = ip;
		portClient = portClientArg;
		nameUser = userName;
		dataUser = msg;
		portServer = portServerArg;
		System.out.println("Port Server Main UI: " + portServer);

		EventQueue.invokeLater(() -> {
			try {
				MainFrame frame = new MainFrame();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public MainFrame() throws Exception {
		setTitle("VKU Client");
		this.addWindowListener(this);
		setResizable(false);

		// Tạo client
		clientNode = new Client(IPClient, portClient, nameUser, dataUser, portServer);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 680, 570);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblTitle = new JLabel("Chat Client");
		lblTitle.setForeground(Color.RED);
		lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 32));
		lblTitle.setBounds(226, 10, 255, 64);
		contentPane.add(lblTitle);

		JLabel lblUser = new JLabel("Username: " + nameUser);
		lblUser.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblUser.setBounds(27, 80, 309, 47);
		contentPane.add(lblUser);

		// Panel danh sách bạn bè
		JPanel panelFriends = new JPanel();
		panelFriends.setBorder(new TitledBorder(new CompoundBorder(null, UIManager.getBorder("CheckBoxMenuItem.border")),
				"Online Users", TitledBorder.LEADING, TitledBorder.TOP, null, Color.BLACK));
		panelFriends.setBackground(Color.WHITE);
		panelFriends.setBounds(27, 164, 613, 344);
		panelFriends.setLayout(new GridLayout(1, 1));
		contentPane.add(panelFriends);

		listActive = new JList<>(model);
		listActive.setBorder(new EmptyBorder(5, 5, 5, 5));
		listActive.setBackground(Color.WHITE);
		listActive.setForeground(Color.RED);
		listActive.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		JScrollPane scrollPane = new JScrollPane(listActive);
		panelFriends.add(scrollPane);

		// Mouse listener để kết nối chat
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

		// Panel server
		JPanel panelServer = new JPanel();
		panelServer.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, Color.WHITE, new Color(160, 160, 160)),
				"VKU Server", TitledBorder.LEADING, TitledBorder.TOP, null, SystemColor.textHighlight));
		panelServer.setForeground(Color.BLUE);
		panelServer.setBackground(Color.BLACK);
		panelServer.setBounds(453, 10, 187, 108);
		panelServer.setLayout(null);
		contentPane.add(panelServer);

		JLabel lblIP = new JLabel("IP Address");
		lblIP.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblIP.setForeground(Color.WHITE);
		lblIP.setBounds(10, 10, 85, 24);
		panelServer.add(lblIP);

		JLabel lblPort = new JLabel("Port Server");
		lblPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblPort.setForeground(Color.WHITE);
		lblPort.setBounds(10, 44, 85, 13);
		panelServer.add(lblPort);

		JLabel lblIPValue = new JLabel("127.0.0.1");
		try {
			lblIPValue.setText(Inet4Address.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		lblIPValue.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblIPValue.setForeground(Color.GREEN);
		lblIPValue.setBounds(88, 9, 115, 24);
		panelServer.add(lblIPValue);

		JLabel lblPortValue = new JLabel(String.valueOf(portServer));
		lblPortValue.setFont(new Font("Tahoma", Font.BOLD, 18));
		lblPortValue.setForeground(Color.RED);
		lblPortValue.setBounds(88, 44, 74, 17);
		panelServer.add(lblPortValue);

		JLabel lblPortClient = new JLabel("Port Client");
		lblPortClient.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblPortClient.setForeground(Color.WHITE);
		lblPortClient.setBounds(10, 81, 74, 13);
		panelServer.add(lblPortClient);

		JLabel lblPortClientValue = new JLabel(String.valueOf(portClient));
		lblPortClientValue.setFont(new Font("Tahoma", Font.BOLD, 18));
		lblPortClientValue.setForeground(new Color(255, 20, 147));
		lblPortClientValue.setBounds(88, 80, 89, 13);
		panelServer.add(lblPortClientValue);

		btnSaveServer = new JButton("Save Server");
		btnSaveServer.setBounds(488, 128, 112, 27);
		btnSaveServer.addActionListener(e -> SaveServer());
		contentPane.add(btnSaveServer);
	}

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

	// WindowListener
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
