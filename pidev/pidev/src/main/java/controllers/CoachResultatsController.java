package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CoachResultatsController implements Initializable {

    @FXML private VBox resultsList;
    @FXML private Label lblCount;
    @FXML private ComboBox<String> cbFilter;

    private QuizResultService quizResultService;
    private QuizService quizService;
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        loadResults();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        quizResultService = new QuizResultService();
        quizService = new QuizService();
        if (cbFilter != null) {
            cbFilter.getItems().addAll("Tous", "Réussis", "Échoués");
            cbFilter.setValue("Tous");
            cbFilter.setOnAction(e -> loadResults());
        }
    }

    private void loadResults() {
        if (resultsList == null) return;
        resultsList.getChildren().clear();

        try {
            // ── Step 1: get only THIS coach's formations ──
            FormationService formationService = new FormationService();
            ParticipantService participantService = new ParticipantService();

            List<Formation> coachFormations;
            if (currentUser != null) {
                coachFormations = formationService.selectByCoach(currentUser.getId_user());
            } else {
                coachFormations = formationService.selectALL();
            }

            // ── Step 2: get patient IDs enrolled in coach's formations ──
            List<Integer> coachFormationIds = new ArrayList<>();
            for (Formation f : coachFormations) {
                coachFormationIds.add(f.getId());
            }

            List<Integer> coachPatientIds = new ArrayList<>();
            for (Participant p : participantService.selectALL()) {
                if (coachFormationIds.contains(p.getFormationId())) {
                    if (!coachPatientIds.contains(p.getUserId())) {
                        coachPatientIds.add(p.getUserId());
                    }
                }
            }

            // ── Step 3: get only results from those patients ──
            List<QuizResult> coachResults = new ArrayList<>();
            for (QuizResult r : quizResultService.selectALL()) {
                if (coachPatientIds.contains(r.getUserId())) {
                    coachResults.add(r);
                }
            }

            // ── Step 4: apply filter ──
            String filter = cbFilter != null ? cbFilter.getValue() : "Tous";
            List<QuizResult> filtered = new ArrayList<>();
            for (QuizResult r : coachResults) {
                if ("Réussis".equals(filter) && !r.isPassed()) continue;
                if ("Échoués".equals(filter) && r.isPassed()) continue;
                filtered.add(r);
            }

            // ── Step 5: sort by date descending ──
            filtered.sort((a, b) -> {
                if (a.getCompletedAt() == null) return 1;
                if (b.getCompletedAt() == null) return -1;
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            });

            // ── Step 6: load patient names from DB ──
            serviceUser us = new serviceUser();
            List<User> allUsers = us.selectALL();

            if (lblCount != null) lblCount.setText(filtered.size() + " résultat(s)");

            if (filtered.isEmpty()) {
                Label empty = new Label("Aucun résultat pour vos patients.");
                empty.setStyle("-fx-font-size:15px;-fx-text-fill:#A0AEC0;-fx-padding:30;");
                resultsList.getChildren().add(empty);
                return;
            }

            // ── Step 7: build cards ──
            for (QuizResult r : filtered) {

                // Find patient name
                String patientName = "Patient #" + r.getUserId();
                for (User u : allUsers) {
                    if (u.getId_user() == r.getUserId()) {
                        patientName = u.getPrenom() + " " + u.getNom();
                        break;
                    }
                }

                // Find quiz name
                String qName = "Quiz #" + r.getQuizId();
                try {
                    for (Quiz q : quizService.selectALL()) {
                        if (q.getId() == r.getQuizId()) {
                            qName = q.getTitle();
                            break;
                        }
                    }
                } catch (SQLException ignored) {}

                HBox card = new HBox(15);
                card.setPadding(new Insets(14));
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

                Label icon = new Label(r.isPassed() ? "✅" : "❌");
                icon.setStyle("-fx-font-size:22px;");

                VBox info = new VBox(3);
                Label lblPatient = new Label(patientName);
                lblPatient.setStyle("-fx-font-weight:bold;-fx-text-fill:#2d3436;");
                Label lblQuiz = new Label(qName);
                lblQuiz.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
                Label lblDate = new Label(r.getCompletedAt() != null
                        ? r.getCompletedAt().toString().substring(0, 16) : "—");
                lblDate.setStyle("-fx-text-fill:#b2bec3;-fx-font-size:11px;");
                info.getChildren().addAll(lblPatient, lblQuiz, lblDate);

                HBox spacer = new HBox();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label score = new Label(String.format("%.0f%%", r.getPercentage()));
                score.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:"
                        + (r.isPassed() ? "#00b894" : "#d63031") + ";");

                card.getChildren().addAll(icon, info, spacer, score);
                resultsList.getChildren().add(card);
            }

        } catch (SQLException e) {
            resultsList.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }
}