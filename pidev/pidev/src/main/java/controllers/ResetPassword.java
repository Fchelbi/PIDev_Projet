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

public class ResetPassword {

    @FXML private TextField tfCode;
    @FXML private PasswordField pfNewPassword, pfConfirm;

    private String userEmail;
    private final serviceUser us = new serviceUser();

    public void setEmail(String email) {
        this.userEmail = email;
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

        if (newPass.length() < 6) {
            LightDialog.showError("Erreur", "Min 6 caractères!");
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
