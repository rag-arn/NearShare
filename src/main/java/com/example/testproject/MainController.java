package com.example.testproject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainController {

    @FXML private TextField targetIpField;
    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;

    // Made static so it persists safely across scene changes
    private static ReceiverTask currentReceiverTask;

    @FXML
    public void initialize() {
        // We check if receiveToggle is not null, meaning we are currently on the Receive Scene
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
    }

    private void startReceiver() {
        stopReceiver(); // Always ensure any old thread is dead before starting a new one
        currentReceiverTask = new ReceiverTask(receiveStatusLabel);
        Thread receiverThread = new Thread(currentReceiverTask);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void stopReceiver() {
        if (currentReceiverTask != null) {
            currentReceiverTask.stopListening();
            currentReceiverTask = null;
        }
    }

    @FXML
    public void onSendButtonClicked(ActionEvent event) {
        if (targetIpField == null) return;

        String targetIP = targetIpField.getText();
        if (targetIP.isEmpty()) {
            sendStatusLabel.setText("Please enter an IP address.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select a file to send");
        File selectedFile = fileChooser.showOpenDialog(targetIpField.getScene().getWindow());

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
        stopReceiver(); // CRITICAL: Frees up Port 8080 before leaving the scene
        Parent root = FXMLLoader.load(getClass().getResource("Send.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @FXML
    public void goToReceiveScene(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("Receive.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}