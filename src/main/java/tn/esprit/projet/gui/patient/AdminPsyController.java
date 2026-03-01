package tn.esprit.projet.gui.patient;

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
import java.util.regex.Pattern;
import tn.esprit.projet.utils.AlertUtils;

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
        // --- DÉBUT CONTRÔLE DE SAISIE RIGOUREUX ---
        String nom = txtNom.getText().trim();
        String prenom = txtPrenom.getText().trim();
        String email = txtEmail.getText().trim();
        String specialite = txtSpecialite.getText().trim();

        // 1. Vérification des champs vides
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || specialite.isEmpty()) {
            AlertUtils.showError("Données manquantes", "Veuillez remplir tous les champs.");
            return;
        }

        // 2. Vérification NOM (Lettres uniquement, pas de chiffres)
        if (!nom.matches("^[a-zA-ZÀ-ÿ\\s\\-]+$")) {
            AlertUtils.showError("Format Nom", "Le nom ne doit contenir que des lettres.");
            return;
        }

        // 3. Vérification PRÉNOM (Lettres uniquement, pas de chiffres)
        if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s\\-]+$")) {
            AlertUtils.showError("Format Prénom", "Le prénom ne doit contenir que des lettres.");
            return;
        }

        // 4. Vérification SPÉCIALITÉ (Lettres uniquement, pas de chiffres)
        if (!specialite.matches("^[a-zA-ZÀ-ÿ\\s\\-]+$")) {
            AlertUtils.showError("Format Spécialité", "La spécialité ne doit pas comporter de chiffres.");
            return;
        }

        // 5. Vérification EMAIL
        if (!isValidEmail(email)) {
            AlertUtils.showError("Format Email", "L'adresse email n'est pas valide.");
            return;
        }

        // 6. Longueur minimale
        if (nom.length() < 2 || prenom.length() < 2) {
            AlertUtils.showError("Longueur", "Le nom et prénom doivent faire au moins 2 caractères.");
            return;
        }
        // --- FIN CONTRÔLE DE SAISIE ---

        try {
            Psychologue newPsy = new Psychologue(0, nom, prenom, specialite, email);
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
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSupprimer() {
        Psychologue selected = tablePsys.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showError("Sélection", "Veuillez sélectionner un psychologue à supprimer.");
            return;
        }
        try {
            ps.delete(selected.getId());
            chargerDonnees();
            AlertUtils.showInfo("Succès", "Psychologue supprimé avec succès !");
        } catch (SQLException e) {
            AlertUtils.showError("Erreur SQL", e.getMessage());
        }
    }

    private void chargerDonnees() throws SQLException {
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

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }
}