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
        // --- CHOOSE YOUR STARTING PAGE HERE ---
        // Change to "/AdminDashboard.fxml" if you want to start there instead
        String fxmlFile = "/Login.fxml";

        System.out.println("1️⃣. Searching for FXML: " + fxmlFile);

        try {
            URL fxmlUrl = getClass().getResource("/Login.fxml");
            // Find the file
            URL fxmlUrl = getClass().getResource(fxmlFile);

            // Check if it exists
            if (fxmlUrl == null) {
                System.out.println("MOCHKLA: Mal9itech 'Login.fxml'! Thabet fi dossier resources.");
                System.out.println("❌ ERROR: Could not find '" + fxmlFile + "'! Check your resources folder.");
                return;
            } else {
                System.out.println("OK! L9it l fichier: " + fxmlUrl);
                System.out.println("✅ Found it! Path: " + fxmlUrl);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("2. Chargement FXML OK.");
            System.out.println("2️⃣. FXML loaded successfully.");

            // Create scene with a default size (adjust if needed)
            Scene scene = new Scene(root);
            primaryStage.setTitle("EchoCare");

            // Try to load CSS if it exists
            try {
                URL cssUrl = getClass().getResource("/style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("🎨 Stylesheet applied.");
                }
            } catch (Exception e) {
                System.out.println("⚠️ Note: No style.css applied.");
            }

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