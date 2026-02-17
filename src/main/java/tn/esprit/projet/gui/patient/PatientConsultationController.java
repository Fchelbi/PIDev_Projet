package tn.esprit.projet.gui.patient;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.esprit.projet.entities.Consultation;
import tn.esprit.projet.entities.Psychologue;
import tn.esprit.projet.services.ConsultationService;
import tn.esprit.projet.services.PsychologueService;

import java.sql.SQLException;

public class PatientConsultationController {

    @FXML private TableView<Object> tableConsultations;
    @FXML private TableColumn<Object, String> colId, colPsy, colDate, colStatut;
    @FXML private Label labelTitre;
    @FXML private VBox paneReservation;
    @FXML private DatePicker dateRes;
    @FXML private TextField heureRes;

    private ConsultationService consultationService = new ConsultationService();
    private PsychologueService psychologueService = new PsychologueService();
    private int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        showConsultationsView();
    }

    @FXML
    public void showConsultationsView() {
        labelTitre.setText("Mes consultations");
        paneReservation.setVisible(false);
        colId.setVisible(false); // Cacher la colonne ID technique

        // CHANGEMENT DYNAMIQUE DES TITRES DE COLONNES
        colPsy.setText("Psychologue (Nom)");
        colDate.setText("Date du RDV");
        colStatut.setText("État du dossier");

        // Remplacement de l'ID par le Nom du psy
        colPsy.setCellValueFactory(cellData -> {
            Consultation c = (Consultation) cellData.getValue();
            try {
                Psychologue p = psychologueService.getById(c.getPsychologueId());
                return new SimpleStringProperty(p != null ? p.getNom() : "Inconnu");
            } catch (SQLException e) {
                return new SimpleStringProperty("Erreur DB");
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateConsultation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        loadConsultations();
    }

    @FXML
    public void showPsyView() {
        labelTitre.setText("Liste des Psychologues");
        paneReservation.setVisible(true);
        colId.setVisible(true);

        // CHANGEMENT DYNAMIQUE DES TITRES DE COLONNES
        colId.setText("ID");
        colPsy.setText("Nom du Praticien");
        colDate.setText("Spécialité");
        colStatut.setText("Email de contact");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPsy.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("specialite"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadPsychologues();
    }

    @FXML
    public void confirmBooking() {
        Psychologue selectedPsy = (Psychologue) tableConsultations.getSelectionModel().getSelectedItem();
        if (selectedPsy == null || dateRes.getValue() == null || heureRes.getText().isEmpty()) {
            System.out.println("Erreur: Sélectionnez un psy et remplissez les champs.");
            return;
        }
        String dateComplete = dateRes.getValue().toString() + " " + heureRes.getText();
        try {
            consultationService.reserverConsultation(selectedPsy.getId(), CURRENT_USER_ID, dateComplete);
            showConsultationsView(); // Revenir auto à la liste
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadConsultations() {
        try {
            tableConsultations.setItems(FXCollections.observableArrayList(consultationService.getConsultationsByPatient(CURRENT_USER_ID)));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadPsychologues() {
        try {
            tableConsultations.setItems(FXCollections.observableArrayList(psychologueService.getAll()));
        } catch (SQLException e) { e.printStackTrace(); }
    }
}