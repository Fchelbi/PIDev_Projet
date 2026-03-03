package controllers;

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
import services.GoogleMeetService;
import entities.Consultation;
import entities.User;
import services.ConsultationService;
import services.SmsService;
import services.serviceUser;
import utils.AlertUtils;

import java.io.IOException;
import java.sql.SQLException;

public class PsyDispoController {

    @FXML private TableView<Consultation> tableRdv;
    @FXML private TableColumn<Consultation, String> colPatient; // Changé en String pour l'affichage
    @FXML private TableColumn<Consultation, String> colDate, colStatut;

    private ConsultationService consultationService = new ConsultationService();

    private final serviceUser us = new serviceUser(); // Ajoute cette ligne ici
    private int CURRENT_PSY_ID=4;


    @FXML
    public void initialize() {
        // Affichage personnalisé pour le patient
        colPatient.setCellValueFactory(cellData -> {
            int idPatient = cellData.getValue().getUtilisateurId();
            try {
                // On utilise ton serviceUser déjà existant pour récupérer les infos
                User p = us.getUserById(idPatient);
                return new SimpleStringProperty(p.getPrenom() + " " + p.getNom());
            } catch (Exception e) {
                return new SimpleStringProperty("ID: " + idPatient);
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateConsultation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colStatut.setCellFactory(column -> new TableCell<Consultation, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(statut);

                switch (statut) {
                    case "Confirmé":
                        setStyle("-fx-background-color: #c8e6c9; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                        break;

                    case "À replanifier":
                        setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #c62828; -fx-font-weight: bold;");
                        break;

                    case "En attente":
                    default:
                        setStyle("-fx-background-color: #ffe0b2; -fx-text-fill: #ef6c00; -fx-font-weight: bold;");
                        break;
                }
            }
        });

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

            // 🔥 Vérification conflit AVANT acceptation
            boolean conflit = consultationService.hasConflit(
                    selected.getPsychologueId(),
                    selected.getDateConsultation(),
                    selected.getId()
            );

            if (conflit) {
                AlertUtils.showError(
                        "Conflit détecté",
                        "⚠️ Vous avez déjà une consultation confirmée à cette heure."
                );
                return;
            }

            // 🚀 1️⃣ Générer lien Google Meet
            String meetLink = GoogleMeetService.createMeeting(selected.getDateConsultation());
            System.out.println("Lien Meet généré : " + meetLink);

            // 🚀 2️⃣ Mettre à jour statut + lien
            consultationService.updateStatutAndLink(
                    selected.getId(),
                    "Confirmé",
                    meetLink
            );

            // 🚀 3️⃣ Envoyer SMS avec lien
            SmsService.envoyerSMS(
                    "+21642253001", // numéro patient (à remplacer dynamiquement plus tard)
                    "Votre consultation est confirmée.\n"
                            + "Lien Google Meet : " + meetLink
            );

            AlertUtils.showInfo("Succès",
                    "Le rendez-vous a été confirmé.\nLien Meet généré avec succès.");

            refreshTable();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
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
            SmsService.envoyerSMS(
                    "+21642253001",
                    "Votre consultation doit être replanifiée. Merci de choisir une autre date."
            );
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


}