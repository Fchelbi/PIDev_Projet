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
import utils.LightDialog;
import java.io.File;
import java.io.IOException;

public class PatientHome {
    @FXML private Label lblWelcome, lblNbRdv, lblNbSeances;
    @FXML private Label lblNom, lblEmail, lblTel, lblAvatarHeader;
    @FXML private StackPane contentArea;
    @FXML private VBox accueilPane;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private Circle headerAvatarCircle;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        lblWelcome.setText(user.getPrenom() + " " + user.getNom());
        lblAvatarHeader.setText(user.getPrenom().substring(0, 1).toUpperCase());
        lblNom.setText(user.getPrenom() + " " + user.getNom());
        lblEmail.setText(user.getEmail());
        lblTel.setText(user.getNum_tel() != null && !user.getNum_tel().isEmpty() ? user.getNum_tel() : "Non renseigné");
        lblNbRdv.setText("0");
        lblNbSeances.setText("0");
        loadHeaderPhoto();
    }

    private void loadHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(f.toURI().toString(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblAvatarHeader.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblAvatarHeader.setVisible(true);
    }

    @FXML void initialize() { System.out.println("✅ PatientHome initialized"); }

    @FXML void showAccueil(ActionEvent event) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(accueilPane);
    }

    @FXML void showProfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            ((Profil) loader.getController()).setCurrentUser(currentUser);
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
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}