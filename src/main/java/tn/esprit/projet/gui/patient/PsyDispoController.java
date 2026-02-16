package tn.esprit.projet.gui.patient;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import tn.esprit.projet.services.DisponibiliteService;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import tn.esprit.projet.utils.AlertUtils; // Pour utiliser tes nouvelles alertes

public class PsyDispoController {

    @FXML private DatePicker pickerDate;
    @FXML private TextField txtHeure;

    private DisponibiliteService ds = new DisponibiliteService();

    @FXML
    void retourMenu(ActionEvent event) {
        try {
            // Chargement du menu principal situé à la racine de resources
            Parent root = FXMLLoader.load(getClass().getResource("/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("PI-DEV : Menu Principal");
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur retour menu : " + e.getMessage());
        }
    }
    @FXML
    private void handleAjouterDispo() {
        LocalDate localDate = pickerDate.getValue();
        String heureStr = txtHeure.getText();

        if (localDate == null || heureStr.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        try {
            // Conversion vers les types SQL
            Date dateSql = Date.valueOf(localDate);
            Time heureSql = Time.valueOf(heureStr);

            // On simule l'ID du psychologue connecté (ex: ID 1 pour hama)
            ds.ajouterDispo(1, dateSql, heureSql);

            showAlert("Succès", "Disponibilité ajoutée ! Elle est maintenant visible par les patients.");
            txtHeure.clear();
            pickerDate.setValue(null);

        } catch (IllegalArgumentException e) {
            showAlert("Format Heure", "L'heure doit être au format HH:mm:ss");
        } catch (SQLException e) {
            showAlert("Erreur SQL", e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void handleEnregistrerCreneau() {
        try {
            // 1. Déclarer et convertir les données de l'interface
            java.sql.Date sqlDate = java.sql.Date.valueOf(pickerDate.getValue());
            java.sql.Time sqlHeure = java.sql.Time.valueOf(txtHeure.getText());

            // 2. Appeler le service avec les variables déclarées
            // Remplacez '1' par un ID qui existe vraiment dans votre table psychologue
            ds.ajouterDispo(1, sqlDate, sqlHeure);

            AlertUtils.showInfo("Succès", "Créneau ajouté !");
        } catch (SQLException e) {
            AlertUtils.showError("Erreur SQL", e.getMessage());
        } catch (Exception e) {
            AlertUtils.showError("Erreur", "Vérifiez le format de l'heure (HH:mm:ss)");
        }
    }
}