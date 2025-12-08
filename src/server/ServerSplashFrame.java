package server;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ServerSplashFrame extends JWindow {

    private JProgressBar progressBar;
    private JLabel percentageLabel;
    private JLabel titleLabel;

    public ServerSplashFrame() {
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient xanh đậm → Server mạnh mẽ
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0, 80, 160),
                        0, getHeight(), new Color(0, 130, 220)
                );
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 35, 35);

                // Viền sáng nhẹ
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 32, 32);
                g2d.dispose();
            }
        };
        panel.setLayout(null);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Tiêu đề lớn, đẹp
        titleLabel = new JLabel("VKU SERVER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(20, 40, 560, 70);
        panel.add(titleLabel);

        JLabel subtitle = new JLabel("Internal Chat Server • Port 3939", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subtitle.setForeground(new Color(220, 240, 255));
        subtitle.setBounds(20, 105, 560, 30);
        panel.add(subtitle);

        progressBar = new JProgressBar();
        progressBar.setBounds(50, 170, 500, 38);
        progressBar.setStringPainted(false);
        progressBar.setString(""); // Bắt buộc tắt chữ
        progressBar.setForeground(Color.WHITE);
        progressBar.setBackground(new Color(255, 255, 255, 50));
        progressBar.setBorderPainted(false);

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
                g2d.fillRoundRect(0, 0, w, h, 19, 19);

                // Thanh trắng sáng
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, fill, h, 19, 19);

                g2d.dispose();
            }

            @Override
            protected void paintString(Graphics g, int x, int y, int w, int h, int fill, Insets i) {
            }
        });
        panel.add(progressBar);

        percentageLabel = new JLabel("0%", SwingConstants.CENTER);
        percentageLabel.setFont(new Font("Consolas", Font.BOLD, 30));
        percentageLabel.setForeground(Color.WHITE);
        percentageLabel.setBounds(0, 220, 600, 50);
        panel.add(percentageLabel);

        // Footer nhẹ
        JLabel footer = new JLabel("© 2025 VKU - Đồ án Mạng Máy Tính", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        footer.setForeground(new Color(200, 230, 255));
        footer.setBounds(0, 300, 600, 20);
        panel.add(footer);

        getContentPane().add(panel);
        setSize(600, 350);
        setLocationRelativeTo(null);
        setShape(new RoundRectangle2D.Double(0, 0, 600, 350, 35, 35));
    }

    public void startSplash() {
        try {
            for (int i = 0; i <= 100; i++) {
                Thread.sleep(30);
                progressBar.setValue(i);
                percentageLabel.setText(i + "%");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServerSplashFrame splash = new ServerSplashFrame();
        splash.setVisible(true);
        splash.startSplash();
        splash.dispose();

        // Mở ServerFrame như cũ của bạn
        new ServerFrame().setVisible(true);
    }
}