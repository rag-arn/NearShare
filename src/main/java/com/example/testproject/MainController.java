//package com.example.testproject;
//
//import javafx.fxml.FXML;
//import javafx.scene.control.Label;
//import javafx.scene.control.TextField;
//import javafx.event.ActionEvent;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//
//public class MainController {
//
//    @FXML private TextField targetIpField;
//    @FXML private Label statusLabel;
//
//    @FXML
//    public void initialize() {
//        // Starts the Receiver thread automatically when the UI loads
//        Thread receiverThread = new Thread(new ReceiverTask(statusLabel));
//        receiverThread.setDaemon(true); // Ensures the thread dies when you close the app
//        receiverThread.start();
//    }
//
//    @FXML
//    public void onSendButtonClicked(javafx.event.ActionEvent event) {
//        String targetIP = targetIpField.getText();
//
//        if (targetIP.isEmpty()) {
//            statusLabel.setText("Please enter an IP address.");
//            return;
//        }
//
//        // Launch the native macOS file picker dialog
//        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
//        fileChooser.setTitle("Select a file to send");
//        java.io.File selectedFile = fileChooser.showOpenDialog(targetIpField.getScene().getWindow());
//
//        // Stop execution gracefully if you close the finder window without picking anything
//        if (selectedFile == null) {
//            statusLabel.setText("Transfer cancelled.");
//            return;
//        }
//
//        // Trigger the background network thread with the actual file you selected
//        Thread senderThread = new Thread(new SenderTask(targetIP, selectedFile, statusLabel));
//        senderThread.start();
//    }
//}

package com.example.testproject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.event.ActionEvent;
import java.io.File;

public class MainController {

    @FXML private TextField targetIpField;
    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;

    private ReceiverTask currentReceiverTask;

    @FXML
    public void initialize() {
        // Automatically start in Receive Mode when the app opens
        receiveToggle.setSelected(true);
        receiveToggle.setText("Receive Mode: ON");
        startReceiver();

        // Listen for when the user clicks the toggle
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

    private void startReceiver() {
        currentReceiverTask = new ReceiverTask(receiveStatusLabel);
        Thread receiverThread = new Thread(currentReceiverTask);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void stopReceiver() {
        if (currentReceiverTask != null) {
            currentReceiverTask.stopListening();
        }
    }

    @FXML
    public void onSendButtonClicked(ActionEvent event) {
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
}