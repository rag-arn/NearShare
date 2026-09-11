package com.example.testproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class NearShare extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Parent fxmlRoot = FXMLLoader.load(getClass().getResource("Receive.fxml"));

        // 1. Forcefully lock the FXML base size
        if (fxmlRoot instanceof Region) {
            Region region = (Region) fxmlRoot;
            region.setMinSize(600, 400);
            region.setPrefSize(600, 400);
            region.setMaxSize(600, 400);
        }

        // 2. Put it directly into the StackPane (NO Group)
        StackPane outerWrapper = new StackPane(fxmlRoot);
        // Matches the professional gray background from your CSS!
        outerWrapper.setStyle("-fx-background-color: #F3F4F6;");

        // 3. Default to 900x600
        Scene scene = new Scene(outerWrapper, 900, 600);

        // 4. Uniform Scaling Math
        javafx.beans.binding.DoubleBinding scaleBinding = javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.min(scene.getWidth() / 600.0, scene.getHeight() / 400.0),
                scene.widthProperty(), scene.heightProperty()
        );

        // 5. Scale directly from the center pivot
        fxmlRoot.scaleXProperty().bind(scaleBinding);
        fxmlRoot.scaleYProperty().bind(scaleBinding);

        stage.setTitle("NearShare");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}