package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;
import utils.LightDialog;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class ModifierUtilisateur {

    @FXML private TextField tfNom, tfPrenom, tfEmail, tfTel;
    @FXML private PasswordField pfMdp;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label lblTitle;

    private User user;
    private Runnable onUserUpdated;
    private final serviceUser us = new serviceUser();

    private static final Pattern EMAIL_P = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_P = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_P  = Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");

    @FXML
    void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
    }

    public void setUser(User u) {
        this.user = u;
        lblTitle.setText("Modifier : " + u.getPrenom() + " " + u.getNom());
        tfNom.setText(u.getNom());
        tfPrenom.setText(u.getPrenom());
        tfEmail.setText(u.getEmail());
        tfTel.setText(u.getNum_tel() != null ? u.getNum_tel() : "");
        cbRole.setValue(u.getRole().toUpperCase());
    }

    public void setOnUserUpdated(Runnable callback) { this.onUserUpdated = callback; }

    @FXML
    void handleSave(ActionEvent event) {
        String nom    = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String email  = tfEmail.getText().trim();
        String tel    = tfTel.getText().trim();
        String mdp    = pfMdp.getText();
        String role   = cbRole.getValue();

        // Validations
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            LightDialog.showError("Champs requis", "Nom, Prénom et Email sont obligatoires."); return;
        }
        if (!NAME_P.matcher(nom).matches() || nom.length() < 2) {
            LightDialog.showError("Nom invalide", "Min 2 lettres alphabétiques."); return;
        }
        if (!NAME_P.matcher(prenom).matches() || prenom.length() < 2) {
            LightDialog.showError("Prénom invalide", "Min 2 lettres alphabétiques."); return;
        }
        if (!EMAIL_P.matcher(email).matches()) {
            LightDialog.showError("Email invalide", "Format email non valide."); return;
        }
        if (!tel.isEmpty() && !PHONE_P.matcher(tel).matches()) {
            LightDialog.showError("Téléphone invalide", "8 chiffres requis."); return;
        }
        // Unicité email (hors cet utilisateur)
        if (us.emailExistsForOther(email, user.getId_user())) {
            LightDialog.showError("Email déjà utilisé", "Cet email est utilisé par un autre compte."); return;
        }
        // Mot de passe optionnel
        if (!mdp.isEmpty()) {
            if (mdp.length() < 6) {
                LightDialog.showError("Mot de passe", "Minimum 6 caractères."); return;
            }
            if (us.passwordExistsForOther(mdp, user.getId_user())) {
                LightDialog.showError("Mot de passe déjà utilisé", "Choisissez un mot de passe différent."); return;
            }
            user.setMdp(mdp);
        }

        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setNum_tel(tel.isEmpty() ? null : tel);
        user.setRole(role);

        try {
            us.updateOne(user);
            LightDialog.showSuccess("Succès", "Utilisateur mis à jour avec succès !");
            if (onUserUpdated != null) onUserUpdated.run();
            closeDialog();
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de mettre à jour: " + e.getMessage());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) { closeDialog(); }

    private void closeDialog() {
        ((Stage) tfNom.getScene().getWindow()).close();
    }
}