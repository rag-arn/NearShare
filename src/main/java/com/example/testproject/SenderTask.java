package com.example.testproject;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class SenderTask implements Runnable {
    private String targetIP;
    private List<File> filesToSend;
    private Label statusLabel;
    private ProgressBar progressBar;
    private final int PORT = 8080;

    public SenderTask(String targetIP, List<File> filesToSend, Label statusLabel, ProgressBar progressBar) {
        this.targetIP = targetIP;
        this.filesToSend = filesToSend;
        this.statusLabel = statusLabel;
        this.progressBar = progressBar;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(targetIP, PORT)) {
            Platform.runLater(() -> {
                progressBar.setVisible(true);
                progressBar.setProgress(0.0);
            });

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(filesToSend.size()); // Send the count first

            for (int i = 0; i < filesToSend.size(); i++) {
                File file = filesToSend.get(i);
                final int current = i + 1;

                Platform.runLater(() -> statusLabel.setText("Sending (" + current + "/" + filesToSend.size() + "): " + file.getName()));

                FileInputStream fis = new FileInputStream(file);
                long fileSize = file.length();
                dos.writeUTF(file.getName());
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
            }
            dos.close();

            Platform.runLater(() -> {
                statusLabel.setText("All files sent successfully!");
                progressBar.setVisible(false);
            });
        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Failed to connect to " + targetIP);
                progressBar.setVisible(false);
            });
        }
    }
}