package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminHome {

    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblNbPatients;
    @FXML
    private Label lblNbCoaches;
    @FXML
    private Label lblNbAdmins;
    @FXML
    private Label lblNbTotal;

    @FXML
    private StackPane contentArea;
    @FXML
    private VBox dashboardPane;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnUtilisateurs;

    private User currentUser;
    private final serviceUser us = new serviceUser();

    /**
     * Méthode appelée par SignIn pour passer l'utilisateur connecté
     */
    public void setUser(User user) {
        this.currentUser = user;
        lblWelcome.setText("Bienvenue " + user.getPrenom() + " " + user.getNom());

        // Charger les statistiques
        refreshStats(null);
    }

    @FXML
    void initialize() {
        System.out.println("✅ AdminHome initialized");
    }

    /**
     * Actualiser les statistiques
     */
    @FXML
    void refreshStats(ActionEvent event) {
        try {
            List<User> users = us.selectALL();

            int nbPatients = 0;
            int nbCoaches = 0;
            int nbAdmins = 0;

            for (User u : users) {
                String role = u.getRole().toUpperCase();
                switch (role) {
                    case "PATIENT":
                        nbPatients++;
                        break;
                    case "COACH":
                        nbCoaches++;
                        break;
                    case "ADMIN":
                        nbAdmins++;
                        break;
                }
            }

            lblNbPatients.setText(String.valueOf(nbPatients));
            lblNbCoaches.setText(String.valueOf(nbCoaches));
            lblNbAdmins.setText(String.valueOf(nbAdmins));
            lblNbTotal.setText(String.valueOf(users.size()));

            System.out.println("📊 Stats: " + users.size() + " users");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les statistiques.");
            e.printStackTrace();
        }
    }

    /**
     * Afficher le Dashboard
     */
    @FXML
    void showDashboard(ActionEvent event) {
        System.out.println("📊 Navigation → Dashboard");

        // Highlight active button
        highlightButton(btnDashboard);

        // Show dashboard pane
        dashboardPane.setVisible(true);
        dashboardPane.setManaged(true);

        // Refresh stats
        refreshStats(null);
    }

    /**
     * Afficher la page Gestion Utilisateurs
     */
    @FXML
    void showUtilisateurs(ActionEvent event) {
        System.out.println("👥 Navigation → Gestion Utilisateurs");

        try {
            // Highlight active button
            highlightButton(btnUtilisateurs);

            // Load GestionUtilisateurs page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionUtilisateurs.fxml"));
            VBox gestionPage = loader.load();

            // Get controller and pass current user
            Gestionutilisateurs controller = loader.getController();
            controller.setCurrentUser(currentUser);

            // Replace content
            contentArea.getChildren().clear();
            contentArea.getChildren().add(gestionPage);

            System.out.println("✅ Page Gestion Utilisateurs chargée");

        } catch (IOException e) {
            System.err.println("❌ Erreur chargement page Utilisateurs:");
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la page Gestion Utilisateurs");
        }
    }

    /**
     * Highlight le bouton actif du menu
     */
    private void highlightButton(Button activeButton) {
        // Reset all buttons
        btnDashboard.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 12; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-cursor: hand;");
        btnUtilisateurs.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 12; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-cursor: hand;");

        // Highlight active button
        activeButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 12; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-cursor: hand;");
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
                // Retour au Login
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