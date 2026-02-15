package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

    // Styles pour le feedback visuel
    private static final String STYLE_ERROR =
            "-fx-background-radius: 5; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 5; -fx-padding: 8;";
    private static final String STYLE_NORMAL =
            "-fx-background-radius: 5; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 8;";
    private static final String STYLE_SUCCESS =
            "-fx-background-radius: 5; -fx-border-color: #27ae60; -fx-border-width: 2; -fx-border-radius: 5; -fx-padding: 8;";

    @FXML
    void initialize() {
        cbRole.getItems().addAll("Patient", "Admin", "Coach");
        cbRole.setValue("Patient");

        // 🎨 Validation en temps réel (optionnel)
        setupRealTimeValidation();
    }

    // Méthode pour setup la validation en temps réel
    private void setupRealTimeValidation() {
        // Validation NOM en temps réel
        tfNom.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfNom.setStyle(STYLE_NORMAL);
            } else if (NAME_PATTERN.matcher(newVal).matches() && newVal.length() >= 2) {
                tfNom.setStyle(STYLE_SUCCESS);
            } else {
                tfNom.setStyle(STYLE_ERROR);
            }
        });

        // Validation PRÉNOM en temps réel
        tfPrenom.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfPrenom.setStyle(STYLE_NORMAL);
            } else if (NAME_PATTERN.matcher(newVal).matches() && newVal.length() >= 2) {
                tfPrenom.setStyle(STYLE_SUCCESS);
            } else {
                tfPrenom.setStyle(STYLE_ERROR);
            }
        });

        // Validation EMAIL en temps réel
        tfEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfEmail.setStyle(STYLE_NORMAL);
            } else if (EMAIL_PATTERN.matcher(newVal).matches()) {
                tfEmail.setStyle(STYLE_SUCCESS);
            } else {
                tfEmail.setStyle(STYLE_ERROR);
            }
        });

        // Validation TÉLÉPHONE en temps réel
        tfTel.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                tfTel.setStyle(STYLE_NORMAL);
            } else if (PHONE_PATTERN.matcher(newVal).matches()) {
                tfTel.setStyle(STYLE_SUCCESS);
            } else {
                tfTel.setStyle(STYLE_ERROR);
            }
        });

        // Validation MOT DE PASSE en temps réel
        pfMdp.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                pfMdp.setStyle(STYLE_NORMAL);
            } else if (newVal.length() >= 6) {
                pfMdp.setStyle(STYLE_SUCCESS);
            } else {
                pfMdp.setStyle(STYLE_ERROR);
            }
        });
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        try {
            // Reset des styles
            resetFieldStyles();

            // Récupération des données
            String nom = tfNom.getText().trim();
            String prenom = tfPrenom.getText().trim();
            String email = tfEmail.getText().trim();
            String mdp = pfMdp.getText();
            String tel = tfTel.getText().trim();
            String role = cbRole.getValue();

            // ========== VALIDATIONS ==========

            // 1. Champs vides
            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
                highlightEmptyFields(nom, prenom, email, mdp);
                showAlert(Alert.AlertType.ERROR, "Champs vides",
                        "Veuillez remplir tous les champs obligatoires !");
                return;
            }

            // 2. Validation NOM
            if (!NAME_PATTERN.matcher(nom).matches()) {
                tfNom.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Nom invalide",
                        "Le nom ne doit contenir que des lettres !\nExemple: Benali, Ben-Salah");
                tfNom.requestFocus();
                return;
            }
            if (nom.length() < 2 || nom.length() > 50) {
                tfNom.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Nom invalide",
                        "Le nom doit contenir entre 2 et 50 caractères !");
                tfNom.requestFocus();
                return;
            }

            // 3. Validation PRÉNOM
            if (!NAME_PATTERN.matcher(prenom).matches()) {
                tfPrenom.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Prénom invalide",
                        "Le prénom ne doit contenir que des lettres !\nExemple: Mohamed, Marie-Claire");
                tfPrenom.requestFocus();
                return;
            }
            if (prenom.length() < 2 || prenom.length() > 50) {
                tfPrenom.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Prénom invalide",
                        "Le prénom doit contenir entre 2 et 50 caractères !");
                tfPrenom.requestFocus();
                return;
            }

            // 4. Validation EMAIL
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                tfEmail.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Email invalide",
                        "Veuillez saisir un email valide !\nExemple: exemple@gmail.com");
                tfEmail.requestFocus();
                return;
            }

            // 5. Validation MOT DE PASSE
            if (mdp.length() < 6) {
                pfMdp.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Mot de passe faible",
                        "Le mot de passe doit contenir au minimum 6 caractères !");
                pfMdp.requestFocus();
                return;
            }
            if (mdp.length() > 100) {
                pfMdp.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Mot de passe trop long",
                        "Le mot de passe ne doit pas dépasser 100 caractères !");
                pfMdp.requestFocus();
                return;
            }

            // 6. Validation TÉLÉPHONE (optionnel)
            if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
                tfTel.setStyle(STYLE_ERROR);
                showAlert(Alert.AlertType.ERROR, "Numéro invalide",
                        "Le numéro doit contenir exactement 8 chiffres !\nExemple: 12345678");
                tfTel.requestFocus();
                return;
            }

            // ========== FIN VALIDATIONS ==========

            // Création User
            User u = new User(0, nom, prenom, email, mdp, role, tel);

            // Appel service
            us.signUp(u);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Compte créé avec succès !\nBienvenue " + prenom + " " + nom + " !");

            // Navigation
            switchToLogin(event);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Inscription", e.getMessage());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible de charger la page de connexion !");
            e.printStackTrace();
        }
    }

    // Highlight les champs vides en rouge
    private void highlightEmptyFields(String nom, String prenom, String email, String mdp) {
        if (nom.isEmpty()) tfNom.setStyle(STYLE_ERROR);
        if (prenom.isEmpty()) tfPrenom.setStyle(STYLE_ERROR);
        if (email.isEmpty()) tfEmail.setStyle(STYLE_ERROR);
        if (mdp.isEmpty()) pfMdp.setStyle(STYLE_ERROR);
    }

    // Reset tous les styles
    private void resetFieldStyles() {
        tfNom.setStyle(STYLE_NORMAL);
        tfPrenom.setStyle(STYLE_NORMAL);
        tfEmail.setStyle(STYLE_NORMAL);
        tfTel.setStyle(STYLE_NORMAL);
        pfMdp.setStyle(STYLE_NORMAL);
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