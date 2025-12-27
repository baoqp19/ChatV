package client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.imageio.ImageIO;

/**
 * Video sending thread using standard Java libraries only.
 * Uses Robot class to capture screen region as a simple webcam alternative.
 * For real webcam: Download LTI-CIVIL or OpenIMAJ (both work without Maven)
 */
public class VideoSendThread extends Thread {
    private DatagramSocket socket;
    private InetAddress peerIP;
    private int peerPort;
    private volatile boolean running = true;
    private ErrorCallback errorCallback;
    private JLabel previewLabel;
    private Robot robot;

    public interface ErrorCallback {
        void onError(String message);
    }

    public VideoSendThread(DatagramSocket socket, InetAddress ip, int port, JLabel preview) {
        this.socket = socket;
        this.peerIP = ip;
        this.peerPort = port;
        this.previewLabel = preview;
    }

    public void setErrorCallback(ErrorCallback callback) {
        this.errorCallback = callback;
    }

    @Override
    public void run() {
        try {
            System.out.println("[VideoSend] Starting video capture...");

            // Use Robot to capture screen - works with standard Java
            robot = new Robot();

            // Capture a corner away from the call window to avoid the mirror effect
            Rectangle captureRect = new Rectangle(0, 0, 320, 240);

            System.out.println("[VideoSend] Capturing screen region (320x240)");
            System.out.println("[VideoSend] Note: Using screen capture. For real webcam, add OpenIMAJ library");

            while (running && !isInterrupted()) {
                try {
                    // Capture screen region
                    BufferedImage image = robot.createScreenCapture(captureRect);

                    if (image != null) {
                        // Show preview
                        if (previewLabel != null) {
                            BufferedImage preview = scaleImage(image, 160, 120);
                            SwingUtilities.invokeLater(() -> {
                                previewLabel.setIcon(new ImageIcon(preview));
                                previewLabel.setText("");
                            });
                        }

                        // Compress with default JPEG settings
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", baos);
                        byte[] imageData = baos.toByteArray();

                        // Send if under packet size limit
                        if (imageData.length < 60000) {
                            DatagramPacket packet = new DatagramPacket(imageData, imageData.length, peerIP, peerPort);
                            socket.send(packet);
                        }

                        // Limit frame rate to ~10 FPS
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (AWTException e) {
            String error = "Screen capture not available: " + e.getMessage();
            System.err.println("[VideoSend] " + error);
            if (errorCallback != null) {
                errorCallback.onError(error);
            }
        } catch (Exception e) {
            System.err.println("[VideoSend] Error: " + e.getMessage());
            e.printStackTrace();
            if (errorCallback != null) {
                errorCallback.onError("Video send error: " + e.getMessage());
            }
        } finally {
            System.out.println("[VideoSend] Video sender stopped");
        }
    }

    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    public void stopVideo() {
        running = false;
        interrupt();
    }
}
