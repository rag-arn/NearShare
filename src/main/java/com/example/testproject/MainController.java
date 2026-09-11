package com.example.testproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private ListView<String> peerListView;
    @FXML private ListView<String> fileListView; // New list for selected files

    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;
    @FXML private ProgressBar sendProgressBar;
    @FXML private ProgressBar receiveProgressBar;

    // Dynamic UI Buttons
    @FXML private Button selectFilesButton;
    @FXML private Button removeFileButton;
    @FXML private Button sendFilesButton;

    private static ReceiverTask currentReceiverTask;
    private ObservableList<String> peers;

    // File tracking
    private List<File> selectedFilesData = new ArrayList<>();
    private ObservableList<String> displayFileNames = FXCollections.observableArrayList();

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

            // Link the UI list to our observable data
            fileListView.setItems(displayFileNames);

            // Disable the remove button if no specific file is clicked in the list
            removeFileButton.setDisable(true);
            fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                removeFileButton.setDisable(newVal == null);
            });
        }
    }

    private void startReceiver() {
        stopReceiver();
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
    public void onSelectFilesButtonClicked(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select files to send");

        // Allows selecting multiple files at once
        List<File> chosenFiles = fileChooser.showOpenMultipleDialog(peerListView.getScene().getWindow());

        if (chosenFiles != null && !chosenFiles.isEmpty()) {
            for (File f : chosenFiles) {
                selectedFilesData.add(f);
                displayFileNames.add(f.getName()); // Only display the clean name
            }
            updateButtonVisibility();
        }
    }

    @FXML
    public void onRemoveFileClicked(ActionEvent event) {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            selectedFilesData.remove(index);
            displayFileNames.remove(index);
            updateButtonVisibility();
        }
    }

    // Controls the swapping of the Select and Send buttons
    private void updateButtonVisibility() {
        boolean hasFiles = !selectedFilesData.isEmpty();
        selectFilesButton.setVisible(!hasFiles);
        sendFilesButton.setVisible(hasFiles);
        removeFileButton.setVisible(hasFiles);
    }

    @FXML
    public void onSendFilesButtonClicked(ActionEvent event) {
        String selectedPeer = peerListView.getSelectionModel().getSelectedItem();
        if (selectedPeer == null) {
            sendStatusLabel.setText("Please select a device from the list.");
            return;
        }

        if (selectedFilesData.isEmpty()) return;

        String targetIP = selectedPeer.substring(selectedPeer.lastIndexOf("(") + 1, selectedPeer.lastIndexOf(")"));

        // Copy the list so we can safely clear the UI immediately
        List<File> filesToTransfer = new ArrayList<>(selectedFilesData);

        Thread senderThread = new Thread(new SenderTask(targetIP, filesToTransfer, sendStatusLabel, sendProgressBar));
        senderThread.start();

        // Clear UI for the next batch
        selectedFilesData.clear();
        displayFileNames.clear();
        updateButtonVisibility();
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