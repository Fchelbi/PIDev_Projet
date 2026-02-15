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
    private void handleLogout() {
        Optional<ButtonType> result = showConfirm(
                "Êtes-vous sûr de vouloir vous déconnecter?");
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
            showAlert("Error loading page: " + e.getMessage());
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] allButtons = {btnUsers, btnJournal, btnConsultation, btnFormations};
        for (Button btn : allButtons) {
            if (btn != null) btn.getStyleClass().remove("sidebar-btn-active");
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bientôt");
        alert.setHeaderText(null);
        alert.setContentText(feature + " — Votre coéquipier va l'implémenter!");
        alert.showAndWait();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        return alert.showAndWait();
    }
}