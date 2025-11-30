package server;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

import data.Peer;

public class ServerFrame extends JFrame {

	private JTextField txtIP;
	private JTextField txtPort;
	private JLabel lblStatus;
	private static JTextArea txtMessage;
	public static JLabel lblUserOnline;
	public static int port = 3939;
	static ServerCore server;
	private JButton btnStartServer, btnStopServer;

	public ServerFrame() {
		setTitle("VKU Server");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(650, 550);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout(10, 10));

		// HEADER
		JLabel lblTitle = new JLabel("VKU SERVER", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
		lblTitle.setForeground(new Color(0, 102, 204));
		add(lblTitle, BorderLayout.NORTH);

		// MAIN PANEL
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
		add(mainPanel, BorderLayout.CENTER);

		// IP & Port Panel
		JPanel networkPanel = new JPanel(new GridLayout(2, 2, 10, 10));
		networkPanel.setBorder(BorderFactory.createTitledBorder("Network Info"));
		mainPanel.add(networkPanel);

		networkPanel.add(new JLabel("IP:", SwingConstants.LEFT));
		txtIP = new JTextField();
		txtIP.setEditable(false);
		txtIP.setForeground(Color.BLUE);
		txtIP.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		networkPanel.add(txtIP);

		networkPanel.add(new JLabel("Port:", SwingConstants.LEFT));
		txtPort = new JTextField(String.valueOf(port));
		txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		txtPort.setForeground(Color.RED);
		networkPanel.add(txtPort);

		// Lấy IP LAN
		try {
			Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
			while (nics.hasMoreElements()) {
				NetworkInterface nic = nics.nextElement();
				Enumeration<InetAddress> addrs = nic.getInetAddresses();
				while (addrs.hasMoreElements()) {
					InetAddress addr = addrs.nextElement();
					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
						txtIP.setText(addr.getHostAddress());
					}
				}
			}
		} catch (SocketException e) {
			txtIP.setText("UNKNOWN");
		}

		// STATUS & USERS PANEL
		JPanel statusPanel = new JPanel(new GridLayout(2, 2, 10, 10));
		statusPanel.setBorder(BorderFactory.createTitledBorder("Server Info"));
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(statusPanel);

		statusPanel.add(new JLabel("Status:", SwingConstants.LEFT));
		lblStatus = new JLabel("OFF");
		lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		lblStatus.setForeground(Color.RED);
		statusPanel.add(lblStatus);

		statusPanel.add(new JLabel("Users Online:", SwingConstants.LEFT));
		lblUserOnline = new JLabel("0");
		lblUserOnline.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		lblUserOnline.setForeground(Color.BLUE);
		statusPanel.add(lblUserOnline);

		// BUTTON PANEL
		JPanel buttonPanel = new JPanel();
//		buttonPanel.setBackground(new Color(60, 63, 65)); // nền panel tối nhẹ để nổi button

		btnStartServer = new JButton("Start VKU Server");
		btnStartServer.setFont(new Font("Segoe UI", Font.BOLD, 18));
		btnStartServer.setBackground(new Color(76, 175, 80)); // xanh lá sáng
		btnStartServer.setForeground(Color.WHITE);
		btnStartServer.setFocusable(false);

		btnStopServer = new JButton("Stop VKU Server");
		btnStopServer.setFont(new Font("Segoe UI", Font.BOLD, 18));
		btnStopServer.setBackground(new Color(244, 67, 54)); // đỏ tươi
		btnStopServer.setForeground(Color.WHITE);
		btnStopServer.setFocusable(false);
		btnStopServer.setEnabled(false);

		buttonPanel.add(btnStartServer);
		buttonPanel.add(Box.createHorizontalStrut(20));
		buttonPanel.add(btnStopServer);

		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(buttonPanel);

		buttonPanel.add(btnStartServer);
		buttonPanel.add(Box.createHorizontalStrut(20));
		buttonPanel.add(btnStopServer);
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(buttonPanel);

		// USERS LIST PANEL
		JPanel usersPanel = new JPanel(new BorderLayout());
		usersPanel.setBorder(BorderFactory.createTitledBorder("Users List"));

		// Nền sáng hơn để chữ trắng dễ đọc
		Color backgroundColor = new Color(45, 45, 45); // xám đậm, dịu mắt
		usersPanel.setBackground(backgroundColor);

		txtMessage = new JTextArea();
		txtMessage.setEditable(false);
		txtMessage.setFont(new Font("Monospaced", Font.PLAIN, 16));
		txtMessage.setBackground(backgroundColor); // nền text area cùng màu panel
		txtMessage.setForeground(Color.WHITE); // chữ trắng

		JScrollPane scrollPane = new JScrollPane(txtMessage);
		scrollPane.getViewport().setBackground(backgroundColor); // viewport cùng màu
		usersPanel.add(scrollPane, BorderLayout.CENTER);

		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(usersPanel);

		// BUTTON ACTIONS
		btnStartServer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					port = Integer.valueOf(txtPort.getText());
					server = new ServerCore(port);
					ServerFrame.updateMessage("START VKU SERVER ON PORT " + port);
					lblStatus.setText("<html><font color='blue'>ON</font></html>");
					btnStopServer.setEnabled(true);
					btnStartServer.setEnabled(false);
				} catch (Exception e1) {
					ServerFrame.updateMessage("START ERROR");
					e1.printStackTrace();
				}
			}
		});

		btnStopServer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				lblUserOnline.setText("0");
				try {
					server.stopserver();
					ServerFrame.updateMessage("STOP VKU SERVER");
					lblStatus.setText("<html><font color='red'>OFF</font></html>");
					btnStopServer.setEnabled(false);
					btnStartServer.setEnabled(true);
				} catch (Exception ex) {
					ex.printStackTrace();
					ServerFrame.updateMessage("STOP VKU SERVER");
					lblStatus.setText("<html><font color='red'>OFF</font></html>");
					btnStopServer.setEnabled(false);
					btnStartServer.setEnabled(true);
				}
			}
		});
	}

	// UPDATE METHODS
	public static void updateMessage(String msg) {
		txtMessage.append(msg + "\n");
	}

	public static void updateNumberClient() {
		int number = Integer.parseInt(lblUserOnline.getText());
		lblUserOnline.setText(Integer.toString(number + 1));
		displayUser();

	}

	public static void decreaseNumberClient() {
		int number = Integer.parseInt(lblUserOnline.getText());
		lblUserOnline.setText(Integer.toString(number - 1));
		displayUser();

	}

	static void displayUser() {
		txtMessage.setText("");
		ArrayList<Peer> list = server.getListUser();
		for (int i = 0; i < list.size(); i++) {
			txtMessage.append((i + 1) + "\t" + list.get(i).getName() + "\n");
		}
	}
}
