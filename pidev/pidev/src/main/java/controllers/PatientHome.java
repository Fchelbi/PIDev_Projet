package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;
import utils.LightDialog;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class PatientHome {
    @FXML private Label lblWelcome, lblNbRdv, lblNbSeances;
    @FXML private Label lblNom, lblEmail, lblTel, lblAvatarHeader;
    @FXML private StackPane contentArea;
    @FXML private VBox accueilPane;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private Circle headerAvatarCircle;

    private User currentUser;
    private final serviceUser us = new serviceUser();

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
    }

    /**
     * ✅ REFRESH USER DATA
     */
    public void refreshUserData() {
        try {
            User updatedUser = us.getUserById(currentUser.getId_user());
            if (updatedUser != null) {
                this.currentUser = updatedUser;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur refresh: " + e.getMessage());
        }

        lblWelcome.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        lblAvatarHeader.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        lblNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        lblEmail.setText(currentUser.getEmail());
        lblTel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "Non renseigné");
        lblNbRdv.setText("0");
        lblNbSeances.setText("0");
        updateHeaderPhoto();
    }

    /**
     * ✅ UPDATE PHOTO
     */
    private void updateHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                String url = f.toURI().toString() + "?t=" + System.currentTimeMillis();
                imgHeaderPhoto.setImage(new Image(url, 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblAvatarHeader.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblAvatarHeader.setVisible(true);
    }

    @FXML void initialize() {
        System.out.println("✅ PatientHome initialized");
    }

    @FXML void showAccueil(ActionEvent event) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(accueilPane);
    }

    @FXML void showProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            Profil profilController = loader.getController();
            profilController.setCurrentUser(currentUser);

            // ✅ CALLBACK
            profilController.setOnPhotoChanged(() -> {
                refreshUserData();
            });

            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le profil.");
        }
    }

    @FXML void handleLogout(ActionEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Êtes-vous sûr ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                // ✅ FIX: utilise contentArea (toujours visible) au lieu de lblWelcome
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("EchoCare - Connexion");
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
    @FXML void handleLogout(javafx.scene.input.MouseEvent e) { handleLogout((ActionEvent)null); }
    @FXML void showMap(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Map.fxml"));
            VBox page = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger la carte.");
        }
    }
    @FXML void showMap(javafx.scene.input.MouseEvent e) { showMap((ActionEvent)null); }

    // ✅ FEATURE 1: Journal de bien-être / Mood Tracker
    @FXML void showMoodTracker(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MoodTracker.fxml"));
            VBox page = loader.load();
            MoodTrackerController ctrl = loader.getController();
            ctrl.setUser(currentUser);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le journal.");
        }
    }
    @FXML void showMoodTracker(javafx.scene.input.MouseEvent e) { showMoodTracker((ActionEvent)null); }
}