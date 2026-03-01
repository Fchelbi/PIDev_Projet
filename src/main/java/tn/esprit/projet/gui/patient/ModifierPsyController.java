package tn.esprit.projet.gui.patient;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.projet.entities.Psychologue;
import tn.esprit.projet.services.PsychologueService;
import tn.esprit.projet.utils.AlertUtils;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class ModifierPsyController {

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtSpecialite;

    private Psychologue psy;

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
        // 1. Vérification de sécurité
        if (psy == null) {
            AlertUtils.showError("Erreur", "Aucun psychologue sélectionné pour la modification.");
            return;
        }

        // --- DÉBUT CONTRÔLE DE SAISIE RIGOUREUX ---
        String nom = txtNom.getText().trim();
        String prenom = txtPrenom.getText().trim();
        String email = txtEmail.getText().trim();
        String specialite = txtSpecialite.getText().trim();

        // 2. Vérification des champs vides
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || specialite.isEmpty()) {
            AlertUtils.showError("Champs vides", "Veuillez remplir tous les champs.");
            return;
        }

        // 3. Validation NOM (Pas de chiffres)
        if (!nom.matches("^[a-zA-ZÀ-ÿ\\s\\-]+$")) {
            AlertUtils.showError("Format Nom", "Le nom ne doit contenir que des lettres.");
            return;
        }

        // 4. Validation PRÉNOM (Pas de chiffres)
        if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s\\-]+$")) {
            AlertUtils.showError("Format Prénom", "Le prénom ne doit contenir que des lettres.");
            return;
        }

        // 5. Validation SPÉCIALITÉ (Pas de chiffres)
        if (!specialite.matches("^[a-zA-ZÀ-ÿ\\s\\-]+$")) {
            AlertUtils.showError("Format Spécialité", "La spécialité ne doit pas comporter de chiffres.");
            return;
        }

        // 6. Validation EMAIL
        if (!isValidEmail(email)) {
            AlertUtils.showError("Email invalide", "Veuillez saisir une adresse email valide.");
            return;
        }

        // 7. Longueur minimale
        if (nom.length() < 2 || prenom.length() < 2) {
            AlertUtils.showError("Format invalide", "Le nom et le prénom doivent avoir au moins 2 caractères.");
            return;
        }
        // --- FIN CONTRÔLE DE SAISIE ---

        try {
            // Mise à jour de l'objet local
            psy.setNom(nom);
            psy.setPrenom(prenom);
            psy.setEmail(email);
            psy.setSpecialite(specialite);

            PsychologueService service = new PsychologueService();
            service.update(psy);

            AlertUtils.showInfo("Succès", "Psychologue modifié avec succès !");

            Stage stage = (Stage) txtNom.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            AlertUtils.showError("Erreur SQL", "Impossible de modifier : " + e.getMessage());
        }
    }

    @FXML
    void handleAnnuler() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }
}