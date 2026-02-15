package controllers;

import entities.Formation;
import javafx.beans.property.SimpleStringProperty;
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

public class FormationListController implements Initializable {

    @FXML private TableView<Formation> tvFormations;
    @FXML private TableColumn<Formation, String> colTitle;
    @FXML private TableColumn<Formation, String> colDescription;
    @FXML private TableColumn<Formation, String> colVideoUrl;
    @FXML private TableColumn<Formation, String> colCategory;
    @FXML private TableColumn<Formation, Void> colActions;
    @FXML private TextField tfSearch;
    @FXML private Label lblTotal;

    private FormationService formationService;
    private ObservableList<Formation> formationList;
    private FilteredList<Formation> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();

        // Setup columns (NO ID column)
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colVideoUrl.setCellValueFactory(new PropertyValueFactory<>("videoUrl"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));

        // Setup action buttons in last column
        setupActionsColumn();

        // Load data
        loadFormations();
    }

    // ═══════════════════════════════════════
    //  LOAD DATA
    // ═══════════════════════════════════════
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

    // ═══════════════════════════════════════
    //  ACTION BUTTONS (Edit + Delete)
    // ═══════════════════════════════════════
    private void setupActionsColumn() {
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️ Edit");
            private final Button btnDelete = new Button("🗑️ Delete");
            private final HBox buttons = new HBox(8, btnEdit, btnDelete);

            {
                // Style buttons
                btnEdit.setStyle(
                        "-fx-background-color: #fdcb6e;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;" +
                                "-fx-background-radius: 5;" +
                                "-fx-padding: 5 10;");

                btnDelete.setStyle(
                        "-fx-background-color: #d63031;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;" +
                                "-fx-background-radius: 5;" +
                                "-fx-padding: 5 10;");

                buttons.setAlignment(Pos.CENTER);

                // Edit action
                btnEdit.setOnAction(event -> {
                    Formation f = getTableView().getItems().get(getIndex());
                    handleEdit(f);
                });

                // Delete action
                btnDelete.setOnAction(event -> {
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

    // ═══════════════════════════════════════
    //  EDIT
    // ═══════════════════════════════════════
    private void handleEdit(Formation formation) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FormationEdit.fxml"));
            Parent page = loader.load();

            // Pass data to edit controller
            FormationEditController controller = loader.getController();
            controller.setFormation(formation);
            controller.setListController(this);

            // Replace content in center area
            StackPane contentArea = (StackPane) tvFormations.getScene()
                    .lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Cannot load edit page: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════
    private void handleDelete(Formation formation) {
        Optional<ButtonType> result = showConfirm(
                "Delete \"" + formation.getTitle() + "\"?\nThis cannot be undone!");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                formationService.deleteOne(formation);
                showAlert(Alert.AlertType.INFORMATION, "Deleted",
                        "Formation deleted successfully!");
                loadFormations();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════
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

    // ═══════════════════════════════════════
    //  REFRESH
    // ═══════════════════════════════════════
    @FXML
    private void handleRefresh() {
        tfSearch.clear();
        loadFormations();
    }

    // Called from other controllers to refresh this list
    public void refreshList() {
        loadFormations();
    }

    // ═══════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════
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