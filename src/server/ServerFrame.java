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
		setTitle("VKU Server - Internal Chat System");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 680);
		setLocationRelativeTo(null);
		setResizable(false);

		// Background toàn bộ
		getContentPane().setBackground(new Color(245, 250, 255));

		// HEADER XANH VKU
		JPanel header = new JPanel();
		header.setBackground(new Color(0, 102, 204));
		header.setPreferredSize(new Dimension(0, 100));
		header.setLayout(new BorderLayout());

		JLabel lblTitle = new JLabel("VKU SERVER", SwingConstants.CENTER);
		lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 48));
		lblTitle.setForeground(Color.WHITE);
		header.add(lblTitle, BorderLayout.CENTER);

		JLabel lblSubtitle = new JLabel("Internal Chat Server • Port 3939", SwingConstants.CENTER);
		lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
		lblSubtitle.setForeground(new Color(200, 240, 255));
		header.add(lblSubtitle, BorderLayout.SOUTH);

		add(header, BorderLayout.NORTH);

		// MAIN CONTENT
		JPanel main = new JPanel(new BorderLayout(20, 20));
		main.setBackground(new Color(245, 250, 255));
		main.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
		add(main, BorderLayout.CENTER);

		// LEFT PANEL: Thông tin & nút điều khiển
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.setBackground(Color.WHITE);
		leftPanel.setBorder(BorderFactory.createLineBorder(new Color(0, 102, 204), 2, true));
		leftPanel.setPreferredSize(new Dimension(380, 0));

		// Network Info
		JPanel networkPanel = new JPanel(new GridLayout(2, 2, 15, 15));
		networkPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
				" Network Configuration ", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16), new Color(0, 102, 204)));
		networkPanel.setBackground(Color.WHITE);
		networkPanel.setMaximumSize(new Dimension(360, 110));

		networkPanel.add(createLabel("IP Address:"));
		txtIP = new JTextField(15);
		txtIP.setEditable(false);
		txtIP.setFont(new Font("Consolas", Font.BOLD, 18));
		txtIP.setForeground(new Color(0, 120, 0));
		txtIP.setBackground(new Color(240, 255, 240));
		networkPanel.add(txtIP);

		networkPanel.add(createLabel("Port:"));
		txtPort = new JTextField(String.valueOf(port), 15);
		txtPort.setFont(new Font("Consolas", Font.BOLD, 18));
		txtPort.setForeground(Color.RED.darker());
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

		// Server Status
		JPanel statusPanel = new JPanel(new GridLayout(2, 2, 15, 15));
		statusPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 122, 255), 2),
				" Server Status ", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16), new Color(0, 122, 255)));
		statusPanel.setBackground(Color.WHITE);
		statusPanel.setMaximumSize(new Dimension(360, 110));

		statusPanel.add(createLabel("Status:"));
		lblStatus = new JLabel("OFF");
		lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 22));
		lblStatus.setForeground(Color.RED.darker());
		statusPanel.add(lblStatus);

		statusPanel.add(createLabel("Users Online:"));
		lblUserOnline = new JLabel("0");
		lblUserOnline.setFont(new Font("Segoe UI", Font.BOLD, 28));
		lblUserOnline.setForeground(new Color(0, 150, 0));
		statusPanel.add(lblUserOnline);

		// Nút điều khiển
// Panel chứa 2 nút – trong suốt + căn giữa hoàn hảo
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());  // thần thánh cho việc căn đều
		buttonPanel.setBackground(Color.WHITE);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0)); // khoảng cách trên/dưới đẹp

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 15, 0, 15);  // khoảng cách giữa 2 nút
		gbc.fill = GridBagConstraints.HORIZONTAL;

		btnStartServer = new JButton("START");
		styleButton(btnStartServer, new Color(0, 190, 100), 20);  // xanh Spotify siêu đẹp
		gbc.gridx = 0;
		gbc.weightx = 1.0;  // chiếm đều không gian
		buttonPanel.add(btnStartServer, gbc);

		btnStopServer = new JButton("STOP");
		styleButton(btnStopServer, new Color(220, 50, 60), 20);   // đỏ Apple sang trọng
		btnStopServer.setEnabled(false);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		buttonPanel.add(btnStopServer, gbc);
		btnStopServer.setEnabled(false);

