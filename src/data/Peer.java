package data;

/**
 * Represents a peer (remote user) in the VKU Chat network.
 * Java 21 compatible.
 */
public class Peer {

	private String namePeer = "";
	private String hostPeer = "";
	private int portPeer = 0;

	/**
	 * Creates a new Peer with specified properties
	 * 
	 * @param name Peer username
	 * @param host Peer IP address or hostname
	 * @param port Peer port number
	 */
	public void setPeer(String name, String host, int port) {
		this.namePeer = name;
		this.hostPeer = host;
		this.portPeer = port;
	}

	/**
	 * Sets the peer's username
	 * 
	 * @param name Username
	 */
	public void setName(String name) {
		this.namePeer = name;
	}

	/**
	 * Sets the peer's host address
	 * 
	 * @param host IP address or hostname
	 */
	public void setHost(String host) {
		this.hostPeer = host;
	}

	/**
	 * Sets the peer's port number
	 * 
	 * @param port Port number
	 */
	public void setPort(int port) {
		this.portPeer = port;
	}

	/**
	 * Gets the peer's username
	 * 
	 * @return Username
	 */
	public String getName() {
		return namePeer;
	}

	/**
	 * Gets the peer's host address
	 * 
	 * @return IP address or hostname
	 */
	public String getHost() {
		return hostPeer;
	}

	/**
	 * Gets the peer's port number
	 * 
	 * @return Port number
	 */
	public int getPort() {
		return portPeer;
	}

	@Override
	public String toString() {
		return String.format("Peer{name='%s', host='%s', port=%d}", namePeer, hostPeer, portPeer);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Peer other))
			return false;
		return namePeer.equals(other.namePeer) &&
				hostPeer.equals(other.hostPeer) &&
				portPeer == other.portPeer;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(namePeer, hostPeer, portPeer);
	}
}
