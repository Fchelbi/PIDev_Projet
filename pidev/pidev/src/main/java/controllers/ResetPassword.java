package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.serviceUser;
import utils.LightDialog;
import utils.PasswordResetManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class ResetPassword {

    // ✅ Mot de passe fort: 8+ chars, 1 majuscule, 1 chiffre, 1 spécial
    private static final Pattern PASS_P = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    @FXML private TextField tfCode;
    @FXML private PasswordField pfNewPassword, pfConfirm;
    @FXML private Label lblPassStrength; // peut être null si pas dans le FXML

    private String userEmail;
    private final serviceUser us = new serviceUser();

    public void setEmail(String email) { this.userEmail = email; }

    @FXML
    void initialize() {
        // ✅ Indicateur de force en temps réel
        if (pfNewPassword != null) {
            pfNewPassword.textProperty().addListener((o, ov, nv) -> {
                if (lblPassStrength == null || nv.isEmpty()) return;
                if (nv.length() < 8) setStrength("Trop court (min 8 chars)", "#C07050");
                else if (!nv.matches(".*[A-Z].*")) setStrength("Ajoutez 1 majuscule (A-Z)", "#C07050");
                else if (!nv.matches(".*[0-9].*")) setStrength("Ajoutez 1 chiffre (0-9)", "#C07050");
                else if (!PASS_P.matcher(nv).matches()) setStrength("Ajoutez 1 caractère spécial", "#C07050");
                else setStrength("✓ Mot de passe fort", "#4A8A5A");
            });
        }
    }

    private void setStrength(String msg, String color) {
        lblPassStrength.setText(msg);
        lblPassStrength.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
    }

    @FXML
    void handleReset(ActionEvent event) {
        String code = tfCode.getText().trim();
        String newPass = pfNewPassword.getText();
        String confirm = pfConfirm.getText();

        if (code.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            LightDialog.showError("Erreur", "Remplissez tous les champs!");
            return;
        }

        if (!PASS_P.matcher(newPass).matches()) {
            String hint = newPass.length() < 8 ? "Min 8 caractères requis" :
                    !newPass.matches(".*[A-Z].*") ? "Ajoutez au moins 1 majuscule" :
                            !newPass.matches(".*[0-9].*") ? "Ajoutez au moins 1 chiffre" :
                                    "Ajoutez 1 caractère spécial (!@#$%...)";
            LightDialog.showError("Mot de passe faible", hint);
            return;
        }

        if (!newPass.equals(confirm)) {
            LightDialog.showError("Erreur", "Mots de passe différents!");
            return;
        }

        if (PasswordResetManager.verifyCode(userEmail, code)) {
            try {
                us.updatePassword(userEmail, newPass);
                PasswordResetManager.removeCode(userEmail);

                LightDialog.showSuccess("Succès",
                        "Mot de passe modifié avec succès!");

                navigateToLogin();

            } catch (SQLException e) {
                LightDialog.showError("Erreur",
                        "Impossible de modifier le mot de passe");
                e.printStackTrace();
            }
        } else {
            LightDialog.showError("Erreur",
                    "Code incorrect ou expiré!");
            tfCode.clear();
            tfCode.requestFocus();
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}