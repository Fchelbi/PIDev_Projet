package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
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

public class Profil {

    @FXML private TextField tfNom, tfPrenom, tfEmail, tfTel, tfRole;
    @FXML private PasswordField pfNewPassword, pfConfirmPassword;
    @FXML private Label lblAvatar, lblFullName, lblRoleBadge;
    @FXML private Label lblEmailDisplay, lblTelDisplay, lblRoleDisplay, lblSubtitle;
    @FXML private Label lblSpecialTitle, lblSpecialInfo;
    @FXML private Button btnEdit, btnChangePhoto;
    @FXML private HBox editButtons;
    @FXML private Circle avatarCircle;
    @FXML private VBox roleSpecialSection;
    @FXML private ImageView imgPhoto;
    @FXML private StackPane avatarContainer, cameraOverlay;

    private User currentUser;
    private boolean isEditMode = false;
    private final serviceUser us = new serviceUser();
    private String selectedPhotoPath = null;
    private String EDITABLE_STYLE;
    private Runnable onPhotoChanged;

    private final String READONLY_STYLE =
            "-fx-background-color: #F7FAFC; -fx-border-color: #E2E8F0; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; " +
                    "-fx-padding: 12; -fx-font-size: 14px;";

    // Dossier pour sauvegarder les photos
    private static final String PHOTOS_DIR = "user_photos";

    @FXML
    void initialize() {
        // Créer le dossier photos s'il n'existe pas
        File photosDir = new File(PHOTOS_DIR);
        if (!photosDir.exists()) {
            photosDir.mkdirs();
        }

        // Hover effect sur l'avatar en mode edit
        avatarContainer.setOnMouseEntered(e -> {
            if (isEditMode) cameraOverlay.setVisible(true);
        });
        avatarContainer.setOnMouseExited(e -> {
            cameraOverlay.setVisible(false);
        });
        avatarContainer.setOnMouseClicked(e -> {
            if (isEditMode) handleChangePhoto();
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        applyRoleTheme();
        loadUserData();
        loadPhoto();
    }

    /**
     * Charger la photo de profil
     */
    private void loadPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            try {
                File photoFile = new File(currentUser.getPhoto());
                if (photoFile.exists()) {
                    Image image = new Image(photoFile.toURI().toString(), 130, 130, false, true);
                    imgPhoto.setImage(image);
                    imgPhoto.setVisible(true);
                    lblAvatar.setVisible(false);
                    System.out.println("📷 Photo chargée: " + currentUser.getPhoto());
                    return;
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur chargement photo: " + e.getMessage());
            }
        }

        // Pas de photo → afficher initiale
        imgPhoto.setImage(null);
        imgPhoto.setVisible(false);
        lblAvatar.setVisible(true);
    }

    /**
     * Changer la photo de profil
     */
    @FXML
    void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) tfNom.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Copier le fichier dans le dossier photos
                String fileName = "user_" + currentUser.getId_user() + "_" +
                        System.currentTimeMillis() + getExtension(selectedFile.getName());
                Path destination = Paths.get(PHOTOS_DIR, fileName);

                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                selectedPhotoPath = destination.toString();

                // Afficher preview
                Image image = new Image(selectedFile.toURI().toString(), 130, 130, false, true);
                imgPhoto.setImage(image);
                imgPhoto.setVisible(true);
                lblAvatar.setVisible(false);

