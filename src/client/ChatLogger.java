package client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ChatLogger {

    private BufferedWriter writer;

    public ChatLogger(String user, String guest) throws IOException {
        String fileName = "chat_" + user + "_" + guest + ".txt";
        writer = new BufferedWriter(new FileWriter(fileName, true)); // append = true
    }

    public synchronized void log(String sender, String message) {
        try {
            writer.write("[" + sender + "]: " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
