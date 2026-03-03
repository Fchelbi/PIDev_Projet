package controllers;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class MainMenuController {

    private void switchScene(ActionEvent event, String fxmlFile, String title) {
        try {
            // Chargement du fichier FXML depuis la racine des ressources
            Parent root = FXMLLoader.load(getClass().getResource("/" + fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur de navigation vers " + fxmlFile + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML void openAdmin(ActionEvent event) { switchScene(event, "AdminPsyGestion.fxml", "Gestion des Psychologues"); }
    @FXML void openPsy(ActionEvent event) { switchScene(event, "PsyDispoGestion.fxml", "Espace Psychologue"); }
    @FXML void openPatient(ActionEvent event) { switchScene(event, "PatientConsultation.fxml", "Espace Patient"); }
}