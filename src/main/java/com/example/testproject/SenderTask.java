package com.example.testproject;

import javafx.application.Platform;
import javafx.scene.control.Label;
import java.io.*;
import java.net.Socket;

public class SenderTask implements Runnable {
    private String targetIP;
    private File fileToSend;
    private Label statusLabel;
    private final int PORT = 8080;

    public SenderTask(String targetIP, File fileToSend, Label statusLabel) {
        this.targetIP = targetIP;
        this.fileToSend = fileToSend;
        this.statusLabel = statusLabel;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(targetIP, PORT)) {
            Platform.runLater(() -> statusLabel.setText("Connecting to " + targetIP + "..."));

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(fileToSend);

            // Send metadata first
            dos.writeUTF(fileToSend.getName());
            dos.writeLong(fileToSend.length());

            // Send actual file bytes
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }

            fis.close();
            dos.close();

            Platform.runLater(() -> statusLabel.setText("Sent successfully!"));
        } catch (IOException e) {
            Platform.runLater(() -> statusLabel.setText("Failed to connect to " + targetIP));
        }
    }
}