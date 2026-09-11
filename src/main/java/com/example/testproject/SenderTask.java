package com.example.testproject;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.io.*;
import java.net.Socket;

public class SenderTask implements Runnable {
    private String targetIP;
    private File fileToSend;
    private Label statusLabel;
    private ProgressBar progressBar;
    private final int PORT = 8080;

    // Added ProgressBar to constructor
    public SenderTask(String targetIP, File fileToSend, Label statusLabel, ProgressBar progressBar) {
        this.targetIP = targetIP;
        this.fileToSend = fileToSend;
        this.statusLabel = statusLabel;
        this.progressBar = progressBar;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(targetIP, PORT)) {
            Platform.runLater(() -> {
                // Updated this line to say "Sending to..."
                statusLabel.setText("Sending to " + targetIP + "...");
                progressBar.setVisible(true); // SHOW the bar
                progressBar.setProgress(0.0);
            });

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(fileToSend);

            long fileSize = fileToSend.length();
            dos.writeUTF(fileToSend.getName());
            dos.writeLong(fileSize);

            byte[] buffer = new byte[4096];
            int read;
            long totalSent = 0;

            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
                totalSent += read;

                double progress = (double) totalSent / fileSize;
                Platform.runLater(() -> progressBar.setProgress(progress));
            }

            fis.close();
            dos.close();

            Platform.runLater(() -> {
                statusLabel.setText("Sent successfully!");
                progressBar.setVisible(false); // HIDE the bar when done
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Failed to connect to " + targetIP);
                progressBar.setVisible(false); // HIDE the bar on error
            });
        }
    }
}