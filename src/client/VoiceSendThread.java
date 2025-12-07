package client;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class VoiceSendThread extends Thread {
    private DatagramSocket socket;
    private InetAddress peerIP;
    private int peerPort;
    private TargetDataLine mic;

    public VoiceSendThread(DatagramSocket socket, InetAddress ip, int port) {
        this.socket = socket;
        this.peerIP = ip;
        this.peerPort = port;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
            mic = AudioSystem.getTargetDataLine(format);
            mic.open(format);
            mic.start();

            byte[] buffer = new byte[512];

            while (!isInterrupted()) {
                int count = mic.read(buffer, 0, buffer.length);
                if (count > 0) {
                    DatagramPacket pkt = new DatagramPacket(buffer, count, peerIP, peerPort);
                    socket.send(pkt);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}