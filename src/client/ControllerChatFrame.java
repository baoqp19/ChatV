package client;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ControllerChatFrame {

    public void copyFileReceive(InputStream inputStr, OutputStream outputStr, String path) throws IOException {
        byte[] buffer = new byte[1024];
        int lenght;
        while ((lenght = inputStr.read(buffer)) > 0) {
            outputStr.write(buffer, 0, lenght);
        }
        inputStr.close();
        outputStr.close();
        File fileTemp = new File(path);
        if (fileTemp.exists()) {
            fileTemp.delete();
        }
    }

    public void appendToPane(JTextPane tp, String msg) {
        HTMLDocument doc = (HTMLDocument) tp.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) tp.getEditorKit();
        try {

            editorKit.insertHTML(doc, doc.getLength(), msg, 0, 0, null);
            tp.setCaretPosition(doc.getLength());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
