package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    // --- Sidebar Navigation Components ---
    @FXML private VBox navFormations;
    @FXML private HBox indicFormations;
    @FXML private HBox indicUtilisateurs; // Added this to fix red error
    @FXML private HBox indicAccueil;      // Added this to fix red error

    // --- Main Dashboard Components ---
    @FXML private StackPane contentArea;
    @FXML private Button btnUsers;
    @FXML private Button btnJournal;
    @FXML private Button btnConsultation;
    @FXML private Button btnFormations;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Loads the formation view as the default starting page
        showFormationView();
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION LOGIC
    // ══════════════════════════════════════════════════════

    @FXML
    public void showFormationView() {
        loadPage("/FormationView.fxml");
        setActiveButton(btnFormations);
    }

    @FXML private void showUsers() {
        showComingSoon("Gestion Utilisateurs");
        setActiveButton(btnUsers);
    }

    @FXML private void showJournal() {
        showComingSoon("Gestion Journal");
        setActiveButton(btnJournal);
    }

    @FXML private void showConsultation() {
        showComingSoon("Gestion Consultation");
        setActiveButton(btnConsultation);
    }

    // ══════════════════════════════════════════════════════
    //  FORMATION BUTTON EVENTS (Hover & Click)
    // ══════════════════════════════════════════════════════

    @FXML
    private void showFormations(MouseEvent event) {
        try {
            Parent fxml = FXMLLoader.load(getClass().getResource("/FormationView.fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(fxml);

            resetIndicators();
            if (indicFormations != null) {
                indicFormations.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 0 2 2 0;");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavFormationsEnter(MouseEvent event) {
        navFormations.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-cursor: hand; -fx-background-radius: 0 10 10 0;");
    }

    @FXML
    private void onNavFormationsExit(MouseEvent event) {
        navFormations.setStyle("-fx-background-color: transparent; -fx-background-radius: 0 10 10 0;");
    }

    // ══════════════════════════════════════════════════════
    //  DASHBOARD UTILS
    // ══════════════════════════════════════════════════════

    public void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            System.err.println("Error loading FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void resetIndicators() {
        if (indicUtilisateurs != null) indicUtilisateurs.setStyle("-fx-background-color: transparent;");
        if (indicAccueil != null) indicAccueil.setStyle("-fx-background-color: transparent;");
        if (indicFormations != null) indicFormations.setStyle("-fx-background-color: transparent;");
    }

    private void setActiveButton(Button active) {
        Button[] all = {btnUsers, btnJournal, btnConsultation, btnFormations};
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("sidebar-btn-active");
        }
        if (active != null) active.getStyleClass().add("sidebar-btn-active");
    }

    // ══════════════════════════════════════════════════════
    //  SWITCHING ROLES & LOGOUT
    // ══════════════════════════════════════════════════════

    @FXML
    private void switchToPatient() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/PatientDashboard.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void switchToCoach() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/CoachDashboard.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        Optional<ButtonType> r = showConfirm("Voulez-vous vous déconnecter?");
        if (r.isPresent() && r.get() == ButtonType.OK) System.exit(0);
    }

    // ══════════════════════════════════════════════════════
    //  ALERTS
    // ══════════════════════════════════════════════════════

    private void showComingSoon(String f) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Bientôt");
        a.setHeaderText(null);
        a.setContentText(f + " — Bientôt!");
        a.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation");
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait();
    }
}