package client;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VoiceReceiveThread extends Thread {
    private DatagramSocket socket;
    private SourceDataLine speaker;

    public VoiceReceiveThread(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            speaker = AudioSystem.getSourceDataLine(format);
            speaker.open(format);
            speaker.start();

            byte[] buffer = new byte[512];
            DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);

            while (!isInterrupted()) {
                socket.receive(pkt);
                speaker.write(pkt.getData(), 0, pkt.getLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}