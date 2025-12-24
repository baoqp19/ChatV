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

import database.UserDAO;
import tags.Encode;
import tags.Tags;

public class StartClientFrame extends JFrame implements ActionListener {

	private static final String NAME_FAILED = "Tên chỉ chứa chữ cái, số và dấu gạch dưới. Không được bắt đầu bằng số!";
	private static final String NAME_EXIST = "Tên này đã được sử dụng. Vui lòng chọn tên khác!";
	private static final String SERVER_NOT_START = "Không thể kết nối tới Server!\nVui lòng kiểm tra lại IP/Port hoặc bật Server.";

	private Pattern checkName = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]{0,19}$"); // Tối đa 20 ký tự
	private JTextField txtIP, txtPort, txtUserName;
	private JButton btnConnectServer;

	String IP, port, userName;
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
		setSize(720, 620); // Tăng chiều cao một chút để thoáng hơn
		setLocationRelativeTo(null);
		setResizable(false);

		// Main container
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(new Color(240, 248, 255));
		mainPanel.setBorder(new EmptyBorder(40, 50, 50, 50));
		setContentPane(mainPanel);

		// === HEADER ===
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setOpaque(false);

		JLabel lblTitle = new JLabel("VKU CHAT", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 52));
		lblTitle.setForeground(new Color(0, 102, 204));

		JLabel lblSubTitle = new JLabel("Đăng nhập để bắt đầu trò chuyện", SwingConstants.CENTER);
		lblSubTitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		lblSubTitle.setForeground(new Color(100, 100, 100));
		lblSubTitle.setBorder(new EmptyBorder(10, 0, 30, 0));

		headerPanel.add(lblTitle, BorderLayout.CENTER);
		headerPanel.add(lblSubTitle, BorderLayout.SOUTH);

		mainPanel.add(headerPanel, BorderLayout.NORTH);

		// === FORM CARD ===
		JPanel cardPanel = new JPanel(new GridBagLayout());
		cardPanel.setBackground(Color.WHITE);
		cardPanel.setPreferredSize(new Dimension(520, 360));
		cardPanel.setMaximumSize(new Dimension(520, 360));
		// Bo góc + bóng nhẹ (modern card style)
		cardPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(200, 220, 255), 1),
				BorderFactory.createEmptyBorder(40, 50, 40, 50)
		));
		// Thêm hiệu ứng bóng (tùy chọn, dùng Rounded border + shadow)
		cardPanel.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(new Color(180, 210, 255), 2, 20), // bo góc 20px
				new EmptyBorder(40, 50, 40, 50)
		));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(12, 0, 12, 0);
		gbc.weightx = 1.0;

		// Server IP
		addLabeledField(cardPanel, gbc, "Server IP:", txtIP = createStyledTextField("127.0.0.1"));

		// Server Port
		addLabeledField(cardPanel, gbc, "Server Port:", txtPort = createStyledTextField("3939"));

		// Username
		addLabeledField(cardPanel, gbc, "Tên của bạn:", txtUserName = createStyledTextField(""));

		mainPanel.add(cardPanel, BorderLayout.CENTER);

		// === BOTTOM: BUTTONS + HINT ===
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setOpaque(false);
		bottomPanel.setBorder(new EmptyBorder(30, 0, 0, 0));

		// Button group
		JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonGroup.setOpaque(false);

		btnConnectServer = new JButton("ĐĂNG NHẬP");
		btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 20));
		btnConnectServer.setPreferredSize(new Dimension(320, 62));
		btnConnectServer.setBackground(new Color(0, 122, 255));
		btnConnectServer.setForeground(Color.WHITE);
		btnConnectServer.setFocusPainted(false);
		btnConnectServer.setBorderPainted(false);
		btnConnectServer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnConnectServer.setBorder(new EmptyBorder(0, 30, 0, 30));

		// Hover effect
		btnConnectServer.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btnConnectServer.setBackground(new Color(0, 105, 220));
			}
			@Override
			public void mouseExited(MouseEvent e) {
				btnConnectServer.setBackground(new Color(0, 122, 255));
			}
		});

		JButton btnRegister = new JButton("ĐĂNG KÝ");
		btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 18));
		btnRegister.setPreferredSize(new Dimension(160, 62));
		btnRegister.setBackground(new Color(245, 245, 245));
		btnRegister.setForeground(new Color(50, 50, 50));
		btnRegister.setFocusPainted(false);
		btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		btnRegister.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

		btnRegister.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				btnRegister.setBackground(new Color(230, 230, 230));
			}
			@Override
			public void mouseExited(MouseEvent e) {
				btnRegister.setBackground(new Color(245, 245, 245));
			}
		});

		btnRegister.addActionListener(e -> {
			new RegisterFrame().setVisible(true);
			dispose();
		});

		btnConnectServer.addActionListener(this);

		buttonGroup.add(btnConnectServer);
		buttonGroup.add(btnRegister);

		// Hint
		JLabel lblHint = new JLabel(
				"<html><div style='text-align: center; color: #888888; font-style: italic;'>"
						+ "Mặc định: 127.0.0.1 - Port 3939<br>"
						+ "Đảm bảo Server đang chạy trước khi kết nối"
						+ "</div></html>"
		);
		lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
		lblHint.setHorizontalAlignment(SwingConstants.CENTER);
		lblHint.setBorder(new EmptyBorder(25, 0, 0, 0));

		bottomPanel.add(buttonGroup, BorderLayout.CENTER);
		bottomPanel.add(lblHint, BorderLayout.SOUTH);

		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
	}

	// Helper method để thêm label + textfield đẹp
	private void addLabeledField(JPanel panel, GridBagConstraints gbc, String labelText, JTextField textField) {
		JLabel label = new JLabel(labelText);
		label.setFont(new Font("Segoe UI", Font.BOLD, 15));
		label.setForeground(new Color(60, 60, 60));

		JPanel row = new JPanel(new BorderLayout(0, 8));
		row.setOpaque(false);
		row.add(label, BorderLayout.NORTH);
		row.add(textField, BorderLayout.CENTER);

		panel.add(row, gbc);
	}

	// Helper để tạo textfield đẹp (bạn có thể tùy chỉnh thêm)
	private JTextField createStyledTextField(String text) {
		JTextField field = new JTextField(text);
		field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		field.setPreferredSize(new Dimension(300, 48));
		field.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(180, 200, 230), 1),
				BorderFactory.createEmptyBorder(0, 15, 0, 15)
		));
		return field;
	}


	// Custom Rounded Border class (thêm vào class của bạn)
	static class RoundedBorder implements Border {
		private Color color;
		private int thickness;
		private int radii;

		public RoundedBorder(Color color, int thickness, int radii) {
			this.color = color;
			this.thickness = thickness;
			this.radii = radii;
		}

		public Insets getBorderInsets(Component c) {
			return new Insets(this.radii, this.radii, this.radii, this.radii);
		}

		public boolean isBorderOpaque() {
			return true;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(color);
			g2.setStroke(new BasicStroke(thickness));
			g2.drawRoundRect(x + thickness/2, y + thickness/2,
					width - thickness, height - thickness, radii, radii);
			g2.dispose();
		}
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
			port = txtPort.getText().trim();

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

			if (!UserDAO.isUserExist(userName)) {
				JOptionPane.showMessageDialog(
						this,
						"Tên đăng nhập không tồn tại. Vui lòng đăng ký!",
						"Đăng nhập thất bại",
						JOptionPane.ERROR_MESSAGE
				);
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