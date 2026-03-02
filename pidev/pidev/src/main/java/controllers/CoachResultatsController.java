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
            List<QuizResult> all = quizResultService.selectALL();
            String filter = cbFilter != null ? cbFilter.getValue() : "Tous";

            List<QuizResult> filtered = all.stream()
                    .filter(r -> {
                        if ("Réussis".equals(filter)) return r.isPassed();
                        if ("Échoués".equals(filter)) return !r.isPassed();
                        return true;
                    })
                    .sorted((a, b) -> {
                        if (a.getCompletedAt() == null) return 1;
                        if (b.getCompletedAt() == null) return -1;
                        return b.getCompletedAt().compareTo(a.getCompletedAt());
                    })
                    .collect(Collectors.toList());

            if (lblCount != null) lblCount.setText(filtered.size() + " résultat(s)");

            if (filtered.isEmpty()) {
                Label empty = new Label("Aucun résultat trouvé.");
                empty.setStyle("-fx-font-size:15px;-fx-text-fill:#A0AEC0;-fx-padding:30;");
                resultsList.getChildren().add(empty);
                return;
            }

            for (QuizResult r : filtered) {
                HBox card = new HBox(15);
                card.setPadding(new Insets(14));
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

                Label icon = new Label(r.isPassed() ? "✅" : "❌");
                icon.setStyle("-fx-font-size:22px;");

                String qName = "Quiz #" + r.getQuizId();
                try {
                    for (Quiz q : quizService.selectALL())
                        if (q.getId() == r.getQuizId()) { qName = q.getTitle(); break; }
                } catch (SQLException ignored) {}

                VBox info = new VBox(3);
                Label u = new Label("Patient #" + r.getUserId());
                u.setStyle("-fx-font-weight:bold;-fx-text-fill:#2d3436;");
                Label q = new Label(qName);
                q.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
                Label d = new Label(r.getCompletedAt() != null
                        ? r.getCompletedAt().toString().substring(0, 16) : "—");
                d.setStyle("-fx-text-fill:#b2bec3;-fx-font-size:11px;");
                info.getChildren().addAll(u, q, d);

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