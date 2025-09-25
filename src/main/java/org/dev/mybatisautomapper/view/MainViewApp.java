package org.dev.mybatisautomapper.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.shape.Path;
import javafx.stage.Stage;

public class MainViewApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/dev/mybatisautomapper/MainView.fxml")
        );

        Parent root = loader.load();

        Scene scene = new Scene(root, 1000, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Mybatis Auto Mapper");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));

//        stage.setResizable(false);

        stage.setScene(scene);

        stage.show();
    }
    public static void main(String[] args) { launch(); }
}