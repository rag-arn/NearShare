package com.example.testproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NearShare extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Updated to load Receive.fxml as the default starting screen
        FXMLLoader fxmlLoader = new FXMLLoader(NearShare.class.getResource("Receive.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("NearShare");
        stage.setScene(scene);
        stage.show();
    }
}