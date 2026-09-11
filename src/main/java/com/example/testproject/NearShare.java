package com.example.testproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.IOException;

public class NearShare extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("Receive.fxml"));
        Scene scene = new Scene(root, 600, 400);

        // Makes the entire UI physically scale up when the window is resized
        Scale scale = new Scale(1, 1);
        scale.xProperty().bind(scene.widthProperty().divide(600));
        scale.yProperty().bind(scene.heightProperty().divide(400));
        root.getTransforms().add(scale);

        stage.setTitle("NearShare");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}