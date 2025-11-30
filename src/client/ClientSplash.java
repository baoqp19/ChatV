package client;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ClientSplash extends JWindow {

    private JProgressBar progressBar;
    private JLabel percentageLabel;
    private JLabel titleLabel;

    public ClientSplash() {
        initUI();
    }

    private void initUI() {

        // Panel có gradient
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color color1 = new Color(255, 153, 51);
                Color color2 = new Color(255, 204, 102);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);

                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };

        panel.setLayout(null);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        titleLabel = new JLabel("VKU CLIENT - INTERNAL CHAT APP", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(20, 30, 560, 40);
        panel.add(titleLabel);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setBounds(50, 150, 500, 25);
        progressBar.setStringPainted(false);
        progressBar.setForeground(new Color(0, 102, 204));
        progressBar.setBackground(Color.WHITE);
        progressBar.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        panel.add(progressBar);

        // Percentage text
        percentageLabel = new JLabel("0%", SwingConstants.CENTER);
        percentageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        percentageLabel.setForeground(Color.WHITE);
        percentageLabel.setBounds(0, 190, 600, 30);
        panel.add(percentageLabel);

        // Add panel
        getContentPane().add(panel);

        setSize(600, 350);
        setLocationRelativeTo(null);

        setShape(new RoundRectangle2D.Double(0, 0, 600, 350, 30, 30));
    }

    public void startSplash() {
        try {
            for (int i = 0; i <= 100; i++) {
                Thread.sleep(20);
                progressBar.setValue(i);
                percentageLabel.setText(i + "% LOADING");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Main
    public static void main(String[] args) {

        ClientSplash splash = new ClientSplash();
        splash.setVisible(true);
        splash.startSplash();
        splash.dispose();

        // Mở client sau khi splash xong
        new StartClientFrame().setVisible(true);
    }
}
