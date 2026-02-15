package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

public class SignUp {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfPrenom;
    @FXML
    private TextField tfEmail;
    @FXML
    private TextField tfTel;
    @FXML
    private PasswordField pfMdp;
    @FXML
    private ComboBox<String> cbRole;

    private final serviceUser us = new serviceUser();

    // Regex patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");

    @FXML
    void initialize() {
        cbRole.getItems().addAll("Patient", "Admin", "Coach");
        cbRole.setValue("Patient");
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        try {
            // Récupération des données
            String nom = tfNom.getText().trim();
            String prenom = tfPrenom.getText().trim();
            String email = tfEmail.getText().trim();
            String mdp = pfMdp.getText();
            String tel = tfTel.getText().trim();
            String role = cbRole.getValue();

            // ========== VALIDATIONS ==========

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Champs vides",
                        "Veuillez remplir tous les champs obligatoires !");
                return;
            }

            if (!NAME_PATTERN.matcher(nom).matches() || nom.length() < 2 || nom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Nom invalide",
                        "Le nom doit contenir entre 2 et 50 lettres uniquement !");
                return;
            }

            if (!NAME_PATTERN.matcher(prenom).matches() || prenom.length() < 2 || prenom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Prénom invalide",
                        "Le prénom doit contenir entre 2 et 50 lettres uniquement !");
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Email invalide",
                        "Veuillez saisir un email valide !\nExemple: exemple@gmail.com");
                return;
            }

            if (mdp.length() < 6) {
                showAlert(Alert.AlertType.ERROR, "Mot de passe faible",
                        "Le mot de passe doit contenir au minimum 6 caractères !");
                return;
            }

            if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
                showAlert(Alert.AlertType.ERROR, "Numéro invalide",
                        "Le numéro doit contenir exactement 8 chiffres !");
                return;
            }

            // ========== FIN VALIDATIONS ==========

            // Création User
            User u = new User(0, nom, prenom, email, mdp, role, tel);

            // Appel service
            us.signUp(u);

            System.out.println("✅ Compte créé: " + prenom + " " + nom + " (" + role + ")");

            // 🎯 Demander à l'utilisateur: Se connecter maintenant ou plus tard?
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Inscription réussie");
            confirmAlert.setHeaderText("Compte créé avec succès !");
            confirmAlert.setContentText("Voulez-vous vous connecter maintenant ?");

            ButtonType btnOui = new ButtonType("Oui, me connecter");
            ButtonType btnNon = new ButtonType("Non, plus tard");
            confirmAlert.getButtonTypes().setAll(btnOui, btnNon);

            Optional<ButtonType> result = confirmAlert.showAndWait();

            if (result.isPresent() && result.get() == btnOui) {
                // 🔥 Option 1: Se connecter directement (navigation par rôle)
                navigateByRole(u, event);
            } else {
                // Option 2: Retour au Login
                switchToLogin(event);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Inscription", e.getMessage());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible de charger la page !");
            e.printStackTrace();
        }
    }

    /**
     * 🎯 Navigation basée sur le rôle (après inscription)
     */
    private void navigateByRole(User user, ActionEvent event) throws IOException {
        String fxmlFile = "";
        String pageTitle = "";

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
                showAlert(Alert.AlertType.ERROR, "Erreur", "Rôle inconnu!");
                return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();

        // Passer l'utilisateur au controller
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
        stage.setTitle(pageTitle);
    }

    @FXML
    void switchToLogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfEmail.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Connexion");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}