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

        if (fxmlRoot instanceof Region) {
            Region region = (Region) fxmlRoot;
            region.setMinSize(1280, 720);
            region.setPrefSize(1280, 720);
            region.setMaxSize(1280, 720);
        }

        StackPane outerWrapper = new StackPane(fxmlRoot);
        outerWrapper.setStyle("-fx-background-color: #050102;");

        // 16:9 Default Resolution
        Scene scene = new Scene(outerWrapper, 1280, 720);

        javafx.beans.binding.DoubleBinding scaleBinding = javafx.beans.binding.Bindings.createDoubleBinding(
                () -> Math.min(scene.getWidth() / 1280.0, scene.getHeight() / 720.0),
                scene.widthProperty(), scene.heightProperty()
        );

        fxmlRoot.scaleXProperty().bind(scaleBinding);
        fxmlRoot.scaleYProperty().bind(scaleBinding);

        stage.setTitle("NearShare");
        stage.setScene(scene);

        stage.setMinWidth(1280);
        stage.setMinHeight(720);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}