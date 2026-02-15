package controllers;

import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.serviceUser;

import java.io.IOException;

public class SignIn {

    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfMdp;

    private final serviceUser us = new serviceUser();

    @FXML
    void initialize() {
        System.out.println("✅ SignIn initialized");
    }

    /**
     * Handle Login
     */
    @FXML
    void handleLogin(ActionEvent event) {
        String email = tfEmail.getText().trim();
        String mdp = pfMdp.getText();

        if (email.isEmpty() || mdp.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Veuillez remplir tous les champs!");
            return;
        }

        try {
            User user = us.login(email, mdp);

            if (user != null) {
                System.out.println("✅ Connexion réussie pour: " + user.getPrenom() + " (" + user.getRole() + ")");
                navigateByRole(user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Email ou mot de passe incorrect!");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur de connexion à la base de données.");
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    /**
     * Navigate to appropriate dashboard based on user role
     */
    private void navigateByRole(User user) {
        try {
            String fxmlFile;

            switch (user.getRole().toLowerCase()) {
                case "admin":
                    fxmlFile = "/AdminHome.fxml";
                    break;
                case "coach":
                    fxmlFile = "/CoachHome.fxml";
                    break;
                case "patient":
                    fxmlFile = "/PatientHome.fxml";
                    break;
                default:
                    System.out.println("❌ Rôle inconnu: " + user.getRole());
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Rôle utilisateur non reconnu!");
                    return;
            }

            System.out.println("🔄 Chargement: " + fxmlFile);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Pass user to controller
            Object controller = loader.getController();

            if (controller instanceof AdminHome) {
                ((AdminHome) controller).setUser(user);
            } else if (controller instanceof CoachHome) {
                ((CoachHome) controller).setUser(user);
            } else if (controller instanceof PatientHome) {
                ((PatientHome) controller).setUser(user);
            }

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Espace " + user.getRole());

            System.out.println("✅ Navigation vers: Espace " + user.getRole());

        } catch (IOException e) {
            System.err.println("❌ Erreur de navigation: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger l'interface!");
        }
    }

    /**
     * Switch to Sign Up page
     */
    @FXML
    void switchToSignUp(ActionEvent event) {
        try {
            System.out.println("🔄 Navigation → Sign Up");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignUp.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Inscription");

            System.out.println("✅ Page Sign Up chargée");

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement Sign Up: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la page d'inscription!");
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}