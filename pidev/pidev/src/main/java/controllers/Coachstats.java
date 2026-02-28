package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import entities.User;
import services.serviceUser;
import utils.LightDialog;

import java.sql.SQLException;
import java.util.List;

/**
 * Statistiques des patients pour le Coach
 * Affiche: liste des patients, humeur moyenne globale,
 * distribution par état (bien/moyen/bas), insights
 */
public class Coachstats {

    // Stat cards
    @FXML private Label lblTotalPatients;
    @FXML private Label lblMoyHumeur, lblMoyEmoji;
    @FXML private Label lblPatientsBien, lblPatientsMoyen, lblPatientsBas;

    // Progress bars humeur
    @FXML private ProgressBar pbBien, pbMoyen, pbBas;
    @FXML private Label lblPctBien, lblPctMoyen, lblPctBas;

    // Liste patients avec leur état
    @FXML private ListView<String> listPatients;
    @FXML private ComboBox<String> cbFiltreEtat;

    // Insight
    @FXML private Label lblInsight;

    // Search
    @FXML private TextField tfSearchPatient;

    private User currentCoach;
    private final serviceUser us = new serviceUser();
    private List<User> allPatients;

    // Données simulées d'humeur (à remplacer par vraies données de la BD)
    private final java.util.Random rng = new java.util.Random(42);

    @FXML
    void initialize() {
        if (cbFiltreEtat != null) {
            cbFiltreEtat.setItems(FXCollections.observableArrayList("Tous", "Bien (7-10)", "Moyen (4-6)", "Bas (0-3)"));
            cbFiltreEtat.setValue("Tous");
            cbFiltreEtat.setOnAction(e -> applyFilter());
        }
        if (tfSearchPatient != null) {
            tfSearchPatient.setOnKeyReleased(e -> applyFilter());
        }
    }

    public void setCoach(User coach) {
        this.currentCoach = coach;
        loadData();
    }

    private void loadData() {
        try {
            allPatients = us.selectALL().stream()
                    .filter(u -> u.getRole().equalsIgnoreCase("PATIENT"))
                    .toList();
            computeStats();
            refreshPatientList(allPatients);
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les données patients.");
        }
    }

    private void computeStats() {
        int total = allPatients.size();
        if (lblTotalPatients != null) lblTotalPatients.setText(String.valueOf(total));

        if (total == 0) {
            if (lblMoyHumeur != null) lblMoyHumeur.setText("N/A");
            if (lblMoyEmoji  != null) lblMoyEmoji.setText("😐");
            return;
        }

        // Scores simulés (dans une vraie app: lire la BD des rapports/moodtracker)
        double totalScore = 0;
        int bien = 0, moyen = 0, bas = 0;
        for (User p : allPatients) {
            // Seed par patient ID pour données stables (à remplacer par vraie BD)
            double score = 3 + (Math.abs((p.getId_user() * 137 + 29) % 70)) / 10.0;
            totalScore += score;
            if (score >= 7) bien++;
            else if (score >= 4) moyen++;
            else bas++;
        }
        double moy = totalScore / total;

        if (lblMoyHumeur != null) lblMoyHumeur.setText(String.format("%.1f", moy));
        if (lblMoyEmoji  != null) {
            lblMoyEmoji.setText(moy >= 7 ? "😄" : moy >= 4 ? "😐" : "😢");
        }
        if (lblPatientsBien  != null) lblPatientsBien.setText(String.valueOf(bien));
        if (lblPatientsMoyen != null) lblPatientsMoyen.setText(String.valueOf(moyen));
        if (lblPatientsBas   != null) lblPatientsBas.setText(String.valueOf(bas));

        // Progress bars
        if (pbBien  != null) pbBien.setProgress(bien  / (double) total);
        if (pbMoyen != null) pbMoyen.setProgress(moyen / (double) total);
        if (pbBas   != null) pbBas.setProgress(bas   / (double) total);
        if (lblPctBien  != null) lblPctBien.setText(String.format("%.0f%%",  bien  * 100.0 / total));
        if (lblPctMoyen != null) lblPctMoyen.setText(String.format("%.0f%%", moyen * 100.0 / total));
        if (lblPctBas   != null) lblPctBas.setText(String.format("%.0f%%",   bas   * 100.0 / total));

        // Insight automatique
        if (lblInsight != null) {
            String insight;
            if (bas > total / 2) {
                insight = "⚠️  " + bas + " patients ont un score d'humeur faible — envisagez une consultation prioritaire.";
            } else if (bien > total / 2) {
                insight = "🌟  " + bien + " patients montrent une bonne évolution — continuez le suivi actuel.";
            } else {
                insight = "💡  " + moyen + " patients sont en zone médiane — des séances de renforcement sont recommandées.";
            }
            lblInsight.setText(insight);
        }
    }

    private void refreshPatientList(List<User> patients) {
        if (listPatients == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (User p : patients) {
            double score = 3 + (Math.abs((p.getId_user() * 137 + 29) % 70)) / 10.0;
            String emoji = score >= 7 ? "😄" : score >= 4 ? "😐" : "😢";
            String etat  = score >= 7 ? "Bien" : score >= 4 ? "Moyen" : "Bas";
            items.add(String.format("%s  %s %s  —  humeur: %.1f/10  [%s]",
                    emoji, p.getPrenom(), p.getNom(), score, etat));
        }
        listPatients.setItems(items);
    }

    @FXML
    void applyFilter() {
        if (allPatients == null) return;
        String search = tfSearchPatient != null ? tfSearchPatient.getText().toLowerCase().trim() : "";
        String filtre = cbFiltreEtat != null ? cbFiltreEtat.getValue() : "Tous";

        List<User> filtered = allPatients.stream().filter(p -> {
            // filtre texte
            boolean matchSearch = search.isEmpty()
                    || p.getNom().toLowerCase().contains(search)
                    || p.getPrenom().toLowerCase().contains(search);
            // filtre état
            double score = 3 + (Math.abs((p.getId_user() * 137 + 29) % 70)) / 10.0;
            boolean matchEtat = filtre.equals("Tous")
                    || (filtre.startsWith("Bien")  && score >= 7)
                    || (filtre.startsWith("Moyen") && score >= 4 && score < 7)
                    || (filtre.startsWith("Bas")   && score < 4);
            return matchSearch && matchEtat;
        }).toList();

        refreshPatientList(filtered);
    }

    @FXML void handleRefresh() { loadData(); }
}