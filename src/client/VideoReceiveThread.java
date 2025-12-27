package client;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import javax.imageio.ImageIO;

public class VideoReceiveThread extends Thread {
    private DatagramSocket socket;
    private volatile boolean running = true;
    private ErrorCallback errorCallback;
    private JLabel displayLabel; // For showing received video

    public interface ErrorCallback {
        void onError(String message);
    }

    public VideoReceiveThread(DatagramSocket socket, JLabel display) {
        this.socket = socket;
        this.displayLabel = display;
    }

    public void setErrorCallback(ErrorCallback callback) {
        this.errorCallback = callback;
    }

    @Override
    public void run() {
        try {
            System.out.println("[VideoReceive] Starting video receiver...");

            byte[] buffer = new byte[65000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running && !isInterrupted()) {
                try {
                    socket.receive(packet);

                    if (packet.getLength() > 0) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(
                                packet.getData(), 0, packet.getLength());
                        BufferedImage image = ImageIO.read(bais);

                        if (image != null && displayLabel != null) {
                            SwingUtilities.invokeLater(() -> {
                                displayLabel.setIcon(new ImageIcon(image));
                            });
                        }
                    }
                } catch (Exception e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[VideoReceive] Error: " + e.getMessage());
            e.printStackTrace();
            if (errorCallback != null) {
                errorCallback.onError("Video receive error: " + e.getMessage());
            }
        } finally {
            System.out.println("[VideoReceive] Video receiver stopped");
        }
    }

    public void stopVideo() {
        running = false;
        interrupt();
    }
}
