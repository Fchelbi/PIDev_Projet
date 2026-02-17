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
    @FXML private PasswordField pfMdp;
    @FXML private ComboBox<String> cbRole;
    @FXML private ImageView imgSignUpPhoto;
    @FXML private Label lblPhotoPlaceholder;
    @FXML private StackPane photoContainer;

    private final serviceUser us = new serviceUser();
    private String selectedPhotoPath = null;
    private static final String PHOTOS_DIR = "user_photos";

    private static final Pattern EMAIL_P = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_P = Pattern.compile("^[0-9]{8}$");
    private static final Pattern NAME_P = Pattern.compile("^[a-zA-ZÀ-ÿ\\s'-]+$");

    private static final String S_N = "-fx-background-color: #F7FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; -fx-padding: 13; -fx-font-size: 14px;";
    private static final String S_E = "-fx-background-color: #FFF5F5; -fx-border-color: #E57373; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; -fx-padding: 13; -fx-font-size: 14px;";
    private static final String S_S = "-fx-background-color: #F0FFF4; -fx-border-color: #81C995; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; -fx-padding: 13; -fx-font-size: 14px;";

    @FXML
    void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("PATIENT", "COACH", "ADMIN"));
        cbRole.setValue("PATIENT");

        // Créer dossier photos
        File dir = new File(PHOTOS_DIR);
        if (!dir.exists()) dir.mkdirs();

        setupValidation();
    }

    private void setupValidation() {
        tfNom.textProperty().addListener((o, ov, nv) -> tfNom.setStyle(nv.isEmpty() ? S_N : NAME_P.matcher(nv).matches() && nv.length() >= 2 ? S_S : S_E));
        tfPrenom.textProperty().addListener((o, ov, nv) -> tfPrenom.setStyle(nv.isEmpty() ? S_N : NAME_P.matcher(nv).matches() && nv.length() >= 2 ? S_S : S_E));
        tfEmail.textProperty().addListener((o, ov, nv) -> tfEmail.setStyle(nv.isEmpty() ? S_N : EMAIL_P.matcher(nv).matches() ? S_S : S_E));
        pfMdp.textProperty().addListener((o, ov, nv) -> pfMdp.setStyle(nv.isEmpty() ? S_N : nv.length() >= 6 ? S_S : S_E));
        tfTel.textProperty().addListener((o, ov, nv) -> tfTel.setStyle(nv.isEmpty() ? S_N : PHONE_P.matcher(nv).matches() ? S_S : S_E));
    }

    /**
     * Choisir photo de profil
     */
    @FXML
    void handleChoosePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fc.showOpenDialog(tfNom.getScene().getWindow());
        if (file != null) {
            try {
                // Fichier temporaire, sera copié après inscription
                selectedPhotoPath = file.getAbsolutePath();

                // Preview
                Image img = new Image(file.toURI().toString(), 100, 100, false, true);
                imgSignUpPhoto.setImage(img);
                imgSignUpPhoto.setVisible(true);
                lblPhotoPlaceholder.setVisible(false);

                System.out.println("📷 Photo sélectionnée: " + selectedPhotoPath);
            } catch (Exception e) {
                LightDialog.showError("Erreur", "Impossible de charger la photo.");
            }
        }
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        String nom = tfNom.getText().trim(), prenom = tfPrenom.getText().trim(),
                email = tfEmail.getText().trim(), mdp = pfMdp.getText(),
                tel = tfTel.getText().trim(), role = cbRole.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            LightDialog.showError("Erreur", "Remplissez les champs obligatoires!"); return;
        }
        if (!NAME_P.matcher(nom).matches() || nom.length() < 2) {
            LightDialog.showError("Nom invalide", "Min 2 lettres!"); return;
        }
        if (!NAME_P.matcher(prenom).matches() || prenom.length() < 2) {
            LightDialog.showError("Prénom invalide", "Min 2 lettres!"); return;
        }
        if (!EMAIL_P.matcher(email).matches()) {
            LightDialog.showError("Email invalide", "Email non valide!"); return;
        }
        if (mdp.length() < 6) {
            LightDialog.showError("Mot de passe", "Min 6 caractères!"); return;
        }
        if (!tel.isEmpty() && !PHONE_P.matcher(tel).matches()) {
            LightDialog.showError("Téléphone", "8 chiffres!"); return;
        }

        try {
            User newUser = new User(0, nom, prenom, email, mdp, role, tel);

            // Copier la photo si sélectionnée
            if (selectedPhotoPath != null) {
                String ext = selectedPhotoPath.substring(selectedPhotoPath.lastIndexOf('.'));
                String fileName = "user_" + System.currentTimeMillis() + ext;
                Path dest = Paths.get(PHOTOS_DIR, fileName);
                Files.copy(Paths.get(selectedPhotoPath), dest, StandardCopyOption.REPLACE_EXISTING);
                newUser.setPhoto(dest.toString());
            }

            us.signUp(newUser);
            LightDialog.showSuccess("Succès", "Compte créé avec succès!");
            switchToLogin(null);

        } catch (SQLException e) {
            LightDialog.showError("Erreur", e.getMessage());
        } catch (IOException e) {
            LightDialog.showError("Erreur", "Problème avec la photo.");
            e.printStackTrace();
        }
    }

    @FXML
    void switchToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            ((Stage) tfNom.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
}