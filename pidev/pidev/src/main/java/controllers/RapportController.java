package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entities.Rapport;
import entities.User;
import services.PDFGenerator;
import services.Servicerapport;
import services.serviceUser;
import utils.LightDialog;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class RapportController {

    @FXML private ComboBox<String> cbPatient;
    @FXML private TextArea taContenu, taRecommandations;
    @FXML private TextField tfNbSeances, tfPeriode;
    @FXML private Slider sliderHumeur;
    @FXML private Label lblHumeurValue, lblHumeurEmoji;

    private User currentCoach;
    private List<User> patients;
    private final serviceUser    us = new serviceUser();
    private final Servicerapport rs = new Servicerapport();

    public void setCoach(User coach) {
        this.currentCoach = coach;
        loadPatients();
    }

    @FXML void initialize() {
        sliderHumeur.valueProperty().addListener((obs, old, newVal) -> {
            double val = newVal.doubleValue();
            lblHumeurValue.setText(String.format("%.1f / 10", val));
            if      (val >= 8) lblHumeurEmoji.setText("😄");
            else if (val >= 6) lblHumeurEmoji.setText("😊");
            else if (val >= 4) lblHumeurEmoji.setText("😐");
            else if (val >= 2) lblHumeurEmoji.setText("😔");
            else               lblHumeurEmoji.setText("😢");
        });
    }

    private void loadPatients() {
        try {
            patients = us.selectALL().stream()
                    .filter(u -> u.getRole().equalsIgnoreCase("PATIENT"))
                    .toList();
            for (User p : patients)
                cbPatient.getItems().add(p.getPrenom() + " " + p.getNom() + " (" + p.getEmail() + ")");
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les patients.");
        }
    }

    @FXML void resetForm() {
        cbPatient.getSelectionModel().clearSelection();
        taContenu.clear();
        taRecommandations.clear();
        tfNbSeances.clear();
        tfPeriode.clear();
        sliderHumeur.setValue(5.0);
    }

    @FXML void handleGenerate() {
        int idx = cbPatient.getSelectionModel().getSelectedIndex();
        if (idx < 0) { LightDialog.showError("Erreur", "Sélectionnez un patient."); return; }
        if (taContenu.getText().trim().isEmpty()) { LightDialog.showError("Erreur", "Saisissez le contenu."); return; }

        User patient = patients.get(idx);

        Rapport rapport = new Rapport();
        rapport.setId_patient(patient.getId_user());
        rapport.setId_coach(currentCoach.getId_user());
        rapport.setContenu(taContenu.getText().trim());
        rapport.setRecommandations(taRecommandations.getText().trim());
        rapport.setNb_seances(Integer.parseInt(tfNbSeances.getText().trim().isEmpty() ? "1" : tfNbSeances.getText().trim()));
        rapport.setScore_humeur(sliderHumeur.getValue());
        rapport.setPeriode(tfPeriode.getText().trim());

        try {
            // 1. Générer le PDF
            String pdfPath = PDFGenerator.generateRapport(patient, currentCoach, rapport);
            rapport.setFichier_pdf(pdfPath);

            // 2. Sauvegarder en BD (pour que CoachStats puisse lire le vrai score)
            rs.save(rapport);

            LightDialog.showSuccess("Succès", "Rapport généré et enregistré ✅");

            // 3. Ouvrir le PDF
            if (Desktop.isDesktopSupported()) {
                File file = new File(pdfPath);
                if (file.exists()) Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            LightDialog.showError("Erreur PDF", "Impossible de générer le PDF : " + e.getMessage());
        } catch (SQLException e) {
            LightDialog.showError("Erreur BD", "PDF généré mais non enregistré en BD : " + e.getMessage());
        }
    }
}