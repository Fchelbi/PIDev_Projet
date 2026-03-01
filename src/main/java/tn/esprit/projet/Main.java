package tn.esprit.projet;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Chargement du menu principal
        Parent root = FXMLLoader.load(getClass().getResource("/MainMenu.fxml"));
        primaryStage.setTitle("PI-DEV : Menu Principal");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}