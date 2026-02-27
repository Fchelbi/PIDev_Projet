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

public class CoachHome {

    @FXML private Label lblWelcome;
    @FXML private Label lblAvatarHeader;
    @FXML private Label lblNbPatients;
    @FXML private Label lblNbSeances;
    @FXML private Label lblNom;
    @FXML private Label lblEmail;
    @FXML private Label lblTel;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane accueilPane;

    // Sidebar nav
    @FXML private VBox navAccueil;
    @FXML private VBox navProfil;
    @FXML private VBox navRapport;
    @FXML private HBox indicAccueil;
    @FXML private HBox indicProfil;
    @FXML private HBox indicRapport;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private VBox currentActiveNav;

    private static final String NAV_NORMAL  =
            "-fx-padding: 12 20 12 16; -fx-cursor: hand; -fx-background-color: transparent; -fx-background-radius: 0 10 10 0;";
    private static final String NAV_ACTIVE  =
            "-fx-padding: 12 20 12 16; -fx-cursor: hand; -fx-background-color: rgba(255,255,255,0.13); -fx-background-radius: 0 10 10 0;";
    private static final String INDIC_HIDDEN  =
            "-fx-min-width: 4; -fx-max-width: 4; -fx-min-height: 30; -fx-background-color: transparent; -fx-background-radius: 0 2 2 0;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width: 4; -fx-max-width: 4; -fx-min-height: 30; -fx-background-color: #E8956D; -fx-background-radius: 0 2 2 0;";

    @FXML
    void initialize() { System.out.println("✅ CoachHome initialized"); }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        setActiveNav(navAccueil, indicAccueil);
    }

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) { System.err.println("Refresh: " + e.getMessage()); }

        lblWelcome.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
        lblAvatarHeader.setText(currentUser.getPrenom().substring(0,1).toUpperCase());
        if (lblNom   != null) lblNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (lblEmail != null) lblEmail.setText(currentUser.getEmail());
        if (lblTel   != null) lblTel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "—");
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

    @FXML
    void showAccueil(MouseEvent event) {
        setActiveNav(navAccueil, indicAccueil);
        contentArea.getChildren().setAll(accueilPane);
    }

    @FXML
    void showProfil(MouseEvent event) {
        try {
            setActiveNav(navProfil, indicProfil);
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

    private void setActiveNav(VBox nav, HBox indic) {
        for (VBox n : new VBox[]{navAccueil, navProfil, navRapport})
            if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i : new HBox[]{indicAccueil, indicProfil, indicRapport})
            if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    @FXML void onNavAccueilEnter(MouseEvent e) { if(navAccueil!=currentActiveNav) navAccueil.setStyle(NAV_ACTIVE); }
    @FXML void onNavAccueilExit(MouseEvent e)  { if(navAccueil!=currentActiveNav) navAccueil.setStyle(NAV_NORMAL); }
    @FXML void onNavProfilEnter(MouseEvent e)  { if(navProfil !=currentActiveNav) navProfil.setStyle(NAV_ACTIVE); }
    @FXML void onNavProfilExit(MouseEvent e)   { if(navProfil !=currentActiveNav) navProfil.setStyle(NAV_NORMAL); }

    @FXML void onNavRapportEnter(MouseEvent e) { if(navRapport!=currentActiveNav) navRapport.setStyle(NAV_ACTIVE); }
    @FXML void onNavRapportExit(MouseEvent e)  { if(navRapport!=currentActiveNav) navRapport.setStyle(NAV_NORMAL); }

    @FXML
    void showRapport(MouseEvent event) {
        try {
            setActiveNav(navRapport, indicRapport);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GenerateRapport.fxml"));
            VBox page = loader.load();
            RapportController ctrl = loader.getController();
            ctrl.setCoach(currentUser);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le rapport.");
        }
    }

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