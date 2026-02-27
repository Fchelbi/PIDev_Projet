package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;
import utils.LightDialog;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class PatientHome {

    @FXML private Label lblWelcome, lblAvatarHeader;
    @FXML private Label lblNbRdv, lblNbSeances;
    @FXML private Label lblNom, lblEmail, lblTel;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane accueilPane;

    // Sidebar — only accueil now
    @FXML private VBox navAccueil;
    @FXML private HBox indicAccueil;

    private User currentUser;
    private final serviceUser us = new serviceUser();

    private static final String NAV_NORMAL =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:transparent;-fx-background-radius:0 10 10 0;";
    private static final String NAV_ACTIVE =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:0 10 10 0;";
    private static final String INDIC_HIDDEN  =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:transparent;-fx-background-radius:0 2 2 0;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:#E8956D;-fx-background-radius:0 2 2 0;";

    private static final String CARD_NORMAL =
            "-fx-background-color:white;-fx-padding:24;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,2);";
    private static final String CARD_HOVER  =
            "-fx-background-color:#F7F5FF;-fx-padding:24;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(74,111,165,0.15),14,0,0,4);";

    @FXML
    void initialize() { System.out.println("✅ PatientHome initialized"); }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        // Set accueil active
        if (navAccueil   != null) navAccueil.setStyle(NAV_ACTIVE);
        if (indicAccueil != null) indicAccueil.setStyle(INDIC_VISIBLE);
        contentArea.getChildren().setAll(accueilPane);
    }

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) { System.err.println("Refresh: " + e.getMessage()); }

        lblWelcome.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
        lblAvatarHeader.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        if (lblNom    != null) lblNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (lblEmail  != null) lblEmail.setText(currentUser.getEmail());
        if (lblTel    != null) lblTel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "—");
        if (lblNbRdv     != null) lblNbRdv.setText("0");
        if (lblNbSeances != null) lblNbSeances.setText("0");
        updateHeaderPhoto();
    }

    private void updateHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(f.toURI() + "?t=" + System.currentTimeMillis(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblAvatarHeader.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblAvatarHeader.setVisible(true);
    }

    // ─── Accueil nav ─────────────────────────────────────────
    @FXML
    void showAccueil(MouseEvent event) {
        if (navAccueil   != null) navAccueil.setStyle(NAV_ACTIVE);
        if (indicAccueil != null) indicAccueil.setStyle(INDIC_VISIBLE);
        contentArea.getChildren().setAll(accueilPane);
    }

    @FXML void onNavAccueilEnter(MouseEvent e) { if (navAccueil != null) navAccueil.setStyle(NAV_ACTIVE); }
    @FXML void onNavAccueilExit(MouseEvent e)  {
        // Keep active style if on accueil - just leave it
    }

    // ─── Profil (from quick-access card in content) ──────────
    @FXML
    void showProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            Profil ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setOnPhotoChanged(this::refreshUserData);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le profil.");
        }
    }

    // Card hover effects
    @FXML void onCardProfilEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardProfilExit(MouseEvent e)  { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }

    // ─── Logout ───────────────────────────────────────────────
    @FXML
    void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Voulez-vous vraiment quitter ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}