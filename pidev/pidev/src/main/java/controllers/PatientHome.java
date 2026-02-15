package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import entities.User;

import java.io.IOException;
import java.util.Optional;

public class PatientHome {

    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblNbRdv;
    @FXML
    private Label lblNbSeances;
    @FXML
    private Label lblNom;
    @FXML
    private Label lblEmail;
    @FXML
    private Label lblTel;

    private User currentUser;

    /**
     * Méthode appelée par SignIn pour passer l'utilisateur connecté
     */
    public void setUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Bienvenue " + user.getPrenom() + " " + user.getNom());

        // Afficher les infos du patient
        lblNom.setText(user.getNom() + " " + user.getPrenom());
        lblEmail.setText(user.getEmail());
        lblTel.setText(user.getNum_tel() != null && !user.getNum_tel().isEmpty()
                ? user.getNum_tel()
                : "Non renseigné");

        // TODO: Charger les rendez-vous et séances depuis la BD
        lblNbRdv.setText("0");
        lblNbSeances.setText("0");

        System.out.println("🏥 Patient connecté: " + user.getPrenom());
    }

    @FXML
    void initialize() {
        // Initialisation si nécessaire
    }

    /**
     * Afficher le profil du patient
     */
    @FXML
    void showProfile(ActionEvent event) {
        if (currentUser != null) {
            String info = "👤 PROFIL PATIENT\n\n" +
                    "Nom: " + currentUser.getNom() + "\n" +
                    "Prénom: " + currentUser.getPrenom() + "\n" +
                    "Email: " + currentUser.getEmail() + "\n" +
                    "Téléphone: " + (currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "Non renseigné") + "\n" +
                    "Rôle: " + currentUser.getRole();

            showAlert(Alert.AlertType.INFORMATION, "Mon Profil", info);
        }
    }

    /**
     * Déconnexion
     */
    @FXML
    void handleLogout(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Déconnexion");
        confirmAlert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Connexion");

                System.out.println("👋 Déconnexion de " + currentUser.getPrenom());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}