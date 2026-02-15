package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class AjouterUtilisateur {

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

    private final serviceUser us = new serviceUser();
    private Runnable onUserAdded;

    // Regex patterns for validation
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
        // Setup ComboBox
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
        cbRole.setValue("PATIENT");

        // Setup real-time validation
        setupRealTimeValidation();
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
            } else if (newVal.length() >= 6) {
                pfMdp.setStyle(STYLE_SUCCESS);
            } else {
                pfMdp.setStyle(STYLE_ERROR);
            }
        });

        // Phone validation
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

    public void setOnUserAdded(Runnable callback) {
        this.onUserAdded = callback;
    }

    /**
     * Handle Ajouter button
     */
    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            // Get data
            String nom = tfNom.getText().trim();
            String prenom = tfPrenom.getText().trim();
            String email = tfEmail.getText().trim();
            String mdp = pfMdp.getText();
            String tel = tfTel.getText().trim();
            String role = cbRole.getValue();

            // ===== VALIDATIONS =====

            // 1. Empty fields
            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Veuillez remplir tous les champs obligatoires!");
                return;
            }

            // 2. Validate NOM
            if (!NAME_PATTERN.matcher(nom).matches() || nom.length() < 2 || nom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Nom invalide",
                        "Le nom doit contenir entre 2 et 50 lettres uniquement!");
                tfNom.requestFocus();
                return;
            }

            // 3. Validate PRÉNOM
            if (!NAME_PATTERN.matcher(prenom).matches() || prenom.length() < 2 || prenom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Prénom invalide",
                        "Le prénom doit contenir entre 2 et 50 lettres uniquement!");
                tfPrenom.requestFocus();
                return;
            }

            // 4. Validate EMAIL
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Email invalide",
                        "Veuillez saisir un email valide!\nExemple: exemple@gmail.com");
                tfEmail.requestFocus();
                return;
            }

            // 5. Validate PASSWORD
            if (mdp.length() < 6) {
                showAlert(Alert.AlertType.ERROR, "Mot de passe faible",
                        "Le mot de passe doit contenir au minimum 6 caractères!");
                pfMdp.requestFocus();
                return;
            }

            // 6. Validate PHONE (optional)
            if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
                showAlert(Alert.AlertType.ERROR, "Numéro invalide",
                        "Le numéro doit contenir exactement 8 chiffres!");
                tfTel.requestFocus();
                return;
            }

            // ===== CREATE USER =====

            User newUser = new User(0, nom, prenom, email, mdp, role, tel);

            // Call service
            us.signUp(newUser);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Utilisateur ajouté avec succès!");

            // Callback to refresh table
            if (onUserAdded != null) {
                onUserAdded.run();
            }

            // Close dialog
            closeDialog();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    /**
     * Handle Annuler button
     */
    @FXML
    void handleAnnuler(ActionEvent event) {
        closeDialog();
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}