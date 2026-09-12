package com.example.testproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class MainController {

    @FXML private TabPane sendModeTabPane;
    @FXML private ListView<String> peerListViewOne;
    @FXML private ListView<String> peerListViewMany;
    @FXML private ListView<String> peerListViewAll;
    @FXML private ListView<String> fileListView;

    @FXML private TableView<HistoryRecord> historyTableView;
    @FXML private TableColumn<HistoryRecord, Integer> colSerial;
    @FXML private TableColumn<HistoryRecord, String> colFileName;
    @FXML private TableColumn<HistoryRecord, String> colStatus;
    @FXML private TableColumn<HistoryRecord, String> colDate;
    @FXML private TableColumn<HistoryRecord, String> colTime;

    @FXML private Label sendStatusLabel;
    @FXML private Label receiveStatusLabel;
    @FXML private ToggleButton receiveToggle;
    @FXML private ProgressBar sendProgressBar;
    @FXML private ProgressBar receiveProgressBar;

    // The new animated GIF node
    @FXML private ImageView loadingAnimation;

    @FXML private Button selectFilesButton;
    @FXML private Button removeFileButton;
    @FXML private Button sendFilesButton;
    @FXML private Button profileButton;
    @FXML private Button historyButton;

    private static ReceiverTask currentReceiverTask;
    private ObservableList<String> peers;

    private List<File> selectedFilesData = new ArrayList<>();
    private ObservableList<String> displayFileNames = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Preferences prefs = Preferences.userNodeForPackage(MainController.class);
        DiscoveryManager.myDeviceName = prefs.get("deviceName", "Ragib's MacBook");

        if (historyTableView != null) {
            colSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            colFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
            historyTableView.getItems().addAll(HistoryManager.getHistory());
        }

        if (receiveToggle != null) {
            receiveToggle.setSelected(true);
            receiveToggle.setText("Receiver Mode");
            startReceiver();

            // Bind the GIF visibility directly to the toggle and progress bar states
            if (loadingAnimation != null && receiveProgressBar != null) {
                // Ensure the layout collapses and expands seamlessly without leaving empty gaps
                loadingAnimation.managedProperty().bind(loadingAnimation.visibleProperty());

                // Visible ONLY if toggle is on AND progress bar is hidden
                loadingAnimation.visibleProperty().bind(
                        receiveToggle.selectedProperty().and(receiveProgressBar.visibleProperty().not())
                );
            }

            receiveToggle.setOnAction(event -> {
                if (receiveToggle.isSelected()) {
                    receiveToggle.setText("Receiver Mode");
                    startReceiver();
                } else {
                    receiveToggle.setText("Turn on Receiver Mode");
                    stopReceiver();
                }
            });
        }

        if (peerListViewOne != null) {
            peers = FXCollections.observableArrayList();
            peerListViewOne.setItems(peers);
            peerListViewMany.setItems(peers);
            peerListViewAll.setItems(peers);

            peerListViewMany.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            peerListViewAll.setMouseTransparent(true);
            peerListViewAll.setFocusTraversable(false);

            fileListView.setItems(displayFileNames);
            DiscoveryManager.startListening(peers);

            selectFilesButton.managedProperty().bind(selectFilesButton.visibleProperty());
            removeFileButton.managedProperty().bind(removeFileButton.visibleProperty());
            sendFilesButton.managedProperty().bind(sendFilesButton.visibleProperty());

            removeFileButton.setDisable(true);
            fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                removeFileButton.setDisable(newVal == null);
            });
        }
    }

    @FXML
    public void onClearHistoryClicked(ActionEvent event) {
        HistoryManager.clearHistory();
        if (historyTableView != null) {
            historyTableView.getItems().clear();
        }
    }

    @FXML
    public void onProfileButtonClicked(ActionEvent event) {
        Preferences prefs = Preferences.userNodeForPackage(MainController.class);
        String currentName = prefs.get("deviceName", "Ragib's MacBook");

        Dialog<Void> profileBanner = new Dialog<>();
        profileBanner.setTitle("Profile");
        profileBanner.setHeaderText("Device Information");
        profileBanner.getDialogPane().setPrefSize(400, 266);

        profileBanner.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        VBox vbox = new VBox(20);
        vbox.setStyle("-fx-padding: 20px;");
        vbox.setAlignment(Pos.CENTER);

        Label nameLabel = new Label("Device Name : " + currentName);
        nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 22px;");

        Label ipLabel = new Label("IP Address : " + getLocalIpAddress());
        ipLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button changeNameBtn = new Button("Change Device Name");
        changeNameBtn.getStyleClass().add("secondary-pill-button");
        changeNameBtn.setStyle("-fx-padding: 6px 16px; -fx-font-size: 13px;");

        vbox.getChildren().addAll(nameLabel, ipLabel, changeNameBtn);
        profileBanner.getDialogPane().setContent(vbox);
        profileBanner.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        changeNameBtn.setOnAction(e -> {
            TextInputDialog renameDialog = new TextInputDialog(prefs.get("deviceName", "Ragib's MacBook"));
            renameDialog.setTitle("Change Device Name");
            renameDialog.setHeaderText("Enter your new device name :");

            renameDialog.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

            Button renameButton = (Button) renameDialog.getDialogPane().lookupButton(ButtonType.OK);
            if (renameButton != null) {
                renameButton.setText("Rename");
            }

            Optional<String> result = renameDialog.showAndWait();

            result.ifPresent(newName -> {
                String trimmedName = newName.trim();
                if (!trimmedName.isEmpty() && !trimmedName.equals(prefs.get("deviceName", ""))) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Change");
                    confirmAlert.setHeaderText(null);
                    confirmAlert.setContentText("Are you sure you want to change your device name to '" + trimmedName + "'?");

                    confirmAlert.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

                    Optional<ButtonType> confirmResult = confirmAlert.showAndWait();
                    if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                        DiscoveryManager.myDeviceName = trimmedName;
                        prefs.put("deviceName", trimmedName);
                        nameLabel.setText("Device Name : " + trimmedName);
                    }
                }
            });
        });

        profileBanner.showAndWait();
    }

    private String getLocalIpAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ex) {
                return "Unknown IP";
            }
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

        List<File> chosenFiles = fileChooser.showOpenMultipleDialog(
                (sendModeTabPane != null) ? sendModeTabPane.getScene().getWindow() : null
        );

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
        int activeTab = sendModeTabPane.getSelectionModel().getSelectedIndex();

        if (activeTab == 0) {
            String selected = peerListViewOne.getSelectionModel().getSelectedItem();
            if (selected != null) targetPeers.add(selected);
        } else if (activeTab == 1) {
            targetPeers.addAll(peerListViewMany.getSelectionModel().getSelectedItems());
        } else if (activeTab == 2) {
            targetPeers.addAll(peers);
        }

        if (targetPeers.isEmpty()) {
            sendStatusLabel.setText("No target devices found or selected.");
            return;
        }

        List<File> filesToTransfer = new ArrayList<>(selectedFilesData);

        for (String peer : targetPeers) {
            String targetIP = peer.substring(peer.lastIndexOf("(") + 1, peer.lastIndexOf(")"));
            for (File f : filesToTransfer) {
                HistoryManager.logTransfer("Sent", f.getName(), peer);
            }
            Thread senderThread = new Thread(new SenderTask(targetIP, filesToTransfer, sendStatusLabel, sendProgressBar));
            senderThread.start();
        }

        selectedFilesData.clear();
        displayFileNames.clear();
        updateButtonVisibility();
    }

    private void applyPerfectScalingAndSwitch(ActionEvent event, String fxmlFile) throws IOException {
        Parent fxmlRoot = FXMLLoader.load(getClass().getResource(fxmlFile));

        if (fxmlRoot instanceof Region) {
            Region region = (Region) fxmlRoot;
            region.setMinSize(1280, 720);
            region.setPrefSize(1280, 720);
            region.setMaxSize(1280, 720);
        }

        StackPane outerWrapper = new StackPane(fxmlRoot);
        outerWrapper.setStyle("-fx-background-color: #050102;");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        double targetWidth = Math.max(stage.getWidth(), 1280.0);
        double targetHeight = Math.max(stage.getHeight(), 720.0);

        Scene scene = new Scene(outerWrapper, targetWidth, targetHeight);

        javafx.beans.binding.DoubleBinding scaleBinding = javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.min(scene.getWidth() / 1280.0, scene.getHeight() / 720.0),
                scene.widthProperty(), scene.heightProperty()
        );

        fxmlRoot.scaleXProperty().bind(scaleBinding);
        fxmlRoot.scaleYProperty().bind(scaleBinding);

        stage.setScene(scene);
        stage.setWidth(targetWidth);
        stage.setHeight(targetHeight);
        stage.show();
    }

    @FXML
    public void goToSendScene(ActionEvent event) throws IOException {
        stopReceiver();
        applyPerfectScalingAndSwitch(event, "Send.fxml");
    }

    @FXML
    public void goToReceiveScene(ActionEvent event) throws IOException {
        DiscoveryManager.stopListening();
        applyPerfectScalingAndSwitch(event, "Receive.fxml");
    }

    @FXML
    public void onHistoryButtonClicked(ActionEvent event) throws IOException {
        stopReceiver();
        applyPerfectScalingAndSwitch(event, "History.fxml");
    }
}