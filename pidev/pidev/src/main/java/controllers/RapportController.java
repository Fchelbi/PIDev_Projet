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
        if (cbPatient.getValue() == null) {
            LightDialog.showError("Erreur", "Sélectionnez un patient!"); return;
        }
        if (taContenu.getText().trim().isEmpty()) {
            LightDialog.showError("Erreur", "Ajoutez des observations!"); return;
        }
        if (taRecommandations.getText().trim().isEmpty()) {
            LightDialog.showError("Erreur", "Ajoutez des recommandations!"); return;
        }
        if (tfNbSeances.getText().trim().isEmpty()) {
            LightDialog.showError("Erreur", "Indiquez le nombre de séances!"); return;
        }
        if (tfPeriode.getText().trim().isEmpty()) {
            LightDialog.showError("Erreur", "Indiquez la période!"); return;
        }

        try {
            int selectedIndex = cbPatient.getSelectionModel().getSelectedIndex();
            User patient = patients.get(selectedIndex);

            Rapport rapport = new Rapport();
            rapport.setId_patient(patient.getId_user());
            rapport.setId_coach(currentCoach.getId_user());
            rapport.setContenu(taContenu.getText().trim());
            rapport.setRecommandations(taRecommandations.getText().trim());
            rapport.setNb_seances(Integer.parseInt(tfNbSeances.getText().trim()));
            rapport.setScore_humeur(sliderHumeur.getValue());
            rapport.setPeriode(tfPeriode.getText().trim());

            String pdfPath = PDFGenerator.generateRapport(patient, currentCoach, rapport);

            if (LightDialog.showConfirmation("Succès",
                    "Rapport PDF généré!\nVoulez-vous l'ouvrir?", "📄")) {
                Desktop.getDesktop().open(new File(pdfPath));
            }

            resetForm();

        } catch (NumberFormatException e) {
            LightDialog.showError("Erreur", "Nombre de séances invalide!");
        } catch (IOException e) {
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