package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;
import utils.LightDialog;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class GestionUtilisateurs {
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterRole;
    @FXML private Label lblTotalUsers, lblFilteredCount;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    public void setCurrentUser(User user) { this.currentUser = user; }

    @FXML void initialize() {
        cbFilterRole.setItems(FXCollections.observableArrayList("Tous", "Patient", "Coach", "Admin"));
        cbFilterRole.setValue("Tous");
        tableUsers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        addActionButtons();
        loadUsers();
        tfSearch.textProperty().addListener((o, ov, nv) -> filterUsers());
        cbFilterRole.valueProperty().addListener((o, ov, nv) -> filterUsers());
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnMod = new Button("✏️");
            private final Button btnDel = new Button("🗑️");
            private final HBox pane = new HBox(8, btnMod, btnDel);
            {
                btnMod.setStyle("-fx-background-color: #A7B5E0; -fx-text-fill: white; -fx-padding: 6 12; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6;");
                btnDel.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-padding: 6 12; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6;");
                pane.setAlignment(Pos.CENTER);
                btnMod.setOnAction(e -> handleModifier(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> handleSupprimer(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty); setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML void loadUsers() {
        try {
            allUsers.setAll(us.selectALL());
            filteredUsers.setAll(allUsers);
            tableUsers.setItems(filteredUsers);
            updateStats();
        } catch (SQLException e) { LightDialog.showError("Erreur", "Impossible de charger les utilisateurs."); }
    }

    private void filterUsers() {
        String s = tfSearch.getText().toLowerCase().trim();
        String r = cbFilterRole.getValue();
        filteredUsers.setAll(allUsers.stream().filter(u ->
                (s.isEmpty() || u.getNom().toLowerCase().contains(s) || u.getPrenom().toLowerCase().contains(s) || u.getEmail().toLowerCase().contains(s)) &&
                        (r.equals("Tous") || u.getRole().equalsIgnoreCase(r))
        ).collect(Collectors.toList()));
        updateStats();
    }

    private void updateStats() {
        lblTotalUsers.setText("Total: " + allUsers.size());
        lblFilteredCount.setText("Affichés: " + filteredUsers.size());
    }

    @FXML void refreshTable(ActionEvent e) { loadUsers(); tfSearch.clear(); cbFilterRole.setValue("Tous"); }
    @FXML void handleSearch(ActionEvent e) { filterUsers(); }
    @FXML void resetFilters(ActionEvent e) { tfSearch.clear(); cbFilterRole.setValue("Tous"); }

    @FXML void showAjouterDialog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Ajouterutilisateur.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Ajouter un utilisateur");
            dialog.setScene(new Scene(loader.load()));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            ((AjouterUtilisateur) loader.getController()).setOnUserAdded(() -> refreshTable(null));
            dialog.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleModifier(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Modifierutilisateur.fxml"));
            Stage dialog = new Stage();
            dialog.setTitle("Modifier l'utilisateur");
            dialog.setScene(new Scene(loader.load()));
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            ModifierUtilisateur ctrl = loader.getController();
            ctrl.setUser(user); ctrl.setOnUserUpdated(() -> refreshTable(null));
            dialog.showAndWait();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleSupprimer(User user) {
        if (LightDialog.showConfirmation("Supprimer", "Supprimer " + user.getPrenom() + " " + user.getNom() + " ?", "🗑️")) {
            try {
                us.deleteOne(user);
                LightDialog.showSuccess("Succès", "Utilisateur supprimé !");
                refreshTable(null);
            } catch (SQLException e) { LightDialog.showError("Erreur", "Impossible de supprimer."); }
        }
    }
}