package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import java.sql.SQLException;
import entities.User;
import entities.Psychologue;
import services.serviceUser;
import services.PsychologueService;
import utils.AlertUtils;

public class AdminPsyController {

    // On change la TableView pour afficher des "User"
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;

    private serviceUser userService = new serviceUser();
    private PsychologueService psyService = new PsychologueService();

    @FXML
    public void initialize() {
        // Mapping des colonnes sur l'entité User
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        chargerUsers();
    }

    private void chargerUsers() {
        try {
            // On récupère tous les utilisateurs
            tableUsers.getItems().setAll(userService.selectALL());
        } catch (SQLException e) {
            AlertUtils.showError("Erreur SQL", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    void handleAjouterCommePsy() {
        User selectedUser = tableUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            AlertUtils.showError("Sélection", "Veuillez sélectionner un utilisateur dans la liste.");
            return;
        }

        try {
            // On crée un objet Psychologue basé sur l'User sélectionné
            // On met une spécialité par défaut car on a supprimé le formulaire
            Psychologue newPsy = new Psychologue(
                    0,
                    selectedUser.getNom(),
                    selectedUser.getPrenom(),
                    "Généraliste",
                    selectedUser.getEmail()
            );

            psyService.insert(newPsy);
            AlertUtils.showInfo("Succès", selectedUser.getPrenom() + " est maintenant enregistré comme Psychologue !");
        } catch (SQLException e) {
            AlertUtils.showError("Erreur", "Cet utilisateur est peut-être déjà psychologue ou erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    void handleModifier(ActionEvent event) {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showError("Sélection", "Veuillez sélectionner un utilisateur à modifier.");
            return;
        }
        // Ici, tu peux rediriger vers ton interface de modification d'User existante
        AlertUtils.showInfo("Info", "Redirection vers la modification de l'utilisateur...");
    }




}