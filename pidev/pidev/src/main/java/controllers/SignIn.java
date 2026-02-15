package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.io.IOException;


public class SignIn {

    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfMdp;

    private final serviceUser us = new serviceUser();

    @FXML
    void handleLogin(ActionEvent event) {
        String email = tfEmail.getText();
        String mdp = pfMdp.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez saisir email et mot de passe.");
            return;
        }

        // Appel de la méthode login
        User connectedUser = us.login(email, mdp);

        if (connectedUser != null) {
            // Login Naje7
            showAlert(Alert.AlertType.INFORMATION, "Bienvenue", "Bonjour " + connectedUser.getPrenom());

            // TODO: Houni t7ott l code bch temchi lel Page d'Accueil (Home)
            // Par exemple : navigateToHome(event);

        } else {
            // Login Ghalet
            showAlert(Alert.AlertType.ERROR, "Erreur", "Email ou mot de passe incorrect.");
        }
    }

    @FXML
    void switchToSignUp(ActionEvent event) throws IOException {
        // Hédhi thezzek l page l Inscription
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignUp.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfEmail.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Inscription");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}