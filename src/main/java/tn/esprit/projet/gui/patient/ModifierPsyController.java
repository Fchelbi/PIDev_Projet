package tn.esprit.projet.gui.patient;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Psychologue; // IMPORT INDISPENSABLE
import tn.esprit.projet.services.PsychologueService;
import tn.esprit.projet.utils.AlertUtils;
import java.sql.SQLException;

public class ModifierPsyController {

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtSpecialite;

    private Psychologue psy; // Maintenant reconnu grâce à l'import

    /**
     * Reçoit les données du psychologue sélectionné depuis la table
     */
    public void setPsyData(Psychologue p) {
        this.psy = p;
        if (p != null) {
            txtNom.setText(p.getNom());
            txtPrenom.setText(p.getPrenom());
            txtEmail.setText(p.getEmail());
            txtSpecialite.setText(p.getSpecialite());
        }
    }

    @FXML
    void handleSave() {
        // Vérification de sécurité pour éviter les NullPointerException
        if (psy == null) {
            AlertUtils.showError("Erreur", "Aucun psychologue sélectionné.");
            return;
        }

        try {
            // Mise à jour de l'objet local avec les nouvelles saisies du formulaire
            psy.setNom(txtNom.getText());
            psy.setPrenom(txtPrenom.getText());
            psy.setEmail(txtEmail.getText());
            psy.setSpecialite(txtSpecialite.getText());

            PsychologueService service = new PsychologueService();

            // Appel de la méthode update qui utilise l'ID pour la clause WHERE
            service.update(psy);

            AlertUtils.showInfo("Succès", "Psychologue modifié avec succès !");

            // Fermeture de la fenêtre (Stage) actuelle
            Stage stage = (Stage) txtNom.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            // Affiche l'erreur SQL réelle en cas de problème (ex: email en double)
            AlertUtils.showError("Erreur SQL", "Impossible de modifier : " + e.getMessage());
        }
    }

    @FXML
    void handleAnnuler() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}