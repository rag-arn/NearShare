package com.example.testproject;

import javafx.application.Platform;
import javafx.scene.control.Label;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiverTask implements Runnable {
    private Label statusLabel;
    private final int PORT = 8080;

    public ReceiverTask(Label statusLabel) {
        this.statusLabel = statusLabel;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Platform.runLater(() -> statusLabel.setText("Listening for files..."));

            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                String fileName = dis.readUTF();
                long fileSize = dis.readLong();

                Platform.runLater(() -> statusLabel.setText("Receiving: " + fileName));

                // Creates a 'Downloads' folder in your project directory if it doesn't exist
                File downloadDir = new File("Downloads");
                if (!downloadDir.exists()) downloadDir.mkdir();

                FileOutputStream fos = new FileOutputStream(new File(downloadDir, fileName));
                byte[] buffer = new byte[4096];
                int read;

                while ((read = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }

                fos.close();
                dis.close();
                socket.close();

                Platform.runLater(() -> statusLabel.setText("Transfer Complete: " + fileName));
            }
        } catch (IOException e) {
            Platform.runLater(() -> statusLabel.setText("Network Error: " + e.getMessage()));
        }
    }
}