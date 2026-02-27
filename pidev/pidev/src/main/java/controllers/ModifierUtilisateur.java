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
    @FXML private TextField tfId, tfNom, tfPrenom, tfEmail, tfTel;
    @FXML private PasswordField pfMdp;
    @FXML private ComboBox<String> cbRole;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private Runnable onUserUpdated;
    private static final Pattern EMAIL_P = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_P = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_P = Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");

    private static final String S_N = "-fx-background-color: #F7FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14px;";
    private static final String S_E = "-fx-background-color: #FFF5F5; -fx-border-color: #E57373; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14px;";
    private static final String S_S = "-fx-background-color: #F0FFF4; -fx-border-color: #81C995; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14px;";

    @FXML void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
        tfNom.textProperty().addListener((o, ov, nv) -> tfNom.setStyle(nv.isEmpty() ? S_N : NAME_P.matcher(nv).matches() && nv.length() >= 2 ? S_S : S_E));
        tfPrenom.textProperty().addListener((o, ov, nv) -> tfPrenom.setStyle(nv.isEmpty() ? S_N : NAME_P.matcher(nv).matches() && nv.length() >= 2 ? S_S : S_E));
        tfEmail.textProperty().addListener((o, ov, nv) -> tfEmail.setStyle(nv.isEmpty() ? S_N : EMAIL_P.matcher(nv).matches() ? S_S : S_E));
        pfMdp.textProperty().addListener((o, ov, nv) -> pfMdp.setStyle(nv.isEmpty() ? S_N : nv.length() >= 6 ? S_S : S_E));
        tfTel.textProperty().addListener((o, ov, nv) -> tfTel.setStyle(nv.isEmpty() ? S_N : PHONE_P.matcher(nv).matches() ? S_S : S_E));
    }

    public void setUser(User user) {
        this.currentUser = user;
        tfId.setText(String.valueOf(user.getId_user()));
        tfNom.setText(user.getNom()); tfPrenom.setText(user.getPrenom());
        tfEmail.setText(user.getEmail()); tfTel.setText(user.getNum_tel());
        cbRole.setValue(user.getRole().toUpperCase());
    }

    public void setOnUserUpdated(Runnable cb) { this.onUserUpdated = cb; }

    @FXML void handleModifier(ActionEvent event) {
        String n = tfNom.getText().trim(), p = tfPrenom.getText().trim(), e = tfEmail.getText().trim(), m = pfMdp.getText().trim(), t = tfTel.getText().trim();
        if (n.isEmpty() || p.isEmpty() || e.isEmpty()) { LightDialog.showError("Erreur", "Champs obligatoires!"); return; }
        if (!NAME_P.matcher(n).matches() || n.length() < 2) { LightDialog.showError("Nom", "Min 2 lettres!"); return; }
        if (!NAME_P.matcher(p).matches() || p.length() < 2) { LightDialog.showError("Prénom", "Min 2 lettres!"); return; }
        if (!EMAIL_P.matcher(e).matches()) { LightDialog.showError("Email", "Email invalide!"); return; }
        if (!m.isEmpty() && m.length() < 6) { LightDialog.showError("Mot de passe", "Min 6 caractères!"); return; }
        if (!t.isEmpty() && !PHONE_P.matcher(t).matches()) { LightDialog.showError("Téléphone", "8 chiffres!"); return; }
        try {
            us.updateOne(new User(currentUser.getId_user(), n, p, e, m.isEmpty() ? currentUser.getMdp() : m, cbRole.getValue(), t));
            LightDialog.showSuccess("Succès", "Utilisateur modifié !");
            if (onUserUpdated != null) onUserUpdated.run();
            ((Stage) tfNom.getScene().getWindow()).close();
        } catch (SQLException ex) { LightDialog.showError("Erreur", ex.getMessage()); }
    }

    @FXML void handleAnnuler(ActionEvent event) { ((Stage) tfNom.getScene().getWindow()).close(); }
}