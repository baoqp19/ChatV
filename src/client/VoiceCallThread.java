package client;

import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
public class VoiceCallThread extends Thread {
    private DatagramSocket socket;
    private InetAddress peerAddress;
    private int peerPort;
    private TargetDataLine microphone;

    public VoiceCallThread(DatagramSocket socket, InetAddress peerAddress, int port) {
        this.socket = socket;
        this.peerAddress = peerAddress;
        this.peerPort = port;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[512]; // packet nhỏ để giảm latency
            while (true) {
                int count = microphone.read(buffer, 0, buffer.length);
                if (count > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, count, peerAddress, peerPort);
                    socket.send(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}