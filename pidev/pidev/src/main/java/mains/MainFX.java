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
        System.out.println("1️⃣. Bdit nlawej 3al FXML...");

        try {
            // Nlawej 3al fichier
            URL fxmlUrl = getClass().getResource("/Login.fxml");

            // Nchouf l9inah walla lé
            if (fxmlUrl == null) {
                System.out.println("❌ MOCHKLA: Mal9itech 'Login.fxml' ! Thabet fi dossier resources.");
                // Nwa9fou l khedma houni
                return;
            } else {
                System.out.println("✅ C Bon! L9it l fichier houni: " + fxmlUrl);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("2️⃣. Chargement FXML mcha mriguel.");

            Scene scene = new Scene(root);
            primaryStage.setTitle("Test Final");
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("3️⃣. Fenetre t7allet !");

        } catch (IOException e) {
            System.out.println("❌ ERREUR FATALE:");
            e.printStackTrace();
        }
    }
}