//		buttonPanel.add(btnStartServer);
//		buttonPanel.add(btnStopServer);

		// Thêm vào left panel
		leftPanel.add(networkPanel);
		leftPanel.add(Box.createVerticalStrut(20));
		leftPanel.add(statusPanel);
		leftPanel.add(Box.createVerticalStrut(25));
		leftPanel.add(buttonPanel);
		leftPanel.add(Box.createVerticalGlue());

		main.add(leftPanel, BorderLayout.WEST);

		// RIGHT PANEL: Danh sách người dùng
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(Color.WHITE);
		rightPanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(new Color(0, 102, 204), 2),
				" Online Users List ", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 16), new Color(0, 102, 204)));

		txtMessage = new JTextArea();
		txtMessage.setEditable(false);
		txtMessage.setFont(new Font("Consolas", Font.PLAIN, 17));
		txtMessage.setBackground(new Color(30, 30, 40));
		txtMessage.setForeground(new Color(0, 255, 150));
		txtMessage.setCaretColor(Color.WHITE);
		txtMessage.setMargin(new Insets(10, 10, 10, 10));

		JScrollPane scroll = new JScrollPane(txtMessage);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		rightPanel.add(scroll, BorderLayout.CENTER);

		main.add(rightPanel, BorderLayout.CENTER);

		// FOOTER
		JLabel footer = new JLabel("© 2025 VKU - Đồ án Mạng Máy Tính • Internal Chat Server", SwingConstants.CENTER);
		footer.setFont(new Font("Segoe UI", Font.ITALIC, 13));
		footer.setForeground(new Color(100, 100, 100));
		add(footer, BorderLayout.SOUTH);

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


	private JLabel createLabel(String text) {
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
		lbl.setForeground(new Color(50, 50, 100));
		return lbl;
	}

	private void styleButton(JButton btn, Color bg, int fontSize) {
		// Màu chính + màu hiệu ứng
		Color hoverColor   = bg.brighter();                 // sáng hơn khi hover
		Color pressedColor = bg.darker();                   // tối hơn khi nhấn

		btn.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
		btn.setForeground(Color.WHITE);
		btn.setBackground(bg);
		btn.setFocusPainted(false);
		btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Kích thước nhỏ gọn đẹp (tùy ý chỉnh)
		btn.setPreferredSize(new Dimension(160, 48));     // nhỏ hơn, tinh tế hơn 180x58

		// Bo tròn 16px – chuẩn app hiện đại (Zalo, Telegram, Discord đều dùng ~14-18px)
		btn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

		// Bật opaque để background hiển thị đúng
		btn.setOpaque(false);
		btn.setContentAreaFilled(false);
		btn.setBorderPainted(false);

		// Vẽ lại button với bo tròn + hiệu ứng (đẹp hơn 1000 lần cách cũ)
		btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
			@Override
			public void paint(Graphics g, JComponent c) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int w = c.getWidth();
				int h = c.getHeight();
				int arc = 16; // độ bo tròn

				// Background theo trạng thái
				if (btn.getModel().isPressed()) {
					g2.setColor(pressedColor);
				} else if (btn.getModel().isRollover()) {
					g2.setColor(hoverColor);
				} else {
					g2.setColor(bg);
				}
				g2.fillRoundRect(0, 0, w, h, arc, arc);

				g2.dispose();
				super.paint(g, c);
			}
		});
	}

	// GIỮ NGUYÊN 100% CÁC HÀM CỦA BẠN
	public static void updateMessage(String msg) {
		txtMessage.append("» " + msg + "\n");
		txtMessage.setCaretPosition(txtMessage.getDocument().getLength());
	}

	public static void updateNumberClient() {
		int number = Integer.parseInt(lblUserOnline.getText());
		lblUserOnline.setText(String.valueOf(number + 1));
		displayUser();
	}

	public static void decreaseNumberClient() {
		int number = Integer.parseInt(lblUserOnline.getText());
		if (number > 0) lblUserOnline.setText(String.valueOf(number - 1));
		displayUser();
	}

	static void displayUser() {
		txtMessage.setText("");
		if (server == null) return;
		ArrayList<Peer> list = server.getListUser();
		txtMessage.append("  NO │ USERNAME\n");
		txtMessage.append(" ───┼────────────────\n");
		for (int i = 0; i < list.size(); i++) {
			txtMessage.append(String.format(" %3d │ %s\n", i + 1, list.get(i).getName()));
		}
		if (list.isEmpty()) {
			txtMessage.append("   (No users online)\n");
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {}
			new ServerFrame().setVisible(true);
		});
	}
}