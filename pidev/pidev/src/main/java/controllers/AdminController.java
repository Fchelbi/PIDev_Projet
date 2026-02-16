package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnUsers;
    @FXML private Button btnJournal;
    @FXML private Button btnConsultation;
    @FXML private Button btnFormations;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showFormationView();
    }

    @FXML
    public void showFormationView() {
        loadPage("/FormationView.fxml");
        setActiveButton(btnFormations);
    }

    @FXML
    private void showUsers() {
        showComingSoon("Gestion Utilisateurs");
        setActiveButton(btnUsers);
    }

    @FXML
    private void showJournal() {
        showComingSoon("Gestion Journal");
        setActiveButton(btnJournal);
    }

    @FXML
    private void showConsultation() {
        showComingSoon("Gestion Consultation");
        setActiveButton(btnConsultation);
    }

    @FXML
    private void switchToPatient() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/PatientDashboard.fxml"));
            btnFormations.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Optional<ButtonType> result = showConfirm("Voulez-vous vous déconnecter?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.exit(0);
        }
    }

    public void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error: " + e.getMessage());
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] all = {btnUsers, btnJournal, btnConsultation, btnFormations};
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("sidebar-btn-active");
        }
        if (activeBtn != null) activeBtn.getStyleClass().add("sidebar-btn-active");
    }

    private void showComingSoon(String feature) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Bientôt");
        a.setHeaderText(null);
        a.setContentText(feature + " — Bientôt disponible!");
        a.showAndWait();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
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