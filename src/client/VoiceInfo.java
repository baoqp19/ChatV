package client;

import java.net.InetAddress;

public class VoiceInfo {
    public InetAddress peerAddress;
    public int peerVoicePort;
    public String peerName;

    public VoiceInfo(InetAddress addr, int port, String name) {
        this.peerAddress = addr;
        this.peerVoicePort = port;
        this.peerName = name;
    }
}