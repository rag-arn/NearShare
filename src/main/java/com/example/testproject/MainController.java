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

    // The new TabPane and its 3 variations of the peer list
    @FXML private TabPane sendModeTabPane;
    @FXML private ListView<String> peerListViewOne;
    @FXML private ListView<String> peerListViewMany;
    @FXML private ListView<String> peerListViewAll;

    @FXML private ListView<String> fileListView;

    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;
    @FXML private ProgressBar sendProgressBar;
    @FXML private ProgressBar receiveProgressBar;

    @FXML private Button selectFilesButton;
    @FXML private Button removeFileButton;
    @FXML private Button sendFilesButton;

    private static ReceiverTask currentReceiverTask;
    private ObservableList<String> peers;

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

        // Initialize the 3 Send Tabs if we are on the Send Scene
        if (peerListViewOne != null) {
            peers = FXCollections.observableArrayList();

            // Bind all 3 lists to the exact same live network data
            peerListViewOne.setItems(peers);
            peerListViewMany.setItems(peers);
            peerListViewAll.setItems(peers);

            // Tab 2: Allow holding CMD/CTRL to select multiple devices
            peerListViewMany.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // Tab 3: Disable clicking entirely (it just targets everyone in the list)
            peerListViewAll.setMouseTransparent(true);
            peerListViewAll.setFocusTraversable(false);

            fileListView.setItems(displayFileNames);
            DiscoveryManager.startListening(peers);

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

        List<File> chosenFiles = fileChooser.showOpenMultipleDialog(sendModeTabPane.getScene().getWindow());

        if (chosenFiles != null && !chosenFiles.isEmpty()) {
            for (File f : chosenFiles) {
                selectedFilesData.add(f);
                displayFileNames.add(f.getName());
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

    private void updateButtonVisibility() {
        boolean hasFiles = !selectedFilesData.isEmpty();
        selectFilesButton.setVisible(!hasFiles);
        sendFilesButton.setVisible(hasFiles);
        removeFileButton.setVisible(hasFiles);
    }

    @FXML
    public void onSendFilesButtonClicked(ActionEvent event) {
        if (selectedFilesData.isEmpty()) return;

        List<String> targetPeers = new ArrayList<>();

        // Determine which targeting logic to use based on the active tab
        int activeTab = sendModeTabPane.getSelectionModel().getSelectedIndex();

        if (activeTab == 0) {
            // One to One
            String selected = peerListViewOne.getSelectionModel().getSelectedItem();
            if (selected != null) targetPeers.add(selected);
        } else if (activeTab == 1) {
            // One to Many
            targetPeers.addAll(peerListViewMany.getSelectionModel().getSelectedItems());
        } else if (activeTab == 2) {
            // One to All Devices
            targetPeers.addAll(peers);
        }

        if (targetPeers.isEmpty()) {
            sendStatusLabel.setText("No target devices found or selected.");
            return;
        }

        List<File> filesToTransfer = new ArrayList<>(selectedFilesData);

        // Spawn a parallel background thread for EVERY target device
        for (String peer : targetPeers) {
            String targetIP = peer.substring(peer.lastIndexOf("(") + 1, peer.lastIndexOf(")"));
            Thread senderThread = new Thread(new SenderTask(targetIP, filesToTransfer, sendStatusLabel, sendProgressBar));
            senderThread.start(); // Operating system handles them simultaneously!
        }

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