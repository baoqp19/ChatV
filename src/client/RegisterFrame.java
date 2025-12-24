package client;

import database.UserDAO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

public class RegisterFrame extends JFrame implements ActionListener {

    private JTextField txtUsername, txtFullName, txtEmail;
    private JPasswordField txtPassword, txtConfirm;
    private JButton btnRegister, btnBack;

    private final Pattern checkName = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]{0,19}$");

    public RegisterFrame() {
        initUI();
        pack(); // Tự động điều chỉnh kích thước dựa trên nội dung
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setTitle("VKU Chat - Đăng ký tài khoản");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 248, 255));
        mainPanel.setBorder(new EmptyBorder(40, 50, 50, 50));
        setContentPane(mainPanel);

        // === TIÊU ĐỀ ===
        JLabel title = new JLabel("ĐĂNG KÝ TÀI KHOẢN", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setForeground(new Color(0, 102, 204));
        title.setBorder(new EmptyBorder(0, 0, 40, 0));
        mainPanel.add(title, BorderLayout.NORTH);

        // === FORM CARD ===
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(180, 210, 255), 2, 20),
                new EmptyBorder(40, 40, 40, 40) // padding đều trong card
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(15, 20, 15, 20); // lề trái-phải đều, khoảng cách dọc rộng rãi

        // Khởi tạo fields
        txtUsername = createStyledTextField("");
        txtPassword = createStyledPasswordField();
        txtConfirm = createStyledPasswordField();
        txtFullName = createStyledTextField("");
        txtEmail = createStyledTextField("");

        addLabeledField(cardPanel, gbc, "Username:", txtUsername);
        addLabeledField(cardPanel, gbc, "Mật khẩu:", txtPassword);
        addLabeledField(cardPanel, gbc, "Xác nhận mật khẩu:", txtConfirm);
        addLabeledField(cardPanel, gbc, "Họ và tên:", txtFullName);
        addLabeledField(cardPanel, gbc, "Email:", txtEmail);

        mainPanel.add(cardPanel, BorderLayout.CENTER);

        // === NÚT BẤM & HINT ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(30, 0, 0, 0));

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonGroup.setOpaque(false);

        btnRegister = createPrimaryButton("ĐĂNG KÝ", 280);
        btnBack = createSecondaryButton("QUAY LẠI", 160);

        btnRegister.addActionListener(this);
        btnBack.addActionListener(this);

        buttonGroup.add(btnRegister);
        buttonGroup.add(btnBack);

        bottomPanel.add(buttonGroup, BorderLayout.CENTER);

        JLabel lblHint = new JLabel("<html><div style='text-align: center; color: #888888; font-style: italic;'>"
                + "Thông tin sẽ được lưu trữ an toàn<br>Vui lòng nhập đầy đủ và chính xác"
                + "</div></html>");
        lblHint.setHorizontalAlignment(SwingConstants.CENTER);
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblHint.setBorder(new EmptyBorder(25, 0, 0, 0));

        bottomPanel.add(lblHint, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    // Helper: label trên, field dưới (giữ nguyên ý bạn)
    private void addLabeledField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(new Color(60, 60, 60));

        row.add(label, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);

        panel.add(row, gbc);
    }

    // Text field rộng, rõ ràng, có viền đẹp
    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(100, 30));    // rộng và cao rõ ràng
        field.setMinimumSize(new Dimension(100, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 2),
                BorderFactory.createEmptyBorder(0, 15, 0, 15)
        ));
        return field;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(100, 30));
        field.setMinimumSize(new Dimension(100, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 2),
                BorderFactory.createEmptyBorder(0, 15, 0, 15)
        ));
        return field;
    }

    // Nút chính (xanh)
    private JButton createPrimaryButton(String text, int width) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btn.setPreferredSize(new Dimension(width, 62));
        btn.setBackground(new Color(0, 122, 255));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0, 105, 220)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(0, 122, 255)); }
        });
        return btn;
    }

    // Nút phụ (xám)
    private JButton createSecondaryButton(String text, int width) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btn.setPreferredSize(new Dimension(width, 62));
        btn.setBackground(new Color(245, 245, 245));
        btn.setForeground(new Color(50, 50, 50));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(230, 230, 230)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(245, 245, 245)); }
        });
        return btn;
    }

    // RoundedBorder class
    static class RoundedBorder implements Border {
        private final Color color;
        private final int thickness;
        private final int radii;

        public RoundedBorder(Color color, int thickness, int radii) {
            this.color = color;
            this.thickness = thickness;
            this.radii = radii;
        }

        public Insets getBorderInsets(Component c) { return new Insets(radii, radii, radii, radii); }
        public boolean isBorderOpaque() { return true; }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness/2, y + thickness/2, w - thickness, h - thickness, radii, radii);
            g2.dispose();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnBack) {
            new StartClientFrame().setVisible(true);
            dispose();
            return;
        }

        if (e.getSource() == btnRegister) {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            String confirm = new String(txtConfirm.getPassword());
            String fullName = txtFullName.getText().trim();
            String email = txtEmail.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username và mật khẩu không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!checkName.matcher(username).matches()) {
                JOptionPane.showMessageDialog(this,
                        "Username không hợp lệ!\nChỉ chứa chữ cái, số, gạch dưới, bắt đầu bằng chữ hoặc gạch dưới, tối đa 20 ký tự.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (UserDAO.isUserExist(username)) {
                JOptionPane.showMessageDialog(this, "Username đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = UserDAO.register(username, password, fullName, email);

            if (success) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công!\nBây giờ bạn có thể đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                new StartClientFrame().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Đăng ký thất bại! Vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}