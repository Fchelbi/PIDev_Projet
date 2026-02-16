package tn.esprit.projet.gui.patient;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.projet.entities.Consultation;
import tn.esprit.projet.services.ConsultationService;

import java.sql.SQLException;
import java.util.List;

public class PatientConsultationController {

    @FXML
    private TableView<Consultation> tableConsultations;

    @FXML
    private TableColumn<Consultation, Integer> colId;

    @FXML
    private TableColumn<Consultation, Integer> colPsy;

    @FXML
    private TableColumn<Consultation, String> colDate;

    @FXML
    private TableColumn<Consultation, String> colStatut;

    private ConsultationService consultationService;

    // ⚠️ pour test (plus tard login dynamique)
    private int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        consultationService = new ConsultationService();

        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colPsy.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("psychologueId"));
        colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateConsultation"));
        colStatut.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut"));

        loadConsultations();
    }

    @FXML
    public void handleRefresh() {
        loadConsultations();
    }

    private void loadConsultations() {
        try {
            List<Consultation> list =
                    consultationService.getConsultationsByPatient(CURRENT_USER_ID);

            ObservableList<Consultation> obs =
                    FXCollections.observableArrayList(list);

            tableConsultations.setItems(obs);

        } catch (SQLException e) {
            System.out.println("Erreur chargement consultations: " + e.getMessage());
        }
    }
}
