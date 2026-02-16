package tn.esprit.projet.gui.patient; // Vérifie que c'est bien ce package dans ton projet

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import tn.esprit.projet.entities.Psychologue;
import tn.esprit.projet.services.PsychologueService;
import java.sql.SQLException;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Stage;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import tn.esprit.projet.utils.AlertUtils; // Pour utiliser tes nouvelles alertes

public class AdminPsyController {
    @FXML private TextField txtNom, txtPrenom, txtEmail, txtSpecialite;
    @FXML private TableView<Psychologue> tablePsys;
    @FXML private TableColumn<Psychologue, String> colNom;
    @FXML private TableColumn<Psychologue, String> colPrenom;
    @FXML private TableColumn<Psychologue, String> colEmail;
    @FXML private TableColumn<Psychologue, String> colSpecialite;

    private PsychologueService ps = new PsychologueService();

    @FXML
    void retourMenu(ActionEvent event) {
        try {
            // Chargement du menu principal situé à la racine de resources
            Parent root = FXMLLoader.load(getClass().getResource("/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("PI-DEV : Menu Principal");
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur retour menu : " + e.getMessage());
        }
    }
    @FXML
    void handleAjouter() {
        try {
            Psychologue newPsy = new Psychologue(0, txtNom.getText(), txtPrenom.getText(), txtEmail.getText(), txtSpecialite.getText());
            ps.insert(newPsy);
            chargerDonnees();
            AlertUtils.showInfo("Succès", "Psychologue ajouté avec succès !");
            clearFields();
        } catch (SQLException e) {
            AlertUtils.showError("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @FXML
    void handleModifier(ActionEvent event) {
        Psychologue selected = tablePsys.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showError("Sélection", "Veuillez sélectionner un psychologue à modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierPsy.fxml"));
            Parent root = loader.load();
            ModifierPsyController controller = loader.getController();
            controller.setPsyData(selected);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Modification");
            stage.showAndWait(); // Attend la fermeture pour rafraîchir
            chargerDonnees();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSupprimer() {
        Psychologue selected = tablePsys.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                ps.delete(selected.getId());
                chargerDonnees();
                AlertUtils.showInfo("Succès", "Psychologue supprimé avec succès !");
            } catch (SQLException e) {
                AlertUtils.showError("Erreur SQL", e.getMessage());
            }
        }
    }

    private void chargerDonnees() throws SQLException {
        // Récupère tout depuis la table psychologue
        tablePsys.getItems().setAll(ps.getAll());
    }

    private void clearFields() {
        txtNom.clear();
        txtPrenom.clear();
        txtEmail.clear();
        txtSpecialite.clear();
    }

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colSpecialite.setCellValueFactory(new PropertyValueFactory<>("specialite"));


        try {
            chargerDonnees();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}