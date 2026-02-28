package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entities.Rapport;
import entities.User;
import services.PDFGenerator;
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
    // ✅ DatePickers pour la période (optionnels - peuvent ne pas exister dans le FXML)
    @FXML private javafx.scene.control.DatePicker dpDebut, dpFin;

    private User currentCoach;
    private List<User> patients;
    private final serviceUser us = new serviceUser();

    public void setCoach(User coach) {
        this.currentCoach = coach;
        loadPatients();
    }

    @FXML
    void initialize() {
        sliderHumeur.valueProperty().addListener((obs, old, newVal) -> {
            double val = newVal.doubleValue();
            lblHumeurValue.setText(String.format("%.1f / 10", val));
            if (val >= 8) lblHumeurEmoji.setText("😄");
            else if (val >= 6) lblHumeurEmoji.setText("😊");
            else if (val >= 4) lblHumeurEmoji.setText("😐");
            else if (val >= 2) lblHumeurEmoji.setText("😔");
            else lblHumeurEmoji.setText("😢");
        });

        // ✅ Auto-remplir tfPeriode depuis DatePickers
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (dpDebut != null) {
            dpDebut.valueProperty().addListener((o, ov, nv) -> updatePeriodeField(fmt));
        }
        if (dpFin != null) {
            dpFin.valueProperty().addListener((o, ov, nv) -> updatePeriodeField(fmt));
        }
    }

    private void updatePeriodeField(java.time.format.DateTimeFormatter fmt) {
        String debut = dpDebut != null && dpDebut.getValue() != null ? dpDebut.getValue().format(fmt) : "";
        String fin   = dpFin   != null && dpFin.getValue()   != null ? dpFin.getValue().format(fmt) : "";
        if (!debut.isEmpty() && !fin.isEmpty()) tfPeriode.setText(debut + " → " + fin);
        else if (!debut.isEmpty()) tfPeriode.setText("Depuis le " + debut);
    }

    private void loadPatients() {
        try {
            patients = us.selectALL().stream()
                    .filter(u -> u.getRole().equalsIgnoreCase("PATIENT"))
                    .toList();

            for (User p : patients) {
                cbPatient.getItems().add(p.getPrenom() + " " + p.getNom() + " (" + p.getEmail() + ")");
            }
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les patients.");
        }
    }

    @FXML
    void handleGenerate() {
        // ✅ Validation complète avec messages précis
        if (currentCoach == null) {
            LightDialog.showError("Erreur", "Coach non défini. Rechargez la page."); return;
        }
        if (cbPatient.getValue() == null) {
            LightDialog.showError("Patient requis", "Sélectionnez un patient dans la liste!"); return;
        }
        String contenu = taContenu.getText().trim();
        if (contenu.isEmpty()) {
            LightDialog.showError("Observations manquantes", "Rédigez vos observations cliniques!"); return;
        }
        if (contenu.length() < 20) {
            LightDialog.showError("Observations trop courtes", "Minimum 20 caractères pour les observations."); return;
        }
        String recos = taRecommandations.getText().trim();
        if (recos.isEmpty()) {
            LightDialog.showError("Recommandations manquantes", "Ajoutez au moins une recommandation!"); return;
        }
        String seancesStr = tfNbSeances.getText().trim();
        if (seancesStr.isEmpty()) {
            LightDialog.showError("Séances manquantes", "Indiquez le nombre de séances effectuées!"); return;
        }
        int nbSeances;
        try {
            nbSeances = Integer.parseInt(seancesStr);
            if (nbSeances < 0 || nbSeances > 999) {
                LightDialog.showError("Valeur invalide", "Le nombre de séances doit être entre 0 et 999."); return;
            }
        } catch (NumberFormatException e) {
            LightDialog.showError("Format invalide", "Le nombre de séances doit être un entier (ex: 5)"); return;
        }
        if (tfPeriode.getText().trim().isEmpty()) {
            LightDialog.showError("Période manquante", "Indiquez la période de suivi (ex: Jan-Mar 2025)!"); return;
        }

        try {
            int selectedIndex = cbPatient.getSelectionModel().getSelectedIndex();
            User patient = patients.get(selectedIndex);

            Rapport rapport = new Rapport();
            rapport.setId_patient(patient.getId_user());
            rapport.setId_coach(currentCoach.getId_user());
            rapport.setContenu(contenu);
            rapport.setRecommandations(recos);
            rapport.setNb_seances(nbSeances);
            rapport.setScore_humeur(sliderHumeur.getValue());
            rapport.setPeriode(tfPeriode.getText().trim());

            String pdfPath = PDFGenerator.generateRapport(patient, currentCoach, rapport);
            LightDialog.showSuccess("✅ PDF Généré", "Rapport sauvegardé: " + new File(pdfPath).getName());

            if (LightDialog.showConfirmation("Ouvrir le rapport",
                    "Voulez-vous ouvrir le PDF maintenant?", "📄")) {
                try { Desktop.getDesktop().open(new File(pdfPath)); }
                catch (Exception ex) { LightDialog.showInfo("Info", "Ouvrez manuellement: " + pdfPath); }
            }

            resetForm();

        } catch (IOException e) {
            LightDialog.showError("Erreur PDF", "Impossible de générer le PDF: " + e.getMessage());
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de générer le PDF.");
            e.printStackTrace();
        }
    }

    @FXML
    void resetForm() {
        cbPatient.setValue(null);
        taContenu.clear();
        taRecommandations.clear();
        tfNbSeances.clear();
        tfPeriode.clear();
        sliderHumeur.setValue(5);
    }

    @FXML
    void handleCancel() {
        ((Stage) cbPatient.getScene().getWindow()).close();
    }
}