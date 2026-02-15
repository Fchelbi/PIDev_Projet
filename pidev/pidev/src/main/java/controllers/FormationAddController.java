package controllers;

import entities.Formation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import services.FormationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FormationAddController implements Initializable {

    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private TextField tfVideoUrl;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblError;

    private FormationService formationService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();

        cbCategory.setItems(FXCollections.observableArrayList(
                "Communication",
                "Leadership",
                "Teamwork",
                "Self-Improvement",
                "Public Speaking",
                "Conflict Resolution",
                "Emotional Intelligence",
                "Time Management"
        ));

        lblError.setText("");
    }

    // ═══════════════════════════════════════
    //  SAVE
    // ═══════════════════════════════════════
    @FXML
    private void handleSave() {
        lblError.setText("");

        if (!validateFields()) return;

        Formation f = new Formation();
        f.setTitle(tfTitle.getText().trim());
        f.setDescription(taDescription.getText().trim());
        f.setVideoUrl(tfVideoUrl.getText().trim());
        f.setCategory(cbCategory.getValue());

        try {
            formationService.insertOne(f);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Formation added successfully! ✅");
            alert.showAndWait();

            // Go back to list
            handleBack();

        } catch (SQLException e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════
    //  CLEAR
    // ═══════════════════════════════════════
    @FXML
    private void handleClear() {
        tfTitle.clear();
        taDescription.clear();
        tfVideoUrl.clear();
        cbCategory.getSelectionModel().clearSelection();
        lblError.setText("");
    }

    // ═══════════════════════════════════════
    //  BACK TO LIST
    // ═══════════════════════════════════════
    @FXML
    private void handleBack() {
        try {
            Parent page = FXMLLoader.load(
                    getClass().getResource("/FormationList.fxml"));
            StackPane contentArea = (StackPane) tfTitle.getScene()
                    .lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════
    private boolean validateFields() {
        StringBuilder errors = new StringBuilder();

        if (tfTitle.getText().trim().isEmpty())
            errors.append("• Title is required\n");
        if (taDescription.getText().trim().isEmpty())
            errors.append("• Description is required\n");
        if (tfVideoUrl.getText().trim().isEmpty())
            errors.append("• Video URL is required\n");
        if (cbCategory.getValue() == null)
            errors.append("• Category must be selected\n");

        if (errors.length() > 0) {
            lblError.setText(errors.toString());
            return false;
        }
        return true;
    }
}