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
import javafx.scene.control.ProgressBar;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainController {

    @FXML private ListView<String> peerListView;
    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;

    // The new Progress Bar variables
    @FXML private ProgressBar sendProgressBar;
    @FXML private ProgressBar receiveProgressBar;

    private static ReceiverTask currentReceiverTask;
    private ObservableList<String> peers;

    @FXML
    public void initialize() {
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

        if (peerListView != null) {
            peers = FXCollections.observableArrayList();
            peerListView.setItems(peers);
            DiscoveryManager.startListening(peers);
        }
    }

    private void startReceiver() {
        stopReceiver();
        // Pass the receiveProgressBar to the task
        currentReceiverTask = new ReceiverTask(receiveStatusLabel, receiveProgressBar);
        Thread receiverThread = new Thread(currentReceiverTask);
        receiverThread.setDaemon(true);
        receiverThread.start();

        DiscoveryManager.startBroadcasting();
    }

    private void stopReceiver() {
        if (currentReceiverTask != null) {
            currentReceiverTask.stopListening();
            currentReceiverTask = null;
        }
        DiscoveryManager.stopBroadcasting();
    }

    @FXML
    public void onSendButtonClicked(ActionEvent event) {
        if (peerListView == null) return;

        String selectedPeer = peerListView.getSelectionModel().getSelectedItem();
        if (selectedPeer == null) {
            sendStatusLabel.setText("Please select a device from the list.");
            return;
        }

        String targetIP = selectedPeer.substring(selectedPeer.lastIndexOf("(") + 1, selectedPeer.lastIndexOf(")"));

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select a file to send");
        File selectedFile = fileChooser.showOpenDialog(peerListView.getScene().getWindow());

        if (selectedFile == null) {
            sendStatusLabel.setText("Transfer cancelled.");
            return;
        }

        // Pass the sendProgressBar to the task
        Thread senderThread = new Thread(new SenderTask(targetIP, selectedFile, sendStatusLabel, sendProgressBar));
        senderThread.start();
    }

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
        DiscoveryManager.stopListening();
        Parent root = FXMLLoader.load(getClass().getResource("Receive.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}