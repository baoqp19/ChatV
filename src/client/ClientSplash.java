package client;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ClientSplash extends JWindow {

    private JProgressBar progressBar;
    private JLabel percentageLabel;

    public ClientSplash() {
        initUI();
    }

    private void initUI() {
        // Buộc dùng System LookAndFeel (Windows/macOS) → không bị Nimbus vẽ % thừa
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Gradient xanh VKU sang trọng
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 102, 204),
                        0, getHeight(), new Color(0, 160, 255));
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);

                // Viền sáng nhẹ
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.drawRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 37, 37);
                g2d.dispose();
            }
        };
        panel.setLayout(null);

        // Tiêu đề
        JLabel title = new JLabel("VKU CHAT", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 56));
        title.setForeground(Color.WHITE);
        title.setBounds(0, 60, 600, 80);
        panel.add(title);

        JLabel subtitle = new JLabel("Internal Chat Application - 2025", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 19));
        subtitle.setForeground(new Color(220, 240, 255));
        subtitle.setBounds(0, 130, 600, 40);
        panel.add(subtitle);

        // Thanh progress – ĐÃ FIX 100% KHÔNG CÓ % Ở GIỮA
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setBounds(80, 200, 440, 42);
        progressBar.setStringPainted(false);
        progressBar.setString("");                    // Bắt buộc trống
        progressBar.setForeground(Color.WHITE);
        progressBar.setBackground(new Color(255, 255, 255, 50));
        progressBar.setBorderPainted(false);

        // Custom UI – tắt hoàn toàn chữ %
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();
                int fill = (int) (w * progressBar.getPercentComplete());

                // Nền mờ
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.fillRoundRect(0, 0, w, h, 21, 21);

                // Thanh trắng sáng
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, fill, h, 21, 21);

                g2d.dispose();
            }

            @Override
            protected void paintString(Graphics g, int x, int y, int w, int h, int fill, Insets i) {
                // Không vẽ gì cả → tắt 100% chữ %
            }
        });
        panel.add(progressBar);

        // Số % ở dưới (giữ lại)
        percentageLabel = new JLabel("0%", SwingConstants.CENTER);
        percentageLabel.setFont(new Font("Consolas", Font.BOLD, 34));
        percentageLabel.setForeground(Color.WHITE);
        percentageLabel.setBounds(0, 245, 600, 50);
        panel.add(percentageLabel);

        // Footer
        JLabel footer = new JLabel("© 2025 VKU - Đồ án Mạng Máy Tính", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        footer.setForeground(new Color(200, 230, 255));
        footer.setBounds(0, 305, 600, 20);
        panel.add(footer);

        getContentPane().add(panel);
        setSize(600, 350);
        setLocationRelativeTo(null);
        // Bo góc cửa sổ (Java 21 hỗ trợ tốt)
        setShape(new RoundRectangle2D.Double(0, 0, 600, 350, 40, 40));
    }

    public void startSplash() {
        setVisible(true);

        Thread.startVirtualThread(() -> {
            try {
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(30);
                    final int value = i;
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(value);
                        percentageLabel.setText(value + "%");
                        if (value > 85) {
                            percentageLabel.setForeground(value % 2 == 0 ? Color.WHITE : new Color(255, 255, 180));
                        }
                    });
                }

                // Khi xong → đóng splash, mở client
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    try {
                        new StartClientFrame().setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientSplash().startSplash());
    }
}