package com.example.testproject;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ReceiverTask implements Runnable {
    private Label statusLabel;
    private ProgressBar progressBar;
    private final int PORT = 8080;
    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;

    // Added ProgressBar to the constructor
    public ReceiverTask(Label statusLabel, ProgressBar progressBar) {
        this.statusLabel = statusLabel;
        this.progressBar = progressBar;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            Platform.runLater(() -> {
                statusLabel.setText("Listening for files...");
                progressBar.setVisible(false); // Keep hidden while just listening
            });

            while (isRunning) {
                Socket socket = serverSocket.accept();
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                String fileName = dis.readUTF();
                long fileSize = dis.readLong();

                Platform.runLater(() -> {
                    statusLabel.setText("Receiving: " + fileName);
                    progressBar.setVisible(true); // SHOW the bar when transfer starts
                    progressBar.setProgress(0.0);
                });

                File downloadDir = new File("Downloads");
                if (!downloadDir.exists()) downloadDir.mkdir();

                FileOutputStream fos = new FileOutputStream(new File(downloadDir, fileName));
                byte[] buffer = new byte[4096];
                int read;
                long totalRead = 0;

                while ((read = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                    totalRead += read;

                    double progress = (double) totalRead / fileSize;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }

                fos.close();
                dis.close();
                socket.close();

                Platform.runLater(() -> {
                    statusLabel.setText("Transfer Complete: " + fileName);
                    progressBar.setVisible(false); // HIDE the bar when done
                });
            }
        } catch (java.net.SocketException e) {
            Platform.runLater(() -> statusLabel.setText("Receive mode is securely OFF."));
        } catch (IOException e) {
            Platform.runLater(() -> {
                statusLabel.setText("Network Error: " + e.getMessage());
                progressBar.setVisible(false);
            });
        }
    }

    public void stopListening() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}