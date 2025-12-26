package client;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import data.DataFile;
import tags.Decode;
import tags.Encode;
import tags.Tags;

import static tags.Encode.sendMessage;

public class ChatFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String URL_DIR = System.getProperty("user.dir");
    private static final int EMOJI_BUTTON_WIDTH = 44;
    private static final int EMOJI_BUTTON_HEIGHT = 41;

    // UI Components
    private JPanel contentPane;
    private JTextPane txtDisplayMessage;
    private JTextField txtMessage;
    private JButton btnSend, btnSendFile;
    private JLabel lblReceive;
    private JProgressBar progressBar;

    // Connection & Data
    private final Socket socketChat;
    private final String nameUser;
    private final String nameGuest;
    private final ChatRoom chat;
    private final ControllerChatFrame frameChat = new ControllerChatFrame();

    private boolean isStop = false;
    private int port;
    private int portVoice;

    public ChatFrame(String user, String guest, Socket socket, int port) throws Exception {
        this(user, guest, socket, port, port);
    }

    public ChatFrame(String user, String guest, Socket socket, int port, int portVoice) throws Exception {
        this.nameUser = user;
        this.nameGuest = guest;
        this.socketChat = socket;
        this.port = port;
        this.portVoice = portVoice;

        this.chat = new ChatRoom(socketChat, nameUser, nameGuest);
        this.chat.start();

        EventQueue.invokeLater(this::initializeUI);
        this.setVisible(true);
    }

    // ============ MESSAGE DISPLAY METHODS ============

    public void updateChat_receive(String msg) {
        String html = buildMessageHtml(msg, "left", "#f1f0f0", "black");
        frameChat.appendToPane(txtDisplayMessage, html);
    }

    public void updateChat_send(String msg) {
        String html = buildMessageHtml(msg, "right", "#0084ff", "white");
        frameChat.appendToPane(txtDisplayMessage, html);
    }

    public void updateChat_notify(String msg) {
        String html = buildMessageHtml(msg, "right", "#f1c40f", "white");
        frameChat.appendToPane(txtDisplayMessage, html);
    }

    public void updateChat_send_Symbol(String msg) {
        String html = "<table style='width: 100%;'>" +
                "<tr align='right'>" +
                "<td style='width: 59%;'></td>" +
                "<td style='width: 40%;'>" + msg + "</td>" +
                "</tr>" +
                "</table>";
        frameChat.appendToPane(txtDisplayMessage, html);
    }

    private String buildMessageHtml(String msg, String align, String bgColor, String textColor) {
        String time = String.format("%d:%d", LocalDateTime.now().getHour(), LocalDateTime.now().getMinute());
        // Escape % characters in message to prevent format string interpretation
        String safeMsgToDisplay = msg.replace("%", "%%");

        if ("left".equals(align)) {
            // Messages on the left: message cell first, then empty space
            return String.format(
                    "<table style='color: %s; clear:both; width: 100%%;'>" +
                            "<tr align='left'>" +
                            "<td style='width: 40%%; background-color: %s;'>%s<br>%s</td>" +
                            "<td style='width: 60%%;'></td>" +
                            "</tr>" +
                            "</table>",
                    textColor, bgColor, time, safeMsgToDisplay);
        } else {
            // Messages on the right: empty space first, then message cell
            return String.format(
                    "<table style='color: %s; clear:both; width: 100%%;'>" +
                            "<tr align='right'>" +
                            "<td style='width: 60%%;'></td>" +
                            "<td style='width: 40%%; background-color: %s;'>%s<br>%s</td>" +
                            "</tr>" +
                            "</table>",
                    textColor, bgColor, time, safeMsgToDisplay);
        }
    }

    // ============ UI INITIALIZATION ============

    private void initializeUI() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setupWindowCloseListener();
        setupFrameProperties();

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        addHeaderPanel();
        addMessageDisplayPanel();
        addEmojiPanel();
        addMessageInputPanel();
        addReceiveLabel();
    }

    private void setupWindowCloseListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                closeChat();
            }
        });
    }

    private void closeChat() {
        try {
            isStop = true;
            dispose();
            chat.sendMessage(Tags.CHAT_CLOSE_TAG);
            chat.stopChat();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // hehe

    private void setupFrameProperties() {
        setResizable(false);
        setTitle("BOX CHAT");
        setBounds(100, 100, 576, 595);
    }

    private void addHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setBounds(0, 0, 573, 67);
        contentPane.add(panel);
        panel.setLayout(null);

        // Logo
        JLabel lblLogo = new JLabel();
        lblLogo.setIcon(new ImageIcon(Objects.requireNonNull(
                ChatFrame.class.getResource("/image/VKU64.png"))));
        lblLogo.setBounds(20, 0, 66, 67);
        panel.add(lblLogo);

        // Guest name
        JLabel nameLabel = new JLabel(nameGuest);
        nameLabel.setFont(new Font("Tahoma", Font.PLAIN, 32));
        nameLabel.setBounds(96, 10, 129, 38);
        panel.add(nameLabel);

        // Voice & Video buttons
        addCallButton(panel, 403, "/image/phone48.png", "Voice call",
                e -> System.out.println("voice call"));
        addCallButton(panel, 474, "/image/videocall48.png", "Video call",
                e -> System.out.println("Video call"));
    }

    private void addCallButton(JPanel panel, int x, String iconPath, String tooltip, ActionListener action) {
        JButton btn = new JButton();
        btn.setToolTipText(tooltip);
        btn.setIcon(new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource(iconPath))));
        btn.setBounds(x, 16, 32, 32);
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.addActionListener(action);
        panel.add(btn);
    }

    private void addMessageDisplayPanel() {
        JPanel panel = new JPanel();
        panel.setBounds(0, 66, 562, 323);
        panel.setLayout(null);
        contentPane.add(panel);

        txtDisplayMessage = new JTextPane();
        txtDisplayMessage.setEditable(false);
        txtDisplayMessage.setContentType("text/html");
        txtDisplayMessage.setBackground(new Color(54, 57, 63));
        txtDisplayMessage.setForeground(Color.WHITE);
        txtDisplayMessage.setFont(new Font("Courier New", Font.PLAIN, 18));
        frameChat.appendToPane(txtDisplayMessage, "<div style='background-color:white'></div>");

        JScrollPane scrollPane = new JScrollPane(txtDisplayMessage);
        scrollPane.setBounds(0, 0, 562, 323);
        panel.add(scrollPane);
    }

    private void addEmojiPanel() {
        JPanel panel = new JPanel();
        panel.setBounds(0, 372, 573, 73);
        contentPane.add(panel);
        panel.setLayout(null);

        addEmojiButton(panel, 31, "/image/like32.png", "like32");
        addEmojiButton(panel, 144, "/image/love32.png", "love32");
        addEmojiButton(panel, 265, "/image/smile32.png", "smile32");
        addEmojiButton(panel, 378, "/image/sad32.png", "sad32");
        addEmojiButton(panel, 495, "/image/img2.png", "img2");

        progressBar = new JProgressBar();
        progressBar.setBounds(10, 22, 540, 41);
        progressBar.setVisible(false);
        panel.add(progressBar);
    }

    private void addEmojiButton(JPanel panel, int x, String iconPath, String imageName) {
        JButton btn = new JButton();
        btn.setIcon(new ImageIcon(Objects.requireNonNull(ChatFrame.class.getResource(iconPath))));
        btn.setBounds(x, 22, EMOJI_BUTTON_WIDTH, EMOJI_BUTTON_HEIGHT);
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.addActionListener(e -> sendEmoji(imageName));
        panel.add(btn);
    }

    private void sendEmoji(String imageName) {
        String msg = "<img src='" + ChatFrame.class.getResource("/image/" + imageName + ".png") + "'></img>";
        try {
            chat.sendMessage(sendMessage(msg));
            updateChat_send_Symbol(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMessageInputPanel() {
        JPanel panel = new JPanel();
        panel.setBounds(0, 446, 562, 73);
        contentPane.add(panel);
        panel.setLayout(null);

        // Message input field
        txtMessage = new JTextField();
        txtMessage.setBounds(0, 5, 433, 45);
        txtMessage.setColumns(10);
        txtMessage.setFont(new Font("Courier New", Font.PLAIN, 18));
        txtMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendTextMessage();
                }
            }
        });
        panel.add(txtMessage);

        // Send file button
        btnSendFile = new JButton();
        btnSendFile.setIcon(new ImageIcon(Objects.requireNonNull(
                ChatFrame.class.getResource("/image/file32.png"))));
        btnSendFile.setBounds(440, 10, 64, 53);
        btnSendFile.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnSendFile.setContentAreaFilled(false);
        btnSendFile.addActionListener(e -> selectAndSendFile());
        panel.add(btnSendFile);

        // Send message button
        btnSend = new JButton();
        btnSend.setBorder(new EmptyBorder(0, 0, 0, 0));
        btnSend.setContentAreaFilled(false);
        ImageIcon originalIcon = new ImageIcon(Objects.requireNonNull(
                ChatFrame.class.getResource("/image/send32.png")));
        Image scaledImage = originalIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
        btnSend.setIcon(new ImageIcon(scaledImage));
        btnSend.setBounds(498, 5, 64, 64);
        btnSend.addActionListener(e -> sendTextMessage());
        panel.add(btnSend);
    }

    private void sendTextMessage() {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty())
            return;

        txtMessage.setText("");
        try {
            chat.sendMessage(sendMessage(msg));
            updateChat_send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                chat.sendMessage(Encode.sendFile(selectedFile.getName()));
                chat.sendFile(selectedFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addReceiveLabel() {
        lblReceive = new JLabel("");
        lblReceive.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblReceive.setBounds(47, 529, 465, 29);
        lblReceive.setVisible(false);
        contentPane.add(lblReceive);
    }

    // ============ CHAT ROOM THREAD ============

    public class ChatRoom extends Thread {
        private Socket connect;
        private ObjectOutputStream outPeer;
        private ObjectInputStream inPeer;
        private boolean finishReceive = false;
        private int sizeReceive = 0;
        private String nameFileReceive = "";

        public ChatRoom(Socket connection, String name, String guest) {
            this.connect = connection;
        }

        @Override
        public void run() {
            System.out.println("Chat Room start");

            try {
                // Initialize both streams once at the start
                // Important: Output stream must be created first to write stream header
                outPeer = new ObjectOutputStream(connect.getOutputStream());
                outPeer.flush();

                inPeer = new ObjectInputStream(connect.getInputStream());

                while (!isStop) {
                    try {
                        handleIncomingMessage();
                    } catch (Exception e) {
                        e.printStackTrace();
                        cleanupIncompleteFile();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleIncomingMessage() throws Exception {
            Object obj = inPeer.readObject();

            if (obj instanceof String) {
                String msgObj = obj.toString();
                handleStringMessage(msgObj);
            } else if (obj instanceof DataFile) {
                DataFile data = (DataFile) obj;
                sizeReceive++;
            }
        }

        private void handleStringMessage(String msgObj) throws Exception {
            if (msgObj.startsWith("<SESSION_ACCEPT>")) {
                handleSessionAccept(msgObj);
            } else if (msgObj.equals(Tags.CHAT_CLOSE_TAG)) {
                handleChatClose();
            } else if (Decode.checkFile(msgObj)) {
                handleFileRequest(msgObj);
            } else if (Decode.checkFeedBack(msgObj)) {
                handleFileFeedback();
            } else if (msgObj.equals(Tags.FILE_DATA_BEGIN_TAG)) {
                handleFileDataBegin();
            } else if (msgObj.equals(Tags.FILE_DATA_CLOSE_TAG)) {
                handleFileDataClose();
            } else {
                String message = Decode.getMessage(msgObj);
                if (message != null && !message.isEmpty()) {
                    updateChat_receive(message);
                } else {
                    System.out.println("Failed to decode message: " + msgObj);
                }
            }
        }

        private void handleSessionAccept(String msgObj) throws Exception {
            System.out.println("SESSION_ACCEPT received");
            // Parse peers from message
            String[] peers = msgObj.split("<PEER>");
            for (String p : peers) {
                if (p.contains("<PEER_NAME>") && p.contains("<IP>") && p.contains("<PORT>")) {
                    String name = extractXmlValue(p, "PEER_NAME");
                    String ip = extractXmlValue(p, "IP").replace("/", "").trim();
                    int port = Integer.parseInt(extractXmlValue(p, "PORT"));
                    System.out.println("Peer found: " + name + " @ " + ip + ":" + port);
                }
            }
        }

        private String extractXmlValue(String xml, String tag) {
            return xml.split("<" + tag + ">")[1].split("</" + tag + ">")[0];
        }

        private void handleChatClose() throws Exception {
            isStop = true;
            Tags.show(ChatFrame.this, nameGuest + " This window will close.", false);
            dispose();
            chat.stopChat();
        }

        private void handleFileRequest(String msgObj) throws Exception {
            isStop = true;
            nameFileReceive = msgObj.substring(10, msgObj.length() - 11);
            File fileReceive = new File(URL_DIR + "/" + nameFileReceive);

            if (!fileReceive.exists()) {
                fileReceive.createNewFile();
            }

            String ack = Tags.FILE_REQ_ACK_OPEN_TAG + Integer.toBinaryString(port) +
                    Tags.FILE_REQ_ACK_CLOSE_TAG;
            sendMessage(ack);
        }

        private void handleFileFeedback() throws Exception {
            btnSendFile.setEnabled(false);
            new Thread(() -> {
                try {
                    sendMessage(Tags.FILE_DATA_BEGIN_TAG);
                    updateChat_notify("Sending file: " + nameFileReceive);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void handleFileDataBegin() throws Exception {
            finishReceive = false;
            lblReceive.setVisible(true);
        }

        private void handleFileDataClose() throws Exception {
            updateChat_receive("File received: " + nameFileReceive + " (" + sizeReceive + " KB)");
            sizeReceive = 0;
            lblReceive.setVisible(false);

            new Thread(this::showSaveFileDialog).start();
            finishReceive = true;
        }

        private void cleanupIncompleteFile() {
            File fileTemp = new File(URL_DIR + nameFileReceive);
            if (fileTemp.exists() && !finishReceive) {
                fileTemp.delete();
            }
        }

        public void sendFile(File file) throws Exception {
            btnSendFile.setEnabled(false);
            lblReceive.setVisible(true);
            progressBar.setVisible(true);

            long fileSize = file.length();
            int chunks = (int) ((fileSize + 1023) / 1024); // Ceiling division

            if (chunks > Tags.MAX_MSG_SIZE / 1024) {
                lblReceive.setText("File is too large...");
                sendMessage(Tags.FILE_DATA_CLOSE_TAG);
                btnSendFile.setEnabled(true);
                lblReceive.setVisible(false);
                return;
            }

            progressBar.setValue(0);
            lblReceive.setText("Sending...");

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int chunkCount = 0;

                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    DataFile dataFile = new DataFile(bytesRead);
                    System.arraycopy(buffer, 0, dataFile.data, 0, bytesRead);
                    sendMessage(dataFile);

                    chunkCount++;
                    progressBar.setValue((chunkCount * 100) / chunks);
                }

                sendMessage(Tags.FILE_DATA_CLOSE_TAG);
                updateChat_notify("File sent successfully");
            } finally {
                progressBar.setVisible(false);
                lblReceive.setVisible(false);
                btnSendFile.setEnabled(true);
            }
        }

        private void showSaveFileDialog() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = fileChooser.showSaveDialog(ChatFrame.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File destinationFile = new File(fileChooser.getSelectedFile().getAbsolutePath() +
                        "/" + nameFileReceive);
                if (!destinationFile.exists()) {
                    try {
                        Thread.sleep(1000);
                        frameChat.copyFileReceive(
                                new FileInputStream(URL_DIR + nameFileReceive),
                                new FileOutputStream(destinationFile.getAbsolutePath()),
                                URL_DIR + nameFileReceive);
                    } catch (Exception e) {
                        Tags.show(ChatFrame.this, "Error receiving file!", false);
                    }
                } else {
                    int overwrite = Tags.show(ChatFrame.this, "File exists. Overwrite?", true);
                    if (overwrite != 0)
                        showSaveFileDialog(); // Retry if not confirmed
                }
            }
        }

        public synchronized void sendMessage(Object obj) throws Exception {
            if (outPeer != null) {
                outPeer.writeObject(obj);
                outPeer.flush();
            }
        }

        public void stopChat() {
            try {
                connect.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}