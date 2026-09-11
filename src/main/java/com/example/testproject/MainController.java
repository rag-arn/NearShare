package com.example.testproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainController {

    @FXML private ListView<String> peerListView; // Replaced TextField with ListView
    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;

    private static ReceiverTask currentReceiverTask;
    private ObservableList<String> peers;

    @FXML
    public void initialize() {
        // If we are on the Receive Scene
        if (receiveToggle != null) {
            receiveToggle.setSelected(true);
            receiveToggle.setText("Receive Mode: ON");
            startReceiver();

            receiveToggle.setOnAction(event -> {
                if (receiveToggle.isSelected()) {
                    receiveToggle.setText("Receive Mode: ON");
                    startReceiver();
                } else {
                    receiveToggle.setText("Receive Mode: OFF");
                    stopReceiver();
                }
            });
        }

        // If we are on the Send Scene
        if (peerListView != null) {
            peers = FXCollections.observableArrayList();
            peerListView.setItems(peers);
            DiscoveryManager.startListening(peers); // Start actively scanning for peers
        }
    }

    private void startReceiver() {
        stopReceiver();
        currentReceiverTask = new ReceiverTask(receiveStatusLabel);
        Thread receiverThread = new Thread(currentReceiverTask);
        receiverThread.setDaemon(true);
        receiverThread.start();

        DiscoveryManager.startBroadcasting(); // Start shouting IP to the network
    }

    private void stopReceiver() {
        if (currentReceiverTask != null) {
            currentReceiverTask.stopListening();
            currentReceiverTask = null;
        }
        DiscoveryManager.stopBroadcasting(); // Stop shouting
    }

    @FXML
    public void onSendButtonClicked(ActionEvent event) {
        if (peerListView == null) return;

        String selectedPeer = peerListView.getSelectionModel().getSelectedItem();
        if (selectedPeer == null) {
            sendStatusLabel.setText("Please select a device from the list.");
            return;
        }

        // Extracts just the IP address from "DeviceName (192.168.1.x)"
        String targetIP = selectedPeer.substring(selectedPeer.lastIndexOf("(") + 1, selectedPeer.lastIndexOf(")"));

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select a file to send");
        File selectedFile = fileChooser.showOpenDialog(peerListView.getScene().getWindow());

        if (selectedFile == null) {
            sendStatusLabel.setText("Transfer cancelled.");
            return;
        }

        Thread senderThread = new Thread(new SenderTask(targetIP, selectedFile, sendStatusLabel));
        senderThread.start();
    }

    // --- SCENE SWITCHING METHODS ---

    @FXML
    public void goToSendScene(ActionEvent event) throws IOException {
        stopReceiver();
        Parent root = FXMLLoader.load(getClass().getResource("Send.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void goToReceiveScene(ActionEvent event) throws IOException {
        DiscoveryManager.stopListening(); // Shut down the scanner before leaving Send Mode
        Parent root = FXMLLoader.load(getClass().getResource("Receive.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}