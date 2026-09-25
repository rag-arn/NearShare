package com.example.testproject;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class SenderTask extends TransferTask {
    private String targetIP;
    private List<File> filesToSend;

    public SenderTask(String targetIP, List<File> filesToSend, TransferListener listener) {
        super(listener);
        this.targetIP = targetIP;
        this.filesToSend = filesToSend;
    }

    @Override
    protected void executeTransfer() throws Exception {
        if (listener != null) {
            listener.onMessage("Connecting to " + targetIP + "...");
            listener.onProgress(-1.0); // Indeterminate progress
        }

        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(targetIP, PORT), 5000);

            if (listener != null) listener.onProgress(0.0);

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(filesToSend.size());

            for (int i = 0; i < filesToSend.size(); i++) {
                File file = filesToSend.get(i);
                final int current = i + 1;

                if (listener != null) {
                    listener.onMessage("Sending (" + current + "/" + filesToSend.size() + "): " + file.getName());
                }

                FileInputStream fis = new FileInputStream(file);
                long fileSize = file.length();
                dos.writeUTF(file.getName());
                dos.writeLong(fileSize);

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                long totalSent = 0;
                long lastUpdate = 0;

                while ((read = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, read);
                    totalSent += read;

                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 50 || totalSent == fileSize) {
                        lastUpdate = now;
                        double progress = fileSize == 0 ? 1.0 : (double) totalSent / fileSize;
                        if (listener != null) listener.onProgress(progress);
                    }
                }
                fis.close();
            }
            dos.flush();
            dos.close();

            if (listener != null) listener.onComplete("All files sent successfully!");

        } catch (IOException e) {
            if (listener != null) listener.onError("Failed to connect to " + targetIP);
        }
    }
}