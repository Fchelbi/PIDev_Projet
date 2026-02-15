package controllers;

import entities.Formation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import services.FormationService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class FormationController implements Initializable {

    // ─── Table (FormationView.fxml) ──────
    @FXML private TableView<Formation> tvFormations;
    @FXML private TableColumn<Formation, String> colTitle;
    @FXML private TableColumn<Formation, String> colDescription;
    @FXML private TableColumn<Formation, String> colVideoUrl;
    @FXML private TableColumn<Formation, String> colCategory;
    @FXML private TableColumn<Formation, Void> colActions;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotal;

    // ─── Form (FormationAdd + FormationEdit) ──
    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private TextField tfVideoUrl;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblError;

    // ─── Data ────────────────────────────
    private FormationService formationService;
    private ObservableList<Formation> formationList;
    private FilteredList<Formation> filteredList;

    // Used for edit mode
    private static Formation formationToEdit;

    private final String[] CATEGORIES = {
            "Communication", "Leadership", "Teamwork",
            "Self-Improvement", "Public Speaking",
            "Conflict Resolution", "Emotional Intelligence",
            "Time Management"
    };

    // ═════════════════════════════════════
    //  INITIALIZE
    // ═════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();

        // If table exists → we are on FormationView
        if (tvFormations != null) {
            initTable();
        }

        // If form exists → we are on Add or Edit
        if (cbCategory != null) {
            initForm();
        }
    }

    // ═════════════════════════════════════
    //  INIT TABLE (FormationView.fxml)
    // ═════════════════════════════════════
    private void initTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colVideoUrl.setCellValueFactory(new PropertyValueFactory<>("videoUrl"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        setupActionsColumn();
        loadFormations();
    }

    private void loadFormations() {
        try {
            formationList = FXCollections.observableArrayList(
                    formationService.selectALL()
            );
            filteredList = new FilteredList<>(formationList, p -> true);
            tvFormations.setItems(filteredList);
            lblTotal.setText("Total: " + formationList.size() + " formations");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDelete = new Button("🗑️");
            private final HBox buttons = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.setStyle(
                        "-fx-background-color: #fdcb6e; -fx-font-size: 14px;" +
                                "-fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 5 10;");
                btnDelete.setStyle(
                        "-fx-background-color: #d63031; -fx-text-fill: white;" +
                                "-fx-font-size: 14px; -fx-cursor: hand;" +
                                "-fx-background-radius: 5; -fx-padding: 5 10;");
                buttons.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    Formation f = getTableView().getItems().get(getIndex());
                    goToEdit(f);
                });

                btnDelete.setOnAction(e -> {
                    Formation f = getTableView().getItems().get(getIndex());
                    handleDelete(f);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    // ═════════════════════════════════════
    //  INIT FORM (Add + Edit)
    // ═════════════════════════════════════
    private void initForm() {
        cbCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        lblError.setText("");

        // If editing → fill form with data
        if (formationToEdit != null && tfTitle != null) {
            tfTitle.setText(formationToEdit.getTitle());
            taDescription.setText(formationToEdit.getDescription());
            tfVideoUrl.setText(formationToEdit.getVideoUrl());
            cbCategory.setValue(formationToEdit.getCategory());
        }
    }

    // ═════════════════════════════════════
    //  ADD (from FormationAdd.fxml)
    // ═════════════════════════════════════
    @FXML
    private void handleAdd() {
        if (!validateForm()) return;

        Formation f = new Formation();
        f.setTitle(tfTitle.getText().trim());
        f.setDescription(taDescription.getText().trim());
        f.setVideoUrl(tfVideoUrl.getText().trim());
        f.setCategory(cbCategory.getValue());

        try {
            formationService.insertOne(f);
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Formation added successfully! ✅");
            handleBackToList();
        } catch (SQLException e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════
    //  UPDATE (from FormationEdit.fxml)
    // ═════════════════════════════════════
    @FXML
    private void handleUpdate() {
        if (!validateForm()) return;
        if (formationToEdit == null) return;

        formationToEdit.setTitle(tfTitle.getText().trim());
        formationToEdit.setDescription(taDescription.getText().trim());
        formationToEdit.setVideoUrl(tfVideoUrl.getText().trim());
        formationToEdit.setCategory(cbCategory.getValue());

        try {
            formationService.updateOne(formationToEdit);
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Formation updated successfully! ✅");
            formationToEdit = null;
            handleBackToList();
        } catch (SQLException e) {
            lblError.setText("Error: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════
    //  DELETE (from table buttons)
    // ═════════════════════════════════════
    private void handleDelete(Formation f) {
        Optional<ButtonType> result = showConfirm(
                "Delete \"" + f.getTitle() + "\"?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                formationService.deleteOne(f);
                showAlert(Alert.AlertType.INFORMATION, "Deleted",
                        "Formation deleted! 🗑️");
                loadFormations();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        }
    }

    // ═════════════════════════════════════
    //  SEARCH
    // ═════════════════════════════════════
    @FXML
    private void handleSearch() {
        String keyword = tfSearch.getText().toLowerCase().trim();
        filteredList.setPredicate(f -> {
            if (keyword.isEmpty()) return true;
            return f.getTitle().toLowerCase().contains(keyword)
                    || f.getDescription().toLowerCase().contains(keyword)
                    || f.getCategory().toLowerCase().contains(keyword);
        });
        lblTotal.setText("Total: " + filteredList.size() + " formations");
    }

    // ═════════════════════════════════════
    //  REFRESH
    // ═════════════════════════════════════
    @FXML
    private void handleRefresh() {
        tfSearch.clear();
        loadFormations();
    }

    // ═════════════════════════════════════
    //  CLEAR FORM
    // ═════════════════════════════════════
    @FXML
    private void handleClear() {
        tfTitle.clear();
        taDescription.clear();
        tfVideoUrl.clear();
        cbCategory.getSelectionModel().clearSelection();
        lblError.setText("");
    }

    // ═════════════════════════════════════
    //  NAVIGATION
    // ═════════════════════════════════════
    private void goToEdit(Formation f) {
        formationToEdit = f;
        loadPage("/FormationEdit.fxml");
    }

    @FXML
    private void handleBackToList() {
        formationToEdit = null;
        loadPage("/FormationView.fxml");
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            // Find the contentArea in AdminDashboard
            StackPane contentArea = (StackPane) getAnyNode().getScene()
                    .lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get any available node to access the scene
    private javafx.scene.Node getAnyNode() {
        if (tvFormations != null) return tvFormations;
        if (tfTitle != null) return tfTitle;
        return null;
    }

    // ═════════════════════════════════════
    //  VALIDATION
    // ═════════════════════════════════════
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (tfTitle.getText().trim().isEmpty())
            errors.append("• Title is required\n");
        if (taDescription.getText().trim().isEmpty())
            errors.append("• Description is required\n");
        if (tfVideoUrl.getText().trim().isEmpty())
            errors.append("• Video URL is required\n");
        if (cbCategory.getValue() == null)
            errors.append("• Category is required\n");

        if (errors.length() > 0) {
            lblError.setText(errors.toString());
            return false;
        }
        return true;
    }

    // ═════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
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