package controllers;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.Emailservice; // ✅ FIX: était "EmailService" (mauvaise casse)
import services.serviceUser;
import utils.LightDialog;
import utils.VerificationCodeManager;

import java.io.IOException;

public class SignIn {

    @FXML private TextField tfEmail;
    @FXML private PasswordField pfMdp;

    private final serviceUser us = new serviceUser();

    @FXML
    void handleLogin(ActionEvent event) {
        String email = tfEmail.getText().trim();
        String mdp = pfMdp.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            LightDialog.showError("Erreur", "Veuillez remplir tous les champs!");
            return;
        }

        try {
            User user = us.login(email, mdp);

            if (user != null) {
                System.out.println("✅ Credentials valides pour: " + user.getPrenom());

                // ✅ FIX: Emailservice (pas EmailService)
                String code = Emailservice.generateCode();

                boolean emailSent = Emailservice.send2FACode(
                        email,
                        code,
                        user.getPrenom() + " " + user.getNom()
                );

                if (emailSent) {
                    VerificationCodeManager.storeCode(email, code, user);
                    LightDialog.showSuccess("Code envoyé", "Vérifiez votre email: " + email);
                    navigateToVerifyCode(email);
                } else {
                    LightDialog.showError("Erreur",
                            "Impossible d'envoyer le code.\nVérifiez la configuration email.");
                }

            } else {
                LightDialog.showError("Erreur", "Email ou mot de passe incorrect!");
            }

        } catch (Exception e) {
            LightDialog.showError("Erreur", "Erreur de connexion");
            e.printStackTrace();
        }
    }

    private void navigateToVerifyCode(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VerifyCode.fxml"));
            Parent root = loader.load();
            VerifyCode controller = loader.getController();
            controller.setEmail(email);
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Vérification");
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger la page");
        }
    }

    @FXML
    void switchToSignUp(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/SignUp.fxml"));
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleForgotPassword(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ForgotPassword.fxml"));
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}