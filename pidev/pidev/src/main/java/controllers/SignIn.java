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
        String email = tfEmail.getText().trim();
        String mdp = pfMdp.getText();

        // Validation simple
        if (email.isEmpty() || mdp.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez saisir email et mot de passe.");
            return;
        }

        // Appel de la méthode login
        User connectedUser = us.login(email, mdp);

        if (connectedUser != null) {
            // Login Réussi ✅
            System.out.println("✅ Connexion réussie pour: " + connectedUser.getPrenom() + " (" + connectedUser.getRole() + ")");

            // 🎯 Navigation basée sur le rôle
            navigateByRole(connectedUser, event);

        } else {
            // Login Échoué ❌
            showAlert(Alert.AlertType.ERROR, "Erreur", "Email ou mot de passe incorrect.");
        }
    }

    /**
     * 🎯 Méthode pour naviguer vers la bonne page selon le rôle
     */
    private void navigateByRole(User user, ActionEvent event) {
        try {
            String fxmlFile = "";
            String pageTitle = "";

            // Déterminer la page selon le rôle
            switch (user.getRole().toLowerCase()) {
                case "admin":
                    fxmlFile = "/AdminHome.fxml";
                    pageTitle = "Espace Administrateur";
                    break;

                case "coach":
                    fxmlFile = "/CoachHome.fxml";
                    pageTitle = "Espace Coach";
                    break;

                case "patient":
                    fxmlFile = "/PatientHome.fxml";
                    pageTitle = "Espace Patient";
                    break;

                default:
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Rôle inconnu: " + user.getRole());
                    return;
            }

            // Charger la page correspondante
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // 🔥 IMPORTANT: Passer les données de l'utilisateur au controller de destination
            Object controller = loader.getController();

            // Appeler setUser() sur le controller (si existe)
            if (controller instanceof AdminHome) {
                ((AdminHome) controller).setUser(user);
            } else if (controller instanceof CoachHome) {
                ((CoachHome) controller).setUser(user);
            } else if (controller instanceof PatientHome) {
                ((PatientHome) controller).setUser(user);
            }

            // Changer la scène
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(pageTitle);

            // Message de bienvenue dans la console
            System.out.println("🏠 Navigation vers: " + pageTitle);

            // Optionnel: Alert de bienvenue
            showAlert(Alert.AlertType.INFORMATION, "Bienvenue",
                    "Bonjour " + user.getPrenom() + " " + user.getNom() + " !");

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement de la page: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la page d'accueil.\nVérifiez que les fichiers FXML existent.");
            e.printStackTrace();
        }
    }

    @FXML
    void switchToSignUp(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignUp.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfEmail.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Inscription");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}