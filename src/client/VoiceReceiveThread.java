package client;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VoiceReceiveThread extends Thread {
    private DatagramSocket socket;
    private SourceDataLine speakers;

    public VoiceReceiveThread(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Định dạng audio: 44.1kHz, 16-bit, mono, signed, little-endian
            AudioFormat format = new AudioFormat(
                    44100.0f,
                    16,
                    1,
                    true,
                    false
            );

            // Tạo line phát audio
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.out.println("Audio format not supported: " + format);
                return;
            }

            speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakers.open(format);
            speakers.start();

            byte[] buffer = new byte[512]; // kích thước gói nhỏ để giảm latency
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Voice receive started...");

            while (true) {
                socket.receive(packet);
                speakers.write(packet.getData(), 0, packet.getLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (speakers != null) {
                speakers.drain();
                speakers.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
