package com.example.testproject;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class DiscoveryManager {

    private static DatagramSocket broadcastSocket;
    private static DatagramSocket listenSocket;
    private static volatile boolean isBroadcasting = false;
    private static volatile boolean isListening = false;

    public static String myDeviceName = "ARNOB's Mac";

    public static void startBroadcasting() {
        if (isBroadcasting) return;
        isBroadcasting = true;

        Thread t = new Thread(() -> {
            try {
                broadcastSocket = new DatagramSocket();
                broadcastSocket.setBroadcast(true);

                while (isBroadcasting) {
                    String message = "NEARSHARE:" + myDeviceName;
                    byte[] buffer = message.getBytes();

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), 8888);
                    broadcastSocket.send(packet);

                    Thread.sleep(1500);
                }
            } catch (Exception e) {
                // Ignore sleep interruptions or socket closures
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void startListening(ObservableList<String> peers) {
        if (isListening) return;
        isListening = true;

        Thread t = new Thread(() -> {
            try {
                listenSocket = new DatagramSocket(8888);
                byte[] buffer = new byte[1024];

                while (isListening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    listenSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.startsWith("NEARSHARE:")) {
                        String senderName = message.substring(10).trim();
                        String ip = packet.getAddress().getHostAddress();

                        String displayString = senderName + " (" + ip + ")";
                        String ipSuffix = "(" + ip + ")";

                        Platform.runLater(() -> {
                            boolean ipFound = false;

                            // Check if this IP is already in the list
                            for (int i = 0; i < peers.size(); i++) {
                                if (peers.get(i).endsWith(ipSuffix)) {
                                    ipFound = true;
                                    // If the name changed, update it in place!
                                    if (!peers.get(i).equals(displayString)) {
                                        peers.set(i, displayString);
                                    }
                                    break;
                                }
                            }

                            if (!ipFound) {
                                peers.add(displayString);
                            }
                        });
                    }
                }
            } catch (SocketException e) {
                // Socket was closed intentionally
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void stopBroadcasting() {
        isBroadcasting = false;
        if (broadcastSocket != null && !broadcastSocket.isClosed()) {
            broadcastSocket.close();
        }
    }

    public static void stopListening() {
        isListening = false;
        if (listenSocket != null && !listenSocket.isClosed()) {
            listenSocket.close();
        }
    }
}