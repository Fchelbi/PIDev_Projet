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

public class Modifierutilisateur {

    @FXML
    private TextField tfId;
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

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private Runnable onUserUpdated;

    // Regex patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");

    @FXML
    void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
    }

    /**
     * Set the user to modify
     */
    public void setUser(User user) {
        this.currentUser = user;

        // Fill fields
        tfId.setText(String.valueOf(user.getId_user()));
        tfNom.setText(user.getNom());
        tfPrenom.setText(user.getPrenom());
        tfEmail.setText(user.getEmail());
        tfTel.setText(user.getNum_tel());
        cbRole.setValue(user.getRole().toUpperCase());

        System.out.println("✏️ Editing user: " + user.getNom());
    }

    public void setOnUserUpdated(Runnable callback) {
        this.onUserUpdated = callback;
    }

    /**
     * Handle Modifier button
     */
    @FXML
    void handleModifier(ActionEvent event) {
        try {
            // Get data
            String nom = tfNom.getText().trim();
            String prenom = tfPrenom.getText().trim();
            String email = tfEmail.getText().trim();
            String mdp = pfMdp.getText().trim();
            String tel = tfTel.getText().trim();
            String role = cbRole.getValue();

            // ===== VALIDATIONS =====

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Veuillez remplir tous les champs obligatoires!");
                return;
            }

            if (!NAME_PATTERN.matcher(nom).matches() || nom.length() < 2 || nom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Nom invalide",
                        "Le nom doit contenir entre 2 et 50 lettres uniquement!");
                return;
            }

            if (!NAME_PATTERN.matcher(prenom).matches() || prenom.length() < 2 || prenom.length() > 50) {
                showAlert(Alert.AlertType.ERROR, "Prénom invalide",
                        "Le prénom doit contenir entre 2 et 50 lettres uniquement!");
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showAlert(Alert.AlertType.ERROR, "Email invalide",
                        "Veuillez saisir un email valide!");
                return;
            }

            // Password is optional for update
            if (!mdp.isEmpty() && mdp.length() < 6) {
                showAlert(Alert.AlertType.ERROR, "Mot de passe faible",
                        "Le mot de passe doit contenir au minimum 6 caractères!");
                return;
            }

            if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
                showAlert(Alert.AlertType.ERROR, "Numéro invalide",
                        "Le numéro doit contenir exactement 8 chiffres!");
                return;
            }

            // ===== UPDATE USER =====

            // Keep current password if not changed
            String finalMdp = mdp.isEmpty() ? currentUser.getMdp() : mdp;

            User updatedUser = new User(
                    currentUser.getId_user(),
                    nom,
                    prenom,
                    email,
                    finalMdp,
                    role,
                    tel
            );

            us.updateOne(updatedUser);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Utilisateur modifié avec succès!");

            // Callback
            if (onUserUpdated != null) {
                onUserUpdated.run();
            }

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