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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Consultation;
import tn.esprit.projet.entities.Psychologue;
import tn.esprit.projet.services.ConsultationService;
import tn.esprit.projet.services.PsychologueService;
import tn.esprit.projet.utils.AlertUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;


public class PatientConsultationController {

    @FXML private TableView<Object> tableConsultations;
    @FXML private TableColumn<Object, String> colId, colPsy, colDate, colStatut;
    @FXML private Label labelTitre;
    @FXML private VBox paneReservation;
    @FXML private DatePicker dateRes;
    @FXML private TextField heureRes;

    private final ConsultationService consultationService = new ConsultationService();
    private final PsychologueService psychologueService = new PsychologueService();
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        showConsultationsView();
    }

    @FXML
    public void showConsultationsView() {
        labelTitre.setText("Mes consultations");
        paneReservation.setVisible(false);
        colId.setVisible(false);

        colPsy.setText("Psychologue (Nom)");
        colDate.setText("Date du RDV");
        colStatut.setText("État du dossier");

        colPsy.setCellValueFactory(cellData -> {
            Consultation c = (Consultation) cellData.getValue();
            try {
                Psychologue p = psychologueService.getById(c.getPsychologueId());
                return new SimpleStringProperty(p != null ? p.getNom() + " " + p.getPrenom() : "Inconnu");
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

        // 1. On cache définitivement la colonne ID pour le patient
        colId.setVisible(false);

        // 2. Configuration des titres de colonnes restants
        colPsy.setText("Psychologue");
        colDate.setText("Spécialité");
        colStatut.setText("Contact Email");

        // 3. Liaison des données (On garde Nom + Prénom pour la clarté)
        colPsy.setCellValueFactory(cellData -> {
            Psychologue p = (Psychologue) cellData.getValue();
            return new SimpleStringProperty(p.getNom() + " " + p.getPrenom());
        });

        // colDate affiche ici la spécialité (vu que tu réutilises la même colonne pour deux vues)
        colDate.setCellValueFactory(new PropertyValueFactory<>("specialite"));

        // colStatut affiche ici l'email
        colStatut.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadPsychologues();
    }

    @FXML
    public void confirmBooking() {
        // --- DÉBUT CONTRÔLE DE SAISIE RIGOUREUX ---

        // 1. Sélection
        Object selectedItem = tableConsultations.getSelectionModel().getSelectedItem();
        if (!(selectedItem instanceof Psychologue selectedPsy)) {
            AlertUtils.showError("Sélection requise", "Veuillez sélectionner un psychologue dans la liste.");
            return;
        }

        // 2. Date
        LocalDate pickedDate = dateRes.getValue();
        if (pickedDate == null) {
            AlertUtils.showError("Date manquante", "Veuillez choisir une date.");
            return;
        }
        if (pickedDate.isBefore(LocalDate.now())) {
            AlertUtils.showError("Date invalide", "La date ne peut pas être dans le passé.");
            return;
        }

        // Optionnel : Bloquer les Week-ends (Samedi=6, Dimanche=7)
        int dayValue = pickedDate.getDayOfWeek().getValue();
        if (dayValue == 7) {
            AlertUtils.showError("Date invalide", "Le psy n'est pas dispo le dimanche.");
            return;
        }

        // 3. Heure
        String heureStr = heureRes.getText().trim();
        if (!heureStr.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            AlertUtils.showError("Format invalide", "L'heure doit être au format HH:mm (ex: 09:00).");
            return;
        }



        // --- FIN CONTRÔLE DE SAISIE ---

        String dateComplete = pickedDate + " " + heureStr;
        try {
            consultationService.reserverConsultation(selectedPsy.getId(), CURRENT_USER_ID, dateComplete);
            AlertUtils.showInfo("Succès", "Demande envoyée !");
            showConsultationsView();
        } catch (SQLException e) {
            AlertUtils.showError("Erreur DB", e.getMessage());
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

    @FXML
    void retourMenu(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/MainMenu.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}