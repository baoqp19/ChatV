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

	private static final String NAME_FAILED = "Tên chỉ chứa chữ cái, số và dấu gạch dưới. Không được bắt đầu bằng số!";
	private static final String NAME_EXIST = "Tên này đã được sử dụng. Vui lòng chọn tên khác!";
	private static final String SERVER_NOT_START = "Không thể kết nối tới Server!\nVui lòng kiểm tra lại IP/Port hoặc bật Server.";

	private Pattern checkName = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]{0,19}$"); // Tối đa 20 ký tự
	private JTextField txtIP, txtPort, txtUserName;
	private JButton btnConnectServer;

	String IP, userName;
	String file = System.getProperty("user.dir") + "\\Server.txt";
	List<String> listServer = new ArrayList<>();

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
				catch (Exception ignored) {}
			}
			new StartClientFrame().setVisible(true);
		});
	}

	public StartClientFrame() {
		initComponents();
		autoDetectLocalIP();
	}

	private void initComponents() {
		setTitle("VKU Chat Client - Kết nối Server");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(720, 560);
		setLocationRelativeTo(null);
		setResizable(false);

		// Background chính
		JPanel background = new JPanel(new BorderLayout());
		background.setBackground(new Color(245, 250, 255));
		background.setBorder(new EmptyBorder(30, 40, 40, 40));
		setContentPane(background);

		// === TIÊU ĐỀ ===
		JLabel lblTitle = new JLabel("VKU CHAT", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 48));
		lblTitle.setForeground(new Color(0, 102, 204));
		lblTitle.setBorder(new EmptyBorder(0, 0, 20, 0));
		background.add(lblTitle, BorderLayout.NORTH);

		JLabel lblSubTitle = new JLabel("Đăng nhập để bắt đầu trò chuyện", SwingConstants.CENTER);
		lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		lblSubTitle.setForeground(new Color(100, 100, 100));
		background.add(lblSubTitle, BorderLayout.NORTH);

		// === FORM CHÍNH ===
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBackground(Color.WHITE);
		formPanel.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(new Color(180, 210, 255), 2, true),
				new EmptyBorder(40, 60, 40, 60)
		));
		formPanel.setPreferredSize(new Dimension(500, 320));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(15, 10, 15, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Server IP
		addRow(formPanel, gbc, 0, "Server IP:", txtIP = createStyledTextField("Đang dò IP..."));

		// Server Port
		addRow(formPanel, gbc, 1, "Server Port:", txtPort = createStyledTextField("3939"));

		// Username
		addRow(formPanel, gbc, 2, "Tên của bạn:", txtUserName = createStyledTextField(""));

		background.add(formPanel, BorderLayout.CENTER);

		// === NÚT KẾT NỐI ===
		JPanel bottomPanel = new JPanel(new FlowLayout());
		bottomPanel.setBackground(new Color(245, 250, 255));

		btnConnectServer = new JButton("KẾT NỐI SERVER");
		btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 20));
		btnConnectServer.setPreferredSize(new Dimension(380, 62));
		btnConnectServer.setBackground(new Color(0, 122, 255));
		btnConnectServer.setForeground(Color.WHITE);
		btnConnectServer.setFocusPainted(false);
		btnConnectServer.setBorderPainted(false);
		btnConnectServer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnConnectServer.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

		// Hover effect
		btnConnectServer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btnConnectServer.setBackground(new Color(0, 100, 220));
			}
			@Override
			public void mouseExited(MouseEvent e) {
				btnConnectServer.setBackground(new Color(0, 122, 255));
			}
		});

		btnConnectServer.addActionListener(this);
		bottomPanel.add(btnConnectServer);

		// Hint nhỏ
		JLabel lblHint = new JLabel("<html><center>Đảm bảo Server đang chạy trên IP và Port đã nhập<br>Mặc định: 127.0.0.1 - Port 3939</center></html>");
		lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
		lblHint.setForeground(new Color(120, 120, 120));
		lblHint.setBorder(new EmptyBorder(20, 0, 0, 0));

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setBackground(new Color(245, 250, 255));
		southPanel.add(bottomPanel, BorderLayout.CENTER);
		southPanel.add(lblHint, BorderLayout.SOUTH);

		background.add(southPanel, BorderLayout.SOUTH);
	}

	private JTextField createStyledTextField(String placeholder) {
		JTextField field = new JTextField(placeholder);
		field.setFont(new Font("Segoe UI", Font.PLAIN, 17));
		field.setForeground(new Color(50, 50, 50));
		field.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(new Color(200, 210, 230), 2, true),
				new EmptyBorder(10, 15, 10, 15)
		));
		field.setPreferredSize(new Dimension(300, 52));
		return field;
	}

	private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField field) {
		gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3; gbc.anchor = GridBagConstraints.EAST;
		JLabel label = new JLabel(labelText);
		label.setFont(new Font("Segoe UI", Font.BOLD, 16));
		label.setForeground(new Color(40, 40, 40));
		panel.add(label, gbc);

		gbc.gridx = 1; gbc.weightx = 0.7; gbc.anchor = GridBagConstraints.WEST;
		panel.add(field, gbc);
	}

	// Tự động dò IP local (không block UI)
	private void autoDetectLocalIP() {
		SwingWorker<Void, Void> worker = new SwingWorker<>() {
			@Override
			protected Void doInBackground() throws Exception {
				Thread.sleep(300); // để UI render xong
				Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
				for (NetworkInterface netint : Collections.list(nets)) {
					if (netint.isUp() && !netint.isLoopback() && !netint.isVirtual()) {
						Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
						for (InetAddress inetAddress : Collections.list(inetAddresses)) {
							if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
								final String ip = inetAddress.getHostAddress();
								SwingUtilities.invokeLater(() -> txtIP.setText(ip));
								return null;
							}
						}
					}
				}
				SwingUtilities.invokeLater(() -> txtIP.setText("127.0.0.1"));
				return null;
			}
		};
		worker.execute();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnConnectServer) {
			userName = txtUserName.getText().trim();
			IP = txtIP.getText().trim();

			if (userName.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Vui lòng nhập tên!", "Lỗi", JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (!checkName.matcher(userName).matches()) {
				JOptionPane.showMessageDialog(this, NAME_FAILED, "Tên không hợp lệ", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (IP.isEmpty()) {
				JOptionPane.showMessageDialog(this, "IP Server không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
				return;
			}

			try {
				Random rd = new Random();
				int portPeer = 10000 + rd.nextInt(1000);
				int portServer = Integer.parseInt(txtPort.getText().trim());

				Socket socketClient = new Socket(IP, portServer);
				socketClient.setSoTimeout(5000); // timeout 5s

				String msg = Encode.getCreateAccount(userName, String.valueOf(portPeer));
				ObjectOutputStream oos = new ObjectOutputStream(socketClient.getOutputStream());
				oos.writeObject(msg);
				oos.flush();

				ObjectInputStream ois = new ObjectInputStream(socketClient.getInputStream());
				msg = (String) ois.readObject();

				socketClient.close();

				if (msg.equals(Tags.SESSION_DENY_TAG)) {
					JOptionPane.showMessageDialog(this, NAME_EXIST, "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Thành công → mở MainFrame
				new MainFrame(IP, portPeer, userName, msg, portServer);
				dispose();

			} catch (ConnectException | SocketTimeoutException ex) {
				JOptionPane.showMessageDialog(this, SERVER_NOT_START, "Không kết nối được", JOptionPane.ERROR_MESSAGE);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Lỗi không xác định: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}
}