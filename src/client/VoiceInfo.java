package client;

import java.net.InetAddress;
import java.util.Objects;

/**
 * Holds voice call information for a peer connection.
 * Java 21 compatible with immutable design pattern.
 */
public final class VoiceInfo {

    private final InetAddress peerAddress;
    private final int peerVoicePort;
    private final String peerName;
    private final long createdTime;

    /**
     * Creates voice information for a peer
     * 
     * @param addr Peer's IP address
     * @param port Peer's voice port
     * @param name Peer's username
     */
    public VoiceInfo(InetAddress addr, int port, String name) {
        this.peerAddress = Objects.requireNonNull(addr, "Address cannot be null");
        this.peerVoicePort = port;
        this.peerName = Objects.requireNonNull(name, "Name cannot be null");
        this.createdTime = System.currentTimeMillis();
    }

    /**
     * Gets the peer's IP address
     * 
     * @return InetAddress of the peer
     */
    public InetAddress getPeerAddress() {
        return peerAddress;
    }

    /**
     * Gets the peer's voice port
     * 
     * @return Voice port number
     */
    public int getPeerVoicePort() {
        return peerVoicePort;
    }

    /**
     * Gets the peer's username
     * 
     * @return Username of the peer
     */
    public String getPeerName() {
        return peerName;
    }

    /**
     * Gets the creation time of this VoiceInfo
     * 
     * @return Creation timestamp in milliseconds
     */
    public long getCreatedTime() {
        return createdTime;
    }

    /**
     * Checks if this voice connection is still valid
     * 
     * @param timeoutMillis Maximum age in milliseconds
     * @return true if not older than timeout
     */
    public boolean isValid(long timeoutMillis) {
        return System.currentTimeMillis() - createdTime < timeoutMillis;
    }

    @Override
    public String toString() {
        return String.format("VoiceInfo{name='%s', address=%s, port=%d}",
                peerName, peerAddress.getHostAddress(), peerVoicePort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof VoiceInfo other))
            return false;
        return peerName.equals(other.peerName) &&
                peerAddress.equals(other.peerAddress) &&
                peerVoicePort == other.peerVoicePort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerName, peerAddress, peerVoicePort);
    }
}