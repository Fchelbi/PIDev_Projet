package controllers;

import entities.Formation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.FormationService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class FormationController implements Initializable {

    // ─── Table ───────────────────────
    @FXML private TableView<Formation> tvFormations;
    @FXML private TableColumn<Formation, String> colTitle;
    @FXML private TableColumn<Formation, String> colDescription;
    @FXML private TableColumn<Formation, String> colVideoUrl;
    @FXML private TableColumn<Formation, String> colCategory;
    @FXML private TableColumn<Formation, Void> colActions;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotal;

    // ─── Sections ────────────────────
    @FXML private StackPane formationContent;
    @FXML private VBox tableSection;
    @FXML private VBox formSection;

    // ─── Form ────────────────────────
    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private TextField tfVideoUrl;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblError;
    @FXML private Label lblFormTitle;
    @FXML private Button btnSave;

    // ─── Data ────────────────────────
    private FormationService formationService;
    private ObservableList<Formation> formationList;
    private FilteredList<Formation> filteredList;
    private Formation editingFormation = null;
    private boolean isEditMode = false;

    private final String[] CATEGORIES = {
            "Communication", "Leadership", "Teamwork",
            "Self-Improvement", "Public Speaking",
            "Conflict Resolution", "Emotional Intelligence",
            "Time Management"
    };

    // ═════════════════════════════════
    //  INITIALIZE
    // ═════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();

        // Setup categories
        if (cbCategory != null) {
            cbCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        }

        // Setup table
        if (tvFormations != null) {
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            colVideoUrl.setCellValueFactory(new PropertyValueFactory<>("videoUrl"));
            colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
            setupActionsColumn();
            loadFormations();
        }
    }

    // ═════════════════════════════════
    //  LOAD DATA
    // ═════════════════════════════════
    private void loadFormations() {
        try {
            formationList = FXCollections.observableArrayList(
                    formationService.selectALL()
            );
            filteredList = new FilteredList<>(formationList, p -> true);
            tvFormations.setItems(filteredList);
            lblTotal.setText("Total: " + formationList.size() + " formations");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les formations:\n" + e.getMessage());
        }
    }

    // ═════════════════════════════════
    //  ACTION BUTTONS IN TABLE
    // ═════════════════════════════════
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

                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                buttons.setAlignment(Pos.CENTER);

                btnEdit.setOnAction(e -> {
                    Formation f = getTableView().getItems().get(getIndex());
                    showEditForm(f);
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

    // ═════════════════════════════════
    //  SHOW TABLE
    // ═════════════════════════════════
    @FXML
    private void showTable() {
        tableSection.setVisible(true);
        tableSection.setManaged(true);
        formSection.setVisible(false);
        formSection.setManaged(false);

        clearForm();
        loadFormations();
    }

    // ═════════════════════════════════
    //  SHOW ADD FORM
    // ═════════════════════════════════
    @FXML
    private void showAddForm() {
        isEditMode = false;
        editingFormation = null;
        lblFormTitle.setText("➕ Ajouter Formation");
        btnSave.setText("✅ Enregistrer");
        clearForm();

        tableSection.setVisible(false);
        tableSection.setManaged(false);
        formSection.setVisible(true);
        formSection.setManaged(true);
    }

    // ═════════════════════════════════
    //  SHOW EDIT FORM
    // ═════════════════════════════════
    private void showEditForm(Formation f) {
        isEditMode = true;
        editingFormation = f;
        lblFormTitle.setText("✏️ Modifier Formation");
        btnSave.setText("💾 Mettre à jour");

        // Fill form with data
        tfTitle.setText(f.getTitle());
        taDescription.setText(f.getDescription());
        tfVideoUrl.setText(f.getVideoUrl());
        cbCategory.setValue(f.getCategory());
        lblError.setText("");

        tableSection.setVisible(false);
        tableSection.setManaged(false);
        formSection.setVisible(true);
        formSection.setManaged(true);
    }

    // ═════════════════════════════════
    //  SAVE (Add or Update)
    // ═════════════════════════════════
    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        if (isEditMode) {
            // UPDATE
            editingFormation.setTitle(tfTitle.getText().trim());
            editingFormation.setDescription(taDescription.getText().trim());
            editingFormation.setVideoUrl(tfVideoUrl.getText().trim());
            editingFormation.setCategory(cbCategory.getValue());

            try {
                formationService.updateOne(editingFormation);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Formation modifiée avec succès! ✅");
                showTable();
            } catch (SQLException e) {
                lblError.setText("Erreur: " + e.getMessage());
            }

        } else {
            // ADD
            Formation f = new Formation();
            f.setTitle(tfTitle.getText().trim());
            f.setDescription(taDescription.getText().trim());
            f.setVideoUrl(tfVideoUrl.getText().trim());
            f.setCategory(cbCategory.getValue());

            try {
                formationService.insertOne(f);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Formation ajoutée avec succès! ✅");
                showTable();
            } catch (SQLException e) {
                lblError.setText("Erreur: " + e.getMessage());
            }
        }
    }

    // ═════════════════════════════════
    //  DELETE
    // ═════════════════════════════════
    private void handleDelete(Formation f) {
        Optional<ButtonType> result = showConfirm(
                "Supprimer \"" + f.getTitle() + "\"?\nCette action est irréversible!");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                formationService.deleteOne(f);
                showAlert(Alert.AlertType.INFORMATION, "Supprimé",
                        "Formation supprimée! 🗑️");
                loadFormations();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    // ═════════════════════════════════
    //  SEARCH
    // ═════════════════════════════════
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

    // ═════════════════════════════════
    //  REFRESH
    // ═════════════════════════════════
    @FXML
    private void handleRefresh() {
        tfSearch.clear();
        loadFormations();
    }

    // ═════════════════════════════════
    //  CLEAR FORM
    // ═════════════════════════════════
    @FXML
    private void handleClear() {
        clearForm();
    }

    private void clearForm() {
        if (tfTitle != null) tfTitle.clear();
        if (taDescription != null) taDescription.clear();
        if (tfVideoUrl != null) tfVideoUrl.clear();
        if (cbCategory != null) cbCategory.getSelectionModel().clearSelection();
        if (lblError != null) lblError.setText("");
        editingFormation = null;
        isEditMode = false;
    }

    // ═════════════════════════════════
    //  VALIDATION
    // ═════════════════════════════════
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (tfTitle.getText().trim().isEmpty())
            errors.append("• Le titre est obligatoire\n");
        if (taDescription.getText().trim().isEmpty())
            errors.append("• La description est obligatoire\n");
        if (tfVideoUrl.getText().trim().isEmpty())
            errors.append("• L'URL vidéo est obligatoire\n");
        if (cbCategory.getValue() == null)
            errors.append("• La catégorie est obligatoire\n");

        if (errors.length() > 0) {
            lblError.setText(errors.toString());
            return false;
        }
        return true;
    }

    // ═════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
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