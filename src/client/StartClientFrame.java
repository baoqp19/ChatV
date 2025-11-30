package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;

import client.MainFrame;
import tags.Encode;
import tags.Tags;

public class StartClientFrame extends JFrame implements ActionListener {
	private static final String NAME_FAILED = "THIS NAME CONTAINS INVALID CHARACTER. PLEASE TRY AGAIN";
	private static final String NAME_EXIST = "THIS NAME IS ALREADY USED. PLEASE TRY AGAIN";
	private static final String SERVER_NOT_START = "TURN ON SERVER BEFORE START";

	private Pattern checkName = Pattern.compile("[_a-zA-Z][_a-zA-Z0-9]*");
	private JPanel contentPane;
	private JTextField txtIP;
	private JTextField txtPort;
	private JTextField txtUserName;
	private JButton btnConnectServer;

	int port;
	String IP, userName;
	String file = System.getProperty("user.dir") + "\\Server.txt";
	List<String> listServer = new ArrayList<>();

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				StartClientFrame frame = new StartClientFrame();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	void updateServer(String IP, String port) {
		txtIP.setText(IP);
		txtPort.setText(port);
	}

	String[] readFileServer() throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(file));
		while (scanner.hasNext()) {
			String server = scanner.nextLine();
			listServer.add(server);
		}
		scanner.close();
		return listServer.toArray(new String[0]);
	}

	public StartClientFrame() {
		setTitle("VKU Client - Connect to Server");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(650, 480);
		setLocationRelativeTo(null); // center window
		contentPane = new JPanel();
		contentPane.setBackground(Color.WHITE);
		contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
		contentPane.setLayout(new BorderLayout(10, 10));
		setContentPane(contentPane);

		// HEADER
		JLabel lblTitle = new JLabel("VKU Client", SwingConstants.CENTER);
		lblTitle.setForeground(new Color(0, 102, 204));
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 32));
		contentPane.add(lblTitle, BorderLayout.NORTH);

		// MAIN PANEL
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(Color.WHITE);
		mainPanel.setLayout(new GridBagLayout());
		contentPane.add(mainPanel, BorderLayout.CENTER);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(12, 12, 12, 12);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// IP
		gbc.gridx = 0; gbc.gridy = 0;
		JLabel lblIP = new JLabel("Server IP:");
		lblIP.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		mainPanel.add(lblIP, gbc);

		gbc.gridx = 1; gbc.gridy = 0;
		txtIP = new JTextField();
		txtIP.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		txtIP.setColumns(20);
		mainPanel.add(txtIP, gbc);

		// PORT
		gbc.gridx = 0; gbc.gridy = 1;
		JLabel lblPort = new JLabel("Server Port:");
		lblPort.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		mainPanel.add(lblPort, gbc);

		gbc.gridx = 1; gbc.gridy = 1;
		txtPort = new JTextField("3939");
		txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		mainPanel.add(txtPort, gbc);

		// USERNAME
		gbc.gridx = 0; gbc.gridy = 2;
		JLabel lblUser = new JLabel("Username:");
		lblUser.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		mainPanel.add(lblUser, gbc);

		gbc.gridx = 1; gbc.gridy = 2;
		txtUserName = new JTextField();
		txtUserName.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		mainPanel.add(txtUserName, gbc);

		// CONNECT BUTTON
		gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.CENTER;
		btnConnectServer = new JButton("Connect");
		btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 18));
		btnConnectServer.setBackground(new Color(0, 153, 0));
		btnConnectServer.setForeground(Color.WHITE);
		btnConnectServer.setFocusPainted(false);
		btnConnectServer.addActionListener(this);
		mainPanel.add(btnConnectServer, gbc);

		// Try to set local IP automatically
		try {
			Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
			while (nics.hasMoreElements()) {
				NetworkInterface nic = nics.nextElement();
				if (nic.isUp() && !nic.isLoopback() && !nic.isVirtual()) {
					Enumeration<InetAddress> addrs = nic.getInetAddresses();
					while (addrs.hasMoreElements()) {
						InetAddress addr = addrs.nextElement();
						if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
							txtIP.setText(addr.getHostAddress());
						}
					}
				}
			}
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnConnectServer) {
			userName = txtUserName.getText().trim();
			IP = txtIP.getText().trim();

			if (checkName.matcher(userName).matches() && !IP.isEmpty()) {
				try {
					Random rd = new Random();
					int portPeer = 10000 + rd.nextInt(1000);
					InetAddress ipServer = InetAddress.getByName(IP);
					int portServer = Integer.parseInt(txtPort.getText().trim());
					Socket socketClient = new Socket(ipServer, portServer);

					String msg = Encode.getCreateAccount(userName, Integer.toString(portPeer));
					ObjectOutputStream serverOutputStream = new ObjectOutputStream(socketClient.getOutputStream());
					serverOutputStream.writeObject(msg);
					serverOutputStream.flush();

					ObjectInputStream serverInputStream = new ObjectInputStream(socketClient.getInputStream());
					msg = (String) serverInputStream.readObject();

					socketClient.close();

					if (msg.equals(Tags.SESSION_DENY_TAG)) {
						JOptionPane.showMessageDialog(this, NAME_EXIST, "Login Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					new MainFrame(IP, portPeer, userName, msg, portServer);
					this.dispose();

				} catch (Exception ex) {
					JOptionPane.showMessageDialog(this, SERVER_NOT_START, "Login Error", JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			} else {
				JOptionPane.showMessageDialog(this, NAME_FAILED, "Login Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
