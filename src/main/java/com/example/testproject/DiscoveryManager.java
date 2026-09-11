package com.example.testproject;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscoveryManager {
    private static final int DISCOVERY_PORT = 8888;
    private static final String IDENTIFIER = "NEARSHARE_RECEIVER";

    private static Thread broadcastThread;
    private static Thread listenThread;
    private static AtomicBoolean isBroadcasting = new AtomicBoolean(false);
    private static AtomicBoolean isListening = new AtomicBoolean(false);
    private static DatagramSocket listenSocket;

    // Called when toggled to Receive Mode
    public static void startBroadcasting() {
        if (isBroadcasting.get()) return;
        isBroadcasting.set(true);

        broadcastThread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                String hostName = InetAddress.getLocalHost().getHostName(); // Gets your computer's name
                String message = IDENTIFIER + ":" + hostName;
                byte[] data = message.getBytes();
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

                while (isBroadcasting.get()) {
                    DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, DISCOVERY_PORT);
                    socket.send(packet);
                    Thread.sleep(2000); // Shouts its presence every 2 seconds
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    public static void stopBroadcasting() {
        isBroadcasting.set(false);
    }

    // Called when switching to Send Mode
    public static void startListening(ObservableList<String> peerList) {
        if (isListening.get()) return;
        isListening.set(true);

        listenThread = new Thread(() -> {
            try {
                listenSocket = new DatagramSocket(DISCOVERY_PORT);
                byte[] buffer = new byte[1024];

                while (isListening.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    listenSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    String senderIP = packet.getAddress().getHostAddress();

                    if (message.startsWith(IDENTIFIER)) {
                        String deviceName = message.split(":")[1];
                        String displayString = deviceName + " (" + senderIP + ")";

                        // Safely update the JavaFX UI
                        Platform.runLater(() -> {
                            if (!peerList.contains(displayString)) {
                                peerList.add(displayString);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                // Expected when socket is forced closed
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public static void stopListening() {
        isListening.set(false);
        if (listenSocket != null && !listenSocket.isClosed()) {
            listenSocket.close();
        }
    }
}