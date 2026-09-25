package com.example.testproject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ReceiverTask extends TransferTask {
    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;

    public ReceiverTask(TransferListener listener) {
        super(listener);
    }

    public void attachListener(TransferListener newListener) {
        this.listener = newListener;
        if (listener != null) {
            listener.onMessage(isRunning ? "Listening for files..." : "Receive mode is securely OFF.");
        }
    }

    @Override
    protected void executeTransfer() throws Exception {
        try {
            serverSocket = new ServerSocket(PORT);
            if (listener != null) listener.onMessage("Listening for files...");

            while (isRunning) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                    int fileCount = dis.readInt();
                    if (listener != null) listener.onProgress(0.0);

                    for (int i = 0; i < fileCount; i++) {
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        final int current = i + 1;

                        if (listener != null) {
                            listener.onMessage("Receiving (" + current + "/" + fileCount + "): " + fileName);
                        }

                        File downloadDir = new File("Downloads");
                        if (!downloadDir.exists()) downloadDir.mkdirs();

                        try (FileOutputStream fos = new FileOutputStream(new File(downloadDir, fileName))) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int read;
                            long totalRead = 0;
                            long lastUpdate = 0;

                            while (totalRead < fileSize) {
                                int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                                read = dis.read(buffer, 0, bytesToRead);
                                if (read == -1) break;

                                fos.write(buffer, 0, read);
                                totalRead += read;

                                long now = System.currentTimeMillis();
                                if (now - lastUpdate > 50 || totalRead == fileSize) {
                                    lastUpdate = now;
                                    double progress = fileSize == 0 ? 1.0 : (double) totalRead / fileSize;
                                    if (listener != null) listener.onProgress(progress);
                                }
                            }
                            fos.flush();
                        }

                        String senderIP = socket.getInetAddress().getHostAddress();
                        HistoryManager.logTransfer("Received", fileName, senderIP);
                    }

                    if (listener != null) listener.onComplete("All files received successfully!");

                } catch (SocketException e) {
                    if (!isRunning) {
                        if (listener != null) listener.onMessage("Receive mode is securely OFF.");
                        break;
                    } else {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) listener.onError("Error during transfer: " + e.getMessage());
                }
            }
        } catch (SocketException e) {
            if (listener != null) listener.onMessage("Receive mode is securely OFF.");
        } catch (IOException e) {
            if (listener != null) listener.onError("Network Error: " + e.getMessage());
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