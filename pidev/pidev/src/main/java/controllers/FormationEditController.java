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

public class FormationEditController implements Initializable {

    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private TextField tfVideoUrl;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblError;

    private FormationService formationService;
    private Formation currentFormation;
    private FormationListController listController;

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
    }

    // Called from ListController to pass data
    public void setFormation(Formation formation) {
        this.currentFormation = formation;
        tfTitle.setText(formation.getTitle());
        taDescription.setText(formation.getDescription());
        tfVideoUrl.setText(formation.getVideoUrl());
        cbCategory.setValue(formation.getCategory());
    }

    public void setListController(FormationListController controller) {
        this.listController = controller;
    }

    public void setAdminController(AdminController controller) {
        // Optional: for navigation
    }

    // ═══════════════════════════════════════
    //  UPDATE
    // ═══════════════════════════════════════
    @FXML
    private void handleUpdate() {
        lblError.setText("");

        if (!validateFields()) return;

        currentFormation.setTitle(tfTitle.getText().trim());
        currentFormation.setDescription(taDescription.getText().trim());
        currentFormation.setVideoUrl(tfVideoUrl.getText().trim());
        currentFormation.setCategory(cbCategory.getValue());

        try {
            formationService.updateOne(currentFormation);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Formation updated successfully! ✅");
            alert.showAndWait();

            // Go back to list
            handleCancel();

        } catch (SQLException e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════
    //  CANCEL → Back to list
    // ═══════════════════════════════════════
    @FXML
    private void handleCancel() {
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