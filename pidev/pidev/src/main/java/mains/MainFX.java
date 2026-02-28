package mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("1. Bdit nlawej 3al FXML...");

        try {
            URL fxmlUrl = getClass().getResource("/Login.fxml");

            if (fxmlUrl == null) {
                System.out.println("MOCHKLA: Mal9itech 'Login.fxml'! Thabet fi dossier resources.");
                return;
            } else {
                System.out.println("OK! L9it l fichier: " + fxmlUrl);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("2. Chargement FXML OK.");

            Scene scene = new Scene(root);
            primaryStage.setTitle("EchoCare");
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("3. Fenetre t7allet!");

        } catch (IOException e) {
            System.out.println("ERREUR FATALE:");
            e.printStackTrace();
        }
    }
}