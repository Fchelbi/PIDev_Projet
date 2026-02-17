package tn.esprit.projet.gui.patient;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Consultation;
import tn.esprit.projet.services.ConsultationService;
import tn.esprit.projet.utils.AlertUtils;

import java.io.IOException;
import java.sql.SQLException;

public class PsyDispoController {

    @FXML private TableView<Consultation> tableRdv;
    @FXML private TableColumn<Consultation, String> colPatient; // Changé en String pour l'affichage
    @FXML private TableColumn<Consultation, String> colDate, colStatut;

    private ConsultationService consultationService = new ConsultationService();
    private int CURRENT_PSY_ID = 1;

    @FXML
    public void initialize() {
        // Affichage personnalisé pour le patient
        colPatient.setCellValueFactory(cellData ->
                new SimpleStringProperty("Patient #" + cellData.getValue().getUtilisateurId()));

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateConsultation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        refreshTable();
    }

    @FXML
    public void handleAccepter() {
        Consultation selected = tableRdv.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showError("Sélection", "Veuillez sélectionner un rendez-vous à accepter.");
            return;
        }
        try {
            consultationService.updateStatut(selected.getId(), "Confirmé");
            AlertUtils.showInfo("Succès", "Le rendez-vous a été confirmé.");
            refreshTable();
        } catch (SQLException e) {
            AlertUtils.showError("Erreur SQL", e.getMessage());
        }
    }

    @FXML
    public void handleRefuser() {
        Consultation selected = tableRdv.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showError("Sélection", "Veuillez sélectionner un rendez-vous à refuser.");
            return;
        }
        try {
            consultationService.updateStatut(selected.getId(), "À replanifier");
            AlertUtils.showInfo("Statut mis à jour", "Le rendez-vous est marqué 'À replanifier'.");
            refreshTable();
        } catch (SQLException e) {
            AlertUtils.showError("Erreur SQL", e.getMessage());
        }
    }

    private void refreshTable() {
        try {
            tableRdv.setItems(FXCollections.observableArrayList(
                    consultationService.getConsultationsByPsychologue(CURRENT_PSY_ID)
            ));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void retourMenu(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}