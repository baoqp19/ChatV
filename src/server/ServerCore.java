package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import data.Peer;
import database.DBUtil;
import tags.Decode;
import tags.Tags;

public class ServerCore {

	private ArrayList<Peer> dataPeer = null;
	private ServerSocket server;
	private Socket connection;
	private ObjectOutputStream obOutputClient;
	private ObjectInputStream obInputStream;
	public boolean isStop = false, isExit = false;
	private static int portServer;

	ArrayList<Peer> getListUser() {
		return dataPeer;
	}

	// Intial server socket
	public ServerCore(int port) throws Exception {
		server = new ServerSocket(port);
		dataPeer = new ArrayList<Peer>();
		portServer = port;
		(new WaitForConnect()).start();
	}

	// show status of state
	private String sendSessionAccept() throws Exception {
		String msg = Tags.SESSION_ACCEPT_OPEN_TAG;
		int size = dataPeer.size();
		for (int i = 0; i < size; i++) {
			Peer peer = dataPeer.get(i);
			msg += Tags.PEER_OPEN_TAG;
			msg += Tags.PEER_NAME_OPEN_TAG;
			msg += peer.getName();
			msg += Tags.PEER_NAME_CLOSE_TAG;
			msg += Tags.IP_OPEN_TAG;
			msg += peer.getHost();
			msg += Tags.IP_CLOSE_TAG;
			msg += Tags.PORT_OPEN_TAG;
			msg += peer.getPort();
			msg += Tags.PORT_CLOSE_TAG;
			msg += Tags.PEER_CLOSE_TAG;
		}
		msg += Tags.SESSION_ACCEPT_CLOSE_TAG;
		return msg;
	}

	// close server
	public void stopserver() throws Exception {
		isStop = true;
		server.close();
		connection.close();
	}

	// client connect to server
	private boolean waitForConnection() throws Exception {
		connection = server.accept();
		obInputStream = new ObjectInputStream(connection.getInputStream());
		String msg = (String) obInputStream.readObject();

		ArrayList<String> getData = Decode.getUser(msg);
		if (getData != null) {
			if (!isExsistName(getData.get(0))) {
				saveNewPeer(getData.get(0), connection.getInetAddress().toString(), Integer.parseInt(getData.get(1)));
				ServerFrame.updateNumberClient();
			} else
				return false;
		} else {
			int size = dataPeer.size();

			Decode.updatePeerOnline(dataPeer, msg);
			if (size != dataPeer.size()) {
				isExit = true;
				ServerFrame.decreaseNumberClient();
			}
		}
		return true;
	}

	private void saveNewPeer(String user, String ip, int port) throws Exception {
		Connection conn = DBUtil.getConnection();
		String sql = "INSERT INTO peers(username, ip, port, status) " +
				"VALUES (?, ?, ?, 'ONLINE') " +
				"ON DUPLICATE KEY UPDATE ip=?, port=?, status='ONLINE'";

		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setString(1, user);
		ps.setString(2, ip);
		ps.setInt(3, port);
		ps.setString(4, ip);
		ps.setInt(5, port);

		ps.executeUpdate();

		Peer newPeer = new Peer();
		if (dataPeer.size() == 0)
			dataPeer = new ArrayList<Peer>();
		newPeer.setPeer(user, ip, port);
		dataPeer.add(newPeer);
	}

	private boolean isExsistName(String name) throws Exception {
		if (dataPeer == null)
			return false;
		int size = dataPeer.size();
		for (int i = 0; i < size; i++) {
			Peer peer = dataPeer.get(i);
			if (peer.getName().equals(name))
				return true;
		}
		return false;
	}

	public class WaitForConnect extends Thread {

		@Override
		public void run() {
			super.run();
			try {
				while (!isStop) {
					if (waitForConnection()) {
						if (isExit) {
							isExit = false;
						} else {
							obOutputClient = new ObjectOutputStream(connection.getOutputStream());
							obOutputClient.flush(); // Flush to write stream header
							obOutputClient.writeObject(sendSessionAccept());
							obOutputClient.flush();
							// Keep the stream open - do NOT close it
							// The connection will handle multiple messages
						}
					} else {
						obOutputClient = new ObjectOutputStream(connection.getOutputStream());
						obOutputClient.flush();
						obOutputClient.writeObject(Tags.SESSION_DENY_TAG);
						obOutputClient.flush();
						// Keep the stream open
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}