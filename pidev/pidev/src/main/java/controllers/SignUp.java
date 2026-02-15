package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;

import java.io.IOException;
import java.sql.SQLException;

public class SignUp {

    // 1. Déclaration des composants (kima sammithom f SceneBuilder)
    @FXML
    private TextField tfNom;
    @FXML
    private TextField tfPrenom;
    @FXML
    private TextField tfEmail;
    @FXML
    private TextField tfTel;
    @FXML
    private PasswordField pfMdp;
    @FXML
    private ComboBox<String> cbRole;

    private final serviceUser us = new serviceUser();

    // 2. Initialisation (teta3mal awel ma tet7all l page)
    @FXML
    void initialize() {
        // N3abbiw l ComboBox bel les roles
        cbRole.getItems().addAll("Patient", "Admin", "Coach");
        cbRole.setValue(""); // Valeur par défaut
    }

    // 3. Action Inscription
    @FXML
    void handleSignUp(ActionEvent event) {
        try {
            // Récupération des données
            String nom = tfNom.getText();
            String prenom = tfPrenom.getText();
            String email = tfEmail.getText();
            String mdp = pfMdp.getText();
            String tel = tfTel.getText();
            String role = cbRole.getValue();

            // Vérification simple (champs vides)
            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs !");
                return;
            }

            // Création de l'objet User
            User u = new User(0, nom, prenom, email, mdp, role, tel);

            // Appel du service (signUp verifi l email deja)
            us.signUp(u);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Compte créé avec succès !");

            // Nhezzouh lel page Login direct
            switchToLogin(event);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Inscription", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 4. Navigation vers Login
    @FXML
    void switchToLogin(ActionEvent event) throws IOException {
        // ✅ PATH SO77I7 -irect mel resources
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) tfEmail.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Connexion");
    }

    // Méthode helper pour afficher les alertes
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}