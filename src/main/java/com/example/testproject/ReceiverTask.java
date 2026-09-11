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
                progressBar.setVisible(false);
            });

            while (isRunning) {
                Socket socket = serverSocket.accept();
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                int fileCount = dis.readInt(); // Read how many files are incoming

                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    progressBar.setProgress(0.0);
                });

                for (int i = 0; i < fileCount; i++) {
                    String fileName = dis.readUTF();
                    long fileSize = dis.readLong();
                    final int current = i + 1;

                    Platform.runLater(() -> statusLabel.setText("Receiving (" + current + "/" + fileCount + "): " + fileName));

                    File downloadDir = new File("Downloads");
                    if (!downloadDir.exists()) downloadDir.mkdir();

                    FileOutputStream fos = new FileOutputStream(new File(downloadDir, fileName));
                    byte[] buffer = new byte[4096];
                    int read;
                    long totalRead = 0;

                    // Strictly bound the reading to fileSize so files don't bleed into each other
                    while (totalRead < fileSize) {
                        int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                        read = dis.read(buffer, 0, bytesToRead);
                        if (read == -1) break;

                        fos.write(buffer, 0, read);
                        totalRead += read;

                        double progress = (double) totalRead / fileSize;
                        Platform.runLater(() -> progressBar.setProgress(progress));
                    }
                    fos.close();
                }
                dis.close();
                socket.close();

                Platform.runLater(() -> {
                    statusLabel.setText("All files received successfully!");
                    progressBar.setVisible(false);
                });
            }
        } catch (SocketException e) {
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