                System.out.println("📷 Photo sélectionnée: " + selectedPhotoPath);

            } catch (IOException e) {
                LightDialog.showError("Erreur", "Impossible de charger la photo.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Obtenir l'extension du fichier
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : ".png";
    }

    private void applyRoleTheme() {
        if (currentUser == null) return;
        String role = currentUser.getRole().toUpperCase();
        String c1, c2, badge, bg, sTitle = "", sInfo = "";

        switch (role) {
            case "COACH":
                c1 = "#81C995"; c2 = "#A7D8B0"; badge = "#5FAD6F"; bg = "#E8F5E9";
                sTitle = "💪 Espace Coach"; sInfo = "Gérez vos patients et séances.";
                lblSubtitle.setText("Gérez votre profil coach"); break;
            case "PATIENT":
                c1 = "#E0A7B5"; c2 = "#D4C5A5"; badge = "#D4889A"; bg = "#FDF2F8";
                sTitle = "🏥 Mon suivi"; sInfo = "Consultez vos rendez-vous.";
                lblSubtitle.setText("Gérez votre profil patient"); break;
            default:
                c1 = "#A7B5E0"; c2 = "#D4A5BD"; badge = "#9FAEE0"; bg = "#EDF0FA";
                sTitle = "👑 Privilèges Admin"; sInfo = "Gestion complète du système.";
                lblSubtitle.setText("Gérez votre compte administrateur"); break;
        }

        avatarCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(c1)), new Stop(1, Color.web(c2))));

        lblRoleBadge.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + badge +
                "; -fx-background-color: " + bg + "; -fx-padding: 5 18; -fx-background-radius: 20;");

        btnEdit.setStyle("-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 +
                "); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600; " +
                "-fx-padding: 12 25; -fx-background-radius: 10; -fx-cursor: hand;");

        EDITABLE_STYLE = "-fx-background-color: white; -fx-border-color: " + c1 +
                "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1.5; " +
                "-fx-padding: 12; -fx-font-size: 14px;";

        if (!sTitle.isEmpty()) {
            roleSpecialSection.setVisible(true);
            roleSpecialSection.setManaged(true);
            roleSpecialSection.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12; -fx-padding: 15;");
            lblSpecialTitle.setText(sTitle);
            lblSpecialTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + badge + ";");
            lblSpecialInfo.setText(sInfo);
        }
    }

    private void loadUserData() {
        if (currentUser == null) return;
        tfNom.setText(currentUser.getNom());
        tfPrenom.setText(currentUser.getPrenom());
        tfEmail.setText(currentUser.getEmail());
        tfTel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "");
        tfRole.setText(currentUser.getRole());

        lblAvatar.setText(currentUser.getPrenom().isEmpty() ? "?" :
                currentUser.getPrenom().substring(0, 1).toUpperCase());
        lblFullName.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        lblRoleBadge.setText(currentUser.getRole().toUpperCase());
        lblEmailDisplay.setText(currentUser.getEmail());
        lblTelDisplay.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "Non renseigné");
        lblRoleDisplay.setText(currentUser.getRole());
    }

    @FXML
    void toggleEditMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            enableEditing(true);
            btnEdit.setText("🔒 Verrouiller");
            btnEdit.setStyle("-fx-background-color: #F0F3F8; -fx-text-fill: #718096; " +
                    "-fx-font-size: 14px; -fx-font-weight: 600; -fx-padding: 12 25; " +
                    "-fx-background-radius: 10; -fx-cursor: hand;");
            editButtons.setVisible(true);
            editButtons.setManaged(true);
            btnChangePhoto.setVisible(true);
            btnChangePhoto.setManaged(true);
        } else {
            cancelEdit();
        }
    }

    @FXML
    void cancelEdit() {
        isEditMode = false;
        enableEditing(false);
        loadUserData();
        loadPhoto();
        selectedPhotoPath = null;
        pfNewPassword.clear();
        pfConfirmPassword.clear();
        applyRoleTheme();
        btnEdit.setText("✏️ Modifier");
        editButtons.setVisible(false);
        editButtons.setManaged(false);
        btnChangePhoto.setVisible(false);
        btnChangePhoto.setManaged(false);
    }

    @FXML
    void saveProfile() {
        String nom = tfNom.getText().trim(), prenom = tfPrenom.getText().trim(),
                email = tfEmail.getText().trim(), tel = tfTel.getText().trim(),
                np = pfNewPassword.getText(), cp = pfConfirmPassword.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            LightDialog.showError("Champs requis", "Nom, prénom et email obligatoires.");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            LightDialog.showError("Email invalide", "Email non valide.");
            return;
        }
        // Unicité email (hors utilisateur actuel)
        if (us.emailExistsForOther(email, currentUser.getId_user())) {
            LightDialog.showError("Email déjà utilisé", "Cet email est déjà utilisé par un autre compte.");
            return;
        }
        if (!tel.isEmpty() && !tel.matches("\\d{8}")) {
            LightDialog.showError("Téléphone", "8 chiffres requis.");
            return;
        }
        if (!np.isEmpty()) {
            if (np.length() < 6) {
                LightDialog.showError("Mot de passe", "Min 6 caractères.");
                return;
            }
            if (!np.equals(cp)) {
                LightDialog.showError("Mot de passe", "Ne correspondent pas.");
                return;
            }
            // Unicité mot de passe (hors utilisateur actuel)
            if (us.passwordExistsForOther(np, currentUser.getId_user())) {
                LightDialog.showError("Mot de passe déjà utilisé", "Ce mot de passe est déjà utilisé par un autre compte.");
                return;
            }
        }
        if (!LightDialog.showConfirmation("Sauvegarder", "Voulez-vous sauvegarder ?", "💾"))
            return;

        try {
            currentUser.setNom(nom);
            currentUser.setPrenom(prenom);
            currentUser.setEmail(email);
            currentUser.setNum_tel(tel);
            if (!np.isEmpty()) currentUser.setMdp(np);

            // ✅ Sauvegarder la photo
            if (selectedPhotoPath != null) {
                // Supprimer l'ancienne photo si existe
                if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
                    File oldPhoto = new File(currentUser.getPhoto());
                    if (oldPhoto.exists()) oldPhoto.delete();
                }
                currentUser.setPhoto(selectedPhotoPath);
            }


            us.updateOne(currentUser);
            selectedPhotoPath = null;
            loadUserData();
            loadPhoto();
            cancelEdit();
            LightDialog.showSuccess("Succès", "Profil mis à jour !");

            if (onPhotoChanged != null) {
                onPhotoChanged.run();
            }
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de sauvegarder.");
            e.printStackTrace();
        }
    }

    @FXML
    void handleLogout() {
        if (LightDialog.showConfirmation("Déconnexion", "Êtes-vous sûr ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) tfNom.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableEditing(boolean ed) {
        tfNom.setEditable(ed);
        tfPrenom.setEditable(ed);
        tfEmail.setEditable(ed);
        tfTel.setEditable(ed);
        pfNewPassword.setEditable(ed);
        pfConfirmPassword.setEditable(ed);
        String s = ed ? EDITABLE_STYLE : READONLY_STYLE;
        tfNom.setStyle(s);
        tfPrenom.setStyle(s);
        tfEmail.setStyle(s);
        tfTel.setStyle(s);
        pfNewPassword.setStyle(s);
        pfConfirmPassword.setStyle(s);
        tfRole.setEditable(false);
    }
    public void setOnPhotoChanged(Runnable callback) {
        this.onPhotoChanged = callback;
    }
}