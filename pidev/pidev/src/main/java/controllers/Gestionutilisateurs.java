package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Gestionutilisateurs {

    @FXML
    private TableView<User> tableUsers;
    @FXML
    private TableColumn<User, Integer> colId;
    @FXML
    private TableColumn<User, String> colNom;
    @FXML
    private TableColumn<User, String> colPrenom;
    @FXML
    private TableColumn<User, String> colEmail;
    @FXML
    private TableColumn<User, String> colTel;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, Void> colActions;

    @FXML
    private TextField tfSearch;
    @FXML
    private ComboBox<String> cbFilterRole;
    @FXML
    private Label lblTotalUsers;
    @FXML
    private Label lblFilteredCount;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("👤 Current admin: " + user.getPrenom());
    }

    @FXML
    void initialize() {
        System.out.println("✅ GestionUtilisateurs initialized");

        // Setup ComboBox roles
        cbFilterRole.setItems(FXCollections.observableArrayList("Tous", "Patient", "Coach", "Admin"));
        cbFilterRole.setValue("Tous");

        // Setup table columns
        setupTableColumns();

        // Add actions column with buttons
        addActionButtons();

        // Load data
        loadUsers();

        // Search listener
        tfSearch.textProperty().addListener((obs, old, newVal) -> filterUsers());
        cbFilterRole.valueProperty().addListener((obs, old, newVal) -> filterUsers());
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        // Note: PropertyValueFactory uses getter names (without "get")
        // For id_user, it looks for getId_user()
        // Already defined in FXML with PropertyValueFactory

        tableUsers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Add action buttons (Modifier, Supprimer) to each row
     */
    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("✏️ Modifier");
            private final Button btnSupprimer = new Button("🗑️ Supprimer");
            private final HBox pane = new HBox(10, btnModifier, btnSupprimer);

            {
                // Style buttons
                btnModifier.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 6 12; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4;");
                btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 6 12; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 4;");

                pane.setAlignment(Pos.CENTER);

                // Actions
                btnModifier.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleModifier(user);
                });

                btnSupprimer.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleSupprimer(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    /**
     * Load all users from database
     */
    @FXML
    void loadUsers() {
        try {
            List<User> users = us.selectALL();
            allUsers.clear();
            allUsers.addAll(users);

            filteredUsers.clear();
            filteredUsers.addAll(users);

            tableUsers.setItems(filteredUsers);

            updateStats();

            System.out.println("📊 Loaded " + users.size() + " users");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger les utilisateurs.");
            e.printStackTrace();
        }
    }

    /**
     * Filter users based on search and role
     */
    private void filterUsers() {
        String searchText = tfSearch.getText().toLowerCase().trim();
        String roleFilter = cbFilterRole.getValue();

        filteredUsers.clear();

        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    // Search filter
                    boolean matchesSearch = searchText.isEmpty() ||
                            u.getNom().toLowerCase().contains(searchText) ||
                            u.getPrenom().toLowerCase().contains(searchText) ||
                            u.getEmail().toLowerCase().contains(searchText);

                    // Role filter
                    boolean matchesRole = roleFilter.equals("Tous") ||
                            u.getRole().equalsIgnoreCase(roleFilter);

                    return matchesSearch && matchesRole;
                })
                .collect(Collectors.toList());

        filteredUsers.addAll(filtered);
        updateStats();
    }

    /**
     * Update statistics labels
     */
    private void updateStats() {
        lblTotalUsers.setText("Total: " + allUsers.size() + " utilisateurs");
        lblFilteredCount.setText("Affichés: " + filteredUsers.size());
    }

    /**
     * Refresh table
     */
    @FXML
    void refreshTable(ActionEvent event) {
        System.out.println("🔄 Refresh table");
        loadUsers();
        tfSearch.clear();
        cbFilterRole.setValue("Tous");
    }

    /**
     * Handle search
     */
    @FXML
    void handleSearch(ActionEvent event) {
        filterUsers();
    }

    /**
     * Reset filters
     */
    @FXML
    void resetFilters(ActionEvent event) {
        tfSearch.clear();
        cbFilterRole.setValue("Tous");
        filterUsers();
    }

    /**
     * Show dialog to add new user
     */
    @FXML
    void showAjouterDialog(ActionEvent event) {
        System.out.println("➕ Ouvrir dialog Ajouter");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterUtilisateur.fxml"));
            Scene scene = new Scene(loader.load());

            Stage dialog = new Stage();
            dialog.setTitle("Ajouter un utilisateur");
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);

            // Get controller and set callback
            Ajouterutilisateur controller = loader.getController();
            controller.setOnUserAdded(() -> refreshTable(null));

            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire d'ajout.");
        }
    }

    /**
     * Handle modify user
     */
    private void handleModifier(User user) {
        System.out.println("✏️ Modifier: " + user.getNom());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierUtilisateur.fxml"));
            Scene scene = new Scene(loader.load());

            Stage dialog = new Stage();
            dialog.setTitle("Modifier l'utilisateur");
            dialog.setScene(scene);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);

            // Pass user to modify
            Modifierutilisateur controller = loader.getController();
            controller.setUser(user);
            controller.setOnUserUpdated(() -> refreshTable(null));

            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire de modification.");
        }
    }

    /**
     * Handle delete user
     */
    private void handleSupprimer(User user) {
        System.out.println("🗑️ Supprimer: " + user.getNom());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " +
                user.getPrenom() + " " + user.getNom() + " ?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                us.deleteOne(user);
                showAlert(Alert.AlertType.INFORMATION, "Succès",
                        "Utilisateur supprimé avec succès!");
                refreshTable(null);

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de supprimer cet utilisateur.");
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