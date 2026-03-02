package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;
import utils.LightDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class SignUp {
    @FXML private TextField tfNom, tfPrenom, tfEmail, tfTel;
    @FXML private PasswordField pfMdp, pfConfirm;
    @FXML private ComboBox<String> cbRole;
    @FXML private ImageView imgSignUpPhoto;
    @FXML private Label lblPhotoPlaceholder;
    @FXML private StackPane photoContainer;
    @FXML private Label lblStrength;    // indicateur force mdp (optionnel, null-safe)

    private final serviceUser us = new serviceUser();
    private String selectedPhotoPath = null;
    private static final String PHOTOS_DIR = "user_photos";

    private static final Pattern EMAIL_P = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_P = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_P  = Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");
    // Fort: min 8 chars, 1 maj, 1 chiffre, 1 spécial
    private static final Pattern PWD_P   = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-\\[\\]{}|;:,.<>?]).{8,}$");

    @FXML
    void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
        cbRole.setValue("PATIENT");
        new File(PHOTOS_DIR).mkdirs();

        // Indicateur force mot de passe (temps réel)
        pfMdp.textProperty().addListener((obs, old, text) -> {
            if (lblStrength == null) return;
            int score = 0;
            if (text.length() >= 8) score++;
            if (text.matches(".*[A-Z].*")) score++;
            if (text.matches(".*[0-9].*")) score++;
            if (text.matches(".*[!@#$%^&*()_+\\-\\[\\]{}|;:,.<>?].*")) score++;
            switch (score) {
                case 0,1 -> { lblStrength.setText("🔴  Très faible"); lblStrength.setStyle("-fx-text-fill:#E07070;-fx-font-size:11px;"); }
                case 2   -> { lblStrength.setText("🟠  Faible");      lblStrength.setStyle("-fx-text-fill:#E8956D;-fx-font-size:11px;"); }
                case 3   -> { lblStrength.setText("🟡  Moyen");       lblStrength.setStyle("-fx-text-fill:#D4A820;-fx-font-size:11px;"); }
                case 4   -> { lblStrength.setText("🟢  Fort ✓");      lblStrength.setStyle("-fx-text-fill:#52B788;-fx-font-size:11px;-fx-font-weight:700;"); }
            }
            lblStrength.setVisible(!text.isEmpty());
        });
    }

    @FXML
    void handleChoosePhoto(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(tfNom.getScene().getWindow());
        if (file != null) {
            try {
                selectedPhotoPath = file.getAbsolutePath();
                Image img = new Image(file.toURI().toString(), 100, 100, false, true);
                imgSignUpPhoto.setImage(img);
                imgSignUpPhoto.setVisible(true);
                if (lblPhotoPlaceholder != null) lblPhotoPlaceholder.setVisible(false);
            } catch (Exception e) {
                LightDialog.showError("Erreur", "Impossible de charger la photo.");
            }
        }
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        String nom    = tfNom.getText().trim(),
                prenom = tfPrenom.getText().trim(),
                email  = tfEmail.getText().trim(),
                mdp    = pfMdp.getText(),
                mdp2   = pfConfirm.getText(),
                tel    = tfTel.getText().trim(),
                role   = cbRole.getValue();

        // ── Validations ──
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            LightDialog.showError("Champs requis", "Remplissez tous les champs obligatoires !"); return;
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
        if (!PWD_P.matcher(mdp).matches()) {
            LightDialog.showError("Mot de passe faible",
                    "Le mot de passe doit contenir au moins :\n" +
                            "  • 8 caractères minimum\n" +
                            "  • 1 lettre majuscule (A-Z)\n" +
                            "  • 1 chiffre (0-9)\n" +
                            "  • 1 caractère spécial (!@#$%...)"); return;
        }
        if (!mdp.equals(mdp2)) {
            LightDialog.showError("Confirmation", "Les mots de passe ne correspondent pas."); return;
        }
        if (!tel.isEmpty() && !PHONE_P.matcher(tel).matches()) {
            LightDialog.showError("Téléphone", "8 chiffres requis."); return;
        }
        // ── Unicité email ──
        if (us.emailExists(email)) {
            LightDialog.showError("Email déjà utilisé", "Un compte existe déjà avec cet email."); return;
        }
        // ── Unicité mot de passe ──
        if (us.passwordExists(mdp)) {
            LightDialog.showError("Mot de passe déjà utilisé", "Ce mot de passe est déjà utilisé par un autre compte. Choisissez-en un autre."); return;
        }

        try {
            User newUser = new User(0, nom, prenom, email, mdp, role, tel);

            if (selectedPhotoPath != null) {
                String ext = selectedPhotoPath.substring(selectedPhotoPath.lastIndexOf('.'));
                String fileName = "user_" + System.currentTimeMillis() + ext;
                Path dest = Paths.get(PHOTOS_DIR, fileName);
                Files.copy(Paths.get(selectedPhotoPath), dest, StandardCopyOption.REPLACE_EXISTING);
                newUser.setPhoto(dest.toString());
            }

            us.signUp(newUser);
            LightDialog.showSuccess("Compte créé !", "Bienvenue sur EchoCare. Connectez-vous !");
            goToLogin(null);

        } catch (SQLException e) {
            LightDialog.showError("Erreur base de données", e.getMessage());
        } catch (IOException e) {
            LightDialog.showError("Erreur photo", "Impossible de sauvegarder la photo.");
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
}