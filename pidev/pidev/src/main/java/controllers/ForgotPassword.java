package controllers;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.Emailservice;
import services.serviceUser;
import utils.LightDialog;
import utils.PasswordResetManager;

import java.io.IOException;
import java.sql.SQLException;

public class ForgotPassword {

    @FXML private TextField tfEmail;

    private final serviceUser us = new serviceUser();

    @FXML
    void handleSendCode(ActionEvent event) {
        String email = tfEmail.getText().trim();

        if (email.isEmpty()) {
            LightDialog.showError("Erreur", "Entrez votre email!");
            return;
        }

        try {
            User user = us.getUserByEmail(email);

            if (user != null) {
                String code = Emailservice.generateCode();
                PasswordResetManager.storeCode(email, code);

                boolean sent = Emailservice.sendPasswordResetCode(
                        email,
                        code,
                        user.getPrenom() + " " + user.getNom()
                );

                if (sent) {
                    LightDialog.showSuccess("Email envoyé",
                            "Vérifiez votre boîte mail!");
                    navigateToResetPassword(email);
                } else {
                    LightDialog.showError("Erreur",
                            "Impossible d'envoyer l'email");
                }

            } else {
                LightDialog.showError("Erreur",
                        "Aucun compte avec cet email!");
            }

        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Erreur base de données");
            e.printStackTrace();
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        navigateToLogin();
    }

    private void navigateToResetPassword(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
            Parent root = loader.load();

            ResetPassword controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}