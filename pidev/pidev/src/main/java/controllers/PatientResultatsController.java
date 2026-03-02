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
import java.util.List;
import java.util.ResourceBundle;

public class PatientResultatsController implements Initializable {

    @FXML private VBox resultsList;
    @FXML private Label lblCount;

    private QuizResultService quizResultService;
    private QuizService quizService;

    private User currentUser;
    private int currentUserId = 1;

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) this.currentUserId = user.getId_user();
        loadResults();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        quizResultService = new QuizResultService();
        quizService = new QuizService();
    }

    private void loadResults() {
        if (resultsList == null) return;
        resultsList.getChildren().clear();
        try {
            List<QuizResult> results = quizResultService.selectByUser(currentUserId);
            if (lblCount != null) lblCount.setText(results.size() + " résultat(s)");

            if (results.isEmpty()) {
                VBox empty = new VBox(10);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(50));
                Label icon = new Label("📭");
                icon.setStyle("-fx-font-size:40px;");
                Label txt = new Label("Aucun résultat de quiz");
                txt.setStyle("-fx-font-size:16px;-fx-text-fill:#A0AEC0;-fx-font-weight:600;");
                Label sub = new Label("Inscrivez-vous à une formation et passez un quiz");
                sub.setStyle("-fx-font-size:12px;-fx-text-fill:#CBD5E0;");
                empty.getChildren().addAll(icon, txt, sub);
                resultsList.getChildren().add(empty);
                return;
            }

            for (QuizResult r : results) {
                resultsList.getChildren().add(createResultCard(r));
            }
        } catch (SQLException e) {
            resultsList.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }

    private HBox createResultCard(QuizResult r) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        Label icon = new Label(r.isPassed() ? "✅" : "❌");
        icon.setStyle("-fx-font-size:24px;");

        String quizName = "Quiz #" + r.getQuizId();
        try {
            for (Quiz q : quizService.selectALL()) {
                if (q.getId() == r.getQuizId()) {
                    quizName = q.getTitle();
                    break;
                }
            }
        } catch (SQLException ignored) {}

        VBox info = new VBox(3);
        Label lblQuiz = new Label(quizName);
        lblQuiz.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#2d3436;");
        Label lblDate = new Label(r.getCompletedAt() != null
                ? r.getCompletedAt().toString().substring(0, 16) : "—");
        lblDate.setStyle("-fx-text-fill:#b2bec3;-fx-font-size:11px;");
        Label lblDetail = new Label(r.getScore() + "/" + r.getTotalPoints() + " pts");
        lblDetail.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
        info.getChildren().addAll(lblQuiz, lblDate, lblDetail);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox scoreBox = new VBox(3);
        scoreBox.setAlignment(Pos.CENTER);
        Label lblPct = new Label(String.format("%.0f%%", r.getPercentage()));
        lblPct.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:"
                + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        Label lblStatus = new Label(r.isPassed() ? "RÉUSSI" : "ÉCHOUÉ");
        lblStatus.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:"
                + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        scoreBox.getChildren().addAll(lblPct, lblStatus);

        card.getChildren().addAll(icon, info, spacer, scoreBox);
        return card;
    }
}