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
    @FXML private Button btnFormations;
    @FXML private Button btnAddFormation;
    @FXML private Button btnQuiz;
    @FXML private Button btnQuestions;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Show formation list by default
        showFormationView();
    }

    // ═══════════════════════════════════
    //  SIDEBAR NAVIGATION
    // ═══════════════════════════════════
    @FXML
    public void showFormationView() {
        loadPage("/FormationView.fxml");
        setActiveButton(btnFormations);
    }

    @FXML
    public void showFormationAdd() {
        loadPage("/FormationAdd.fxml");
        setActiveButton(btnAddFormation);
    }

    @FXML
    private void showQuiz() {
        showComingSoon("Quiz Management");
    }

    @FXML
    private void showQuestions() {
        showComingSoon("Question Management");
    }

    @FXML
    private void handleLogout() {
        Optional<ButtonType> result = showConfirm(
                "Are you sure you want to logout?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.exit(0);
        }
    }

    // ═══════════════════════════════════
    //  LOAD PAGE INTO CENTER
    // ═══════════════════════════════════
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

    // ═══════════════════════════════════
    //  HIGHLIGHT ACTIVE BUTTON
    // ═══════════════════════════════════
    private void setActiveButton(Button activeBtn) {
        btnFormations.getStyleClass().remove("sidebar-btn-active");
        btnAddFormation.getStyleClass().remove("sidebar-btn-active");
        btnQuiz.getStyleClass().remove("sidebar-btn-active");
        btnQuestions.getStyleClass().remove("sidebar-btn-active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("sidebar-btn-active");
        }
    }

    // ═══════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════
    private void showComingSoon(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText(feature + " coming soon!");
        alert.showAndWait();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        return alert.showAndWait();
    }
}