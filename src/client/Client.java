package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

import data.Peer;
import database.UserDAO;
import tags.Decode;
import tags.Encode;
import tags.Tags;

public class Client {

	public ArrayList<Peer> clientList = new ArrayList<>();
	private ClientServer server;

	private final InetAddress serverIP;
	private final int serverPort;
	private final int clientPort;

	private final String username;
	private boolean isRunning = true;

	private static final int TIMEOUT = 10000;

	private Socket socketClient;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	public String getUsername() {
		return username;
	}

	// ========================= CONSTRUCTOR =========================
	public Client(String ip, int portClient, String username, String rawUserList, int portServer) throws Exception {
		this.serverIP = InetAddress.getByName(ip);
		this.clientPort = portClient;
		this.username = username;
		this.serverPort = portServer;

		System.out.println("Client Started:");
		System.out.println(" → Server IP   : " + ip);
		System.out.println(" → Server Port : " + portServer);
		System.out.println(" → Client Port : " + portClient);
		System.out.println(" → Username    : " + username);

		this.clientList = Decode.getAllUser(rawUserList);

		// Thread update friend list
		new Thread(this::updateFriendList).start();

		// Start listening client
		server = new ClientServer(this);

		// Start periodic request thread
		new RequestThread().start();
	}

	// ========================= GETTER =========================
	public int getClientPort() {
		return clientPort;
	}

	// ========================= SEND REQUEST TO SERVER =========================
	private void sendRequest() throws Exception {
		socketClient = new Socket();
		SocketAddress address = new InetSocketAddress(serverIP, serverPort);

		socketClient.connect(address);

		// Send request
		out = new ObjectOutputStream(socketClient.getOutputStream());
		out.writeObject(Encode.sendRequest(username));
		out.flush();

		// Receive response
		in = new ObjectInputStream(socketClient.getInputStream());
		String msg = (String) in.readObject();

		// Debug
		System.out.println("Server Response: " + msg);

		clientList = Decode.getAllUser(msg);

		in.close();
		socketClient.close();

		// Update friend list
		new Thread(this::updateFriendList).start();
	}

	// ========================= PERIODIC REQUEST THREAD =========================
	private class RequestThread extends Thread {
		@Override
		public void run() {
			while (isRunning) {
				try {
					Thread.sleep(TIMEOUT);
					sendRequest();
				} catch (Exception e) {
					System.err.println("Connection lost. Retrying...");
				}
			}
		}
	}

	// ========================= INITIAL NEW CHAT =========================
	public void startChat(String ip, int port, String guest) throws Exception {
		Socket chatSocket = new Socket(InetAddress.getByName(ip), port);

		// Request chat
		ObjectOutputStream requestOut = new ObjectOutputStream(chatSocket.getOutputStream());
		requestOut.writeObject(Encode.sendRequestChat(username));
		requestOut.flush();

		ObjectInputStream responseIn = new ObjectInputStream(chatSocket.getInputStream());
		String response = (String) responseIn.readObject();

		if (response.equals(Tags.CHAT_DENY_TAG)) {
			MainFrame.request("Your friend denied the connection!", false);
			chatSocket.close();
			return;
		}

		// Open chat window
		new ChatFrame(username, guest, chatSocket, clientPort);
	}

	// ========================= EXIT CLIENT =========================
	public void exit() throws IOException, ClassNotFoundException {
		UserDAO.updateUserStatus(username, "OFFLINE");
		isRunning = false;

		socketClient = new Socket();
		SocketAddress addressServer = new InetSocketAddress(serverIP, serverPort);
		socketClient.connect(addressServer);

		String msg = Encode.exit(username);

		out = new ObjectOutputStream(socketClient.getOutputStream());
		out.writeObject(msg);
		out.flush();
		out.close();

		server.exit();
	}

	// ========================= UPDATE FRIEND LIST =========================
	public void updateFriendList() {
		MainFrame.resetList();

		for (Peer p : clientList) {
			if (!p.getName().equals(username)) {
				MainFrame.updateFriendMainFrame(p.getName());
			}
		}
	}
}
