package com.example.testproject;

import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryManager {

    private static DatagramSocket broadcastSocket;
    private static DatagramSocket listenSocket;
    private static volatile boolean isBroadcasting = false;
    private static volatile boolean isListening = false;

    public static String getDefaultDeviceName() {
        try {
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            if (hostName != null && !hostName.trim().isEmpty()) {
                if (hostName.endsWith(".local")) {
                    hostName = hostName.substring(0, hostName.length() - 6);
                }
                return hostName;
            }
        } catch (Exception e) {
            // Ignored, fallback below
        }
        return System.getProperty("user.name") + "'s Device";
    }

    public static String myDeviceName = getDefaultDeviceName();


    private static final String INSTANCE_ID = java.util.UUID.randomUUID().toString();


    private static ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    public static void startBroadcasting() {
        if (isBroadcasting) return;
        isBroadcasting = true;

        Thread t = new Thread(() -> {
            try {
                broadcastSocket = new DatagramSocket();
                broadcastSocket.setBroadcast(true);

                while (isBroadcasting) {
                    String message = "NEARSHARE:" + INSTANCE_ID + "|" + myDeviceName;
                    byte[] buffer = message.getBytes();


                    try {
                        java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                        while (interfaces.hasMoreElements()) {
                            java.net.NetworkInterface networkInterface = interfaces.nextElement();
                            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                                continue;
                            }
                            for (java.net.InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                                InetAddress broadcast = interfaceAddress.getBroadcast();
                                if (broadcast != null) {
                                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, 8888);
                                    broadcastSocket.send(packet);
                                }
                            }
                        }
                    } catch (Exception e) {

                    }


                    try {
                        DatagramPacket fallbackPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("255.255.255.255"), 8888);
                        broadcastSocket.send(fallbackPacket);
                    } catch (Exception e) {

                    }

                    Thread.sleep(1500);
                }
            } catch (Exception e) {

            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void startListening(ObservableList<String> peers) {
        if (isListening) return;
        isListening = true;
        lastSeen.clear();


        Thread receiverThread = new Thread(() -> {
            try {
                listenSocket = new DatagramSocket(null);
                listenSocket.setReuseAddress(true);
                listenSocket.bind(new java.net.InetSocketAddress(8888));
                byte[] buffer = new byte[1024];

                while (isListening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    listenSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.startsWith("NEARSHARE:")) {
                        String payload = message.substring(10);
                        int sep = payload.indexOf('|');
                        if (sep < 0) continue;

                        String senderInstanceId = payload.substring(0, sep);
                        if (senderInstanceId.equals(INSTANCE_ID)) {

                            continue;
                        }

                        String senderName = payload.substring(sep + 1).trim();
                        String ip = packet.getAddress().getHostAddress();

                        String displayString = senderName + " (" + ip + ")";
                        String ipSuffix = "(" + ip + ")";


                        lastSeen.put(displayString, System.currentTimeMillis());

                        Platform.runLater(() -> {
                            boolean ipFound = false;

                            for (int i = 0; i < peers.size(); i++) {
                                if (peers.get(i).endsWith(ipSuffix)) {
                                    ipFound = true;
                                    if (!peers.get(i).equals(displayString)) {

                                        lastSeen.remove(peers.get(i));
                                        peers.set(i, displayString);
                                        lastSeen.put(displayString, System.currentTimeMillis());
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();


        Thread cleanupThread = new Thread(() -> {
            while (isListening) {
                try {
                    Thread.sleep(2000);
                    long now = System.currentTimeMillis();

                    Platform.runLater(() -> {
                        List<String> toRemove = new ArrayList<>();
                        for (String peer : peers) {
                            Long lastTime = lastSeen.get(peer);

                            if (lastTime == null || (now - lastTime > 4500)) {
                                toRemove.add(peer);
                            }
                        }

                        peers.removeAll(toRemove);
                        for (String deadPeer : toRemove) {
                            lastSeen.remove(deadPeer);
                        }
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
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