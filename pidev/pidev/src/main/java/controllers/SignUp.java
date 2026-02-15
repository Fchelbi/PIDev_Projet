package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class SignUp {

    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfPrenom;
    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfMdp;
    @FXML
    private TextField tfTel;
    @FXML
    private ComboBox<String> cbRole;
    @FXML
    private Button btnSignUp;

    private final serviceUser us = new serviceUser();

    // Regex patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");

    // Styles pour le feedback visuel
    private static final String STYLE_ERROR =
            "-fx-padding: 10; -fx-font-size: 13px; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;";
    private static final String STYLE_NORMAL =
            "-fx-padding: 10; -fx-font-size: 13px; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5;";
    private static final String STYLE_SUCCESS =
            "-fx-padding: 10; -fx-font-size: 13px; -fx-border-color: #27ae60; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;";

    @FXML
    void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
        cbRole.setValue("PATIENT");

        // Setup real-time validation
        setupRealTimeValidation();

        System.out.println("✅ SignUp initialized with validation");
    }

    /**
     * Setup real-time validation for all fields
     */
    private void setupRealTimeValidation() {
        // Nom validation
        tfNom.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfNom.setStyle(STYLE_NORMAL);
            } else if (NAME_PATTERN.matcher(newVal).matches() && newVal.length() >= 2 && newVal.length() <= 50) {
                tfNom.setStyle(STYLE_SUCCESS);
            } else {
                tfNom.setStyle(STYLE_ERROR);
            }
        });

        // Prénom validation
        tfPrenom.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfPrenom.setStyle(STYLE_NORMAL);
            } else if (NAME_PATTERN.matcher(newVal).matches() && newVal.length() >= 2 && newVal.length() <= 50) {
                tfPrenom.setStyle(STYLE_SUCCESS);
            } else {
                tfPrenom.setStyle(STYLE_ERROR);
            }
        });

        // Email validation
        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfEmail.setStyle(STYLE_NORMAL);
            } else if (EMAIL_PATTERN.matcher(newVal).matches()) {
                tfEmail.setStyle(STYLE_SUCCESS);
            } else {
                tfEmail.setStyle(STYLE_ERROR);
            }
        });

        // Password validation
        pfMdp.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                pfMdp.setStyle(STYLE_NORMAL);
            } else if (newVal.length() >= 6 && newVal.length() <= 100) {
                pfMdp.setStyle(STYLE_SUCCESS);
            } else {
                pfMdp.setStyle(STYLE_ERROR);
            }
        });

        // Phone validation (optional)
        tfTel.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfTel.setStyle(STYLE_NORMAL);
            } else if (PHONE_PATTERN.matcher(newVal).matches()) {
                tfTel.setStyle(STYLE_SUCCESS);
            } else {
                tfTel.setStyle(STYLE_ERROR);
            }
        });
    }

    /**
     * Handle Sign Up
     */
    @FXML
    void handleSignUp(ActionEvent event) {
        try {
            String nom = tfNom.getText().trim();
            String prenom = tfPrenom.getText().trim();
            String email = tfEmail.getText().trim();
            String mdp = pfMdp.getText();
            String tel = tfTel.getText().trim();
            String role = cbRole.getValue();

            // ===== VALIDATIONS =====

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Veuillez remplir tous les champs obligatoires!");
                return;
            }

            if (!NAME_PATTERN.matcher(nom).matches() || nom.length() < 2 || nom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Nom invalide",
                        "Le nom doit contenir entre 2 et 50 lettres uniquement!");
                tfNom.requestFocus();
                return;
            }

            if (!NAME_PATTERN.matcher(prenom).matches() || prenom.length() < 2 || prenom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Prénom invalide",
                        "Le prénom doit contenir entre 2 et 50 lettres uniquement!");
                tfPrenom.requestFocus();
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Email invalide",
                        "Veuillez saisir un email valide!\nExemple: exemple@gmail.com");
                tfEmail.requestFocus();
                return;
            }

            if (mdp.length() < 6 || mdp.length() > 100) {
                showAlert(Alert.AlertType.ERROR, "Mot de passe invalide",
                        "Le mot de passe doit contenir entre 6 et 100 caractères!");
                pfMdp.requestFocus();
                return;
            }

            if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
                showAlert(Alert.AlertType.ERROR, "Numéro invalide",
                        "Le numéro doit contenir exactement 8 chiffres!");
                tfTel.requestFocus();
                return;
            }

            // ===== CREATE USER =====

            User newUser = new User(0, nom, prenom, email, mdp, role, tel);
            us.signUp(newUser);

            System.out.println("✅ Compte créé: " + prenom + " " + nom + " (" + role + ")");

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Compte créé avec succès pour : " + prenom + " " + nom);

            // Navigate to dashboard based on role
            navigateByRole(newUser);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    /**
     * Navigate to appropriate dashboard based on role
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
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Rôle non reconnu!");
                    return;
            }

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

            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Espace " + user.getRole());

            System.out.println("✅ Navigation → " + user.getRole() + " Dashboard");

        } catch (IOException e) {
            System.err.println("❌ Erreur navigation: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger l'interface!");
        }
    }

    /**
     * Return to Login page
     */
    @FXML
    void switchToLogin(ActionEvent event) {
        try {
            System.out.println("🔄 Retour → Login");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");

            System.out.println("✅ Page Login chargée");

        } catch (IOException e) {
            System.err.println("❌ Erreur retour Login: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la page de connexion!");
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