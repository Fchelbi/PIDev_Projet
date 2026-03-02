package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.*;
import utils.VideoPlayerUtil;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class PatientMesFormationsController implements Initializable {

    @FXML private FlowPane cardsPane;
    @FXML private Label lblCount;

    private FormationService formationService;
    private ParticipantService participantService;
    private QuizService quizService;
    private QuestionService questionService;
    private ReponseService reponseService;
    private QuizResultService quizResultService;
    private CertificateService certificateService;

    private User currentUser;
    private int currentUserId = 1;

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) this.currentUserId = user.getId_user();
        loadMyFormations();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        participantService = new ParticipantService();
        quizService = new QuizService();
        questionService = new QuestionService();
        reponseService = new ReponseService();
        quizResultService = new QuizResultService();
        certificateService = new CertificateService();
    }

    private void loadMyFormations() {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();
        try {
            List<Participant> myList = participantService.selectALL();
            List<Formation> allFormations = formationService.selectALL();
            int count = 0;
            for (Participant p : myList) {
                if (p.getUserId() == currentUserId) {
                    for (Formation f : allFormations) {
                        if (f.getId() == p.getFormationId()) {
                            cardsPane.getChildren().add(createMyCard(f));
                            count++;
                        }
                    }
                }
            }
            if (lblCount != null) lblCount.setText(count + " formation(s) inscrite(s)");
            if (count == 0) {
                VBox empty = new VBox(10);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(50));
                Label icon = new Label("📭");
                icon.setStyle("-fx-font-size:40px;");
                Label txt = new Label("Vous n'êtes inscrit à aucune formation");
                txt.setStyle("-fx-font-size:16px;-fx-text-fill:#A0AEC0;-fx-font-weight:600;");
                Label sub = new Label("Allez dans 'Formations' pour vous inscrire");
                sub.setStyle("-fx-font-size:12px;-fx-text-fill:#CBD5E0;");
                empty.getChildren().addAll(icon, txt, sub);
                cardsPane.getChildren().add(empty);
            }
        } catch (SQLException e) {
            cardsPane.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }

    private VBox createMyCard(Formation f) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");
        card.setPadding(new Insets(20));

        Label lblCat = new Label(f.getCategory() != null ? f.getCategory() : "—");
        lblCat.setStyle("-fx-background-color:#4A6FA5;-fx-text-fill:white;"
                + "-fx-padding:3 10;-fx-background-radius:10;-fx-font-size:11px;");

        Label lblTitle = new Label(f.getTitle());
        lblTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2d3436;");
        lblTitle.setWrapText(true);

        Label lblDesc = new Label(f.getDescription());
        lblDesc.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(50);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);

        // Video button
        if (f.getVideoUrl() != null && !f.getVideoUrl().trim().isEmpty()) {
            Button btnVideo = new Button("▶ Vidéo");
            btnVideo.setStyle("-fx-background-color:#6c5ce7;-fx-text-fill:white;"
                    + "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 12;");
            btnVideo.setOnAction(e -> watchVideo(f));
            buttons.getChildren().add(btnVideo);
        }

        // Quiz button
        try {
            Quiz quiz = quizService.selectByFormation(f.getId());
            if (quiz != null) {
                boolean passed = quizResultService.hasUserPassedQuiz(currentUserId, quiz.getId());
                Button btnQuiz = new Button(passed ? "✅ Réussi" : "📝 Quiz");
                btnQuiz.setStyle("-fx-background-color:" + (passed ? "#00b894" : "#E8956D")
                        + ";-fx-text-fill:white;-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 12;");
                if (!passed) {
                    btnQuiz.setOnAction(e -> takeQuiz(f, quiz));
                }
                buttons.getChildren().add(btnQuiz);
            }
        } catch (SQLException ignored) {}

        card.getChildren().addAll(lblCat, lblTitle, lblDesc, buttons);
        return card;
    }

    private void watchVideo(Formation f) {
        String videoUrl = f.getVideoUrl();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🎬 " + f.getTitle());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(700, 500);
        VBox container;
        if (VideoPlayerUtil.isYouTubeUrl(videoUrl)) {
            container = VideoPlayerUtil.createYouTubePlayer(videoUrl);
        } else {
            File file = new File(videoUrl);
            if (file.exists()) container = VideoPlayerUtil.createLocalPlayer(videoUrl);
            else container = VideoPlayerUtil.createErrorMessage("Fichier non trouvé", videoUrl);
        }
        dialog.getDialogPane().setContent(container);
        dialog.setOnCloseRequest(e -> VideoPlayerUtil.stopMedia(container));
        dialog.showAndWait();
    }

    private void takeQuiz(Formation f, Quiz quiz) {
        try {
            List<Question> questions = questionService.selectByQuiz(quiz.getId());
            if (questions.isEmpty()) {
                showInfo("Ce quiz n'a pas encore de questions.");
                return;
            }

            Dialog<int[]> dialog = new Dialog<>();
            dialog.setTitle("📝 Quiz: " + quiz.getTitle());
            ButtonType submitBtn = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(submitBtn, ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(600);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));

            Label header = new Label("Quiz: " + quiz.getTitle()
                    + " | Score minimum: " + quiz.getPassingScore() + "%");
            header.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2D3748;");
            content.getChildren().add(header);

            java.util.Map<Integer, ToggleGroup> groups = new java.util.HashMap<>();
            int num = 1;
            for (Question q : questions) {
                VBox qBox = new VBox(8);
                qBox.setStyle("-fx-background-color:#f8f9fa;-fx-padding:15;-fx-background-radius:8;");
                Label lblQ = new Label("Q" + num + ". " + q.getQuestionText()
                        + " (" + q.getPoints() + " pts)");
                lblQ.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
                lblQ.setWrapText(true);
                qBox.getChildren().add(lblQ);

                ToggleGroup group = new ToggleGroup();
                groups.put(q.getId(), group);
                for (Reponse r : reponseService.selectByQuestion(q.getId())) {
                    RadioButton rb = new RadioButton(r.getOptionText());
                    rb.setToggleGroup(group);
                    rb.setUserData(r);
                    rb.setWrapText(true);
                    rb.setStyle("-fx-font-size:13px;");
                    qBox.getChildren().add(rb);
                }
                content.getChildren().add(qBox);
                num++;
            }

            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(500);
            dialog.getDialogPane().setContent(scroll);

            dialog.setResultConverter(btn -> {
                if (btn == submitBtn) {
                    int total = 0, earned = 0;
                    for (Question q : questions) {
                        total += q.getPoints();
                        ToggleGroup g = groups.get(q.getId());
                        if (g.getSelectedToggle() != null) {
                            Reponse chosen = (Reponse) ((RadioButton) g.getSelectedToggle()).getUserData();
                            if (chosen.isCorrect()) earned += q.getPoints();
                        }
                    }
                    return new int[]{earned, total};
                }
                return null;
            });

            dialog.showAndWait().ifPresent(scores -> {
                int earned = scores[0], total = scores[1];
                double pct = total > 0 ? (earned * 100.0 / total) : 0;
                boolean passed = pct >= quiz.getPassingScore();

                QuizResult result = new QuizResult();
                result.setQuizId(quiz.getId());
                result.setUserId(currentUserId);
                result.setScore(earned);
                result.setTotalPoints(total);
                result.setPassed(passed);
                try {
                    quizResultService.insertOne(result);
                } catch (SQLException e) {
                    System.err.println("Save result error: " + e.getMessage());
                }

                String msg = passed
                        ? String.format("🎉 RÉUSSI! Score: %.0f%% (%d/%d pts)", pct, earned, total)
                        : String.format("❌ ÉCHOUÉ. Score: %.0f%% (%d/%d pts)\nMinimum requis: %d%%",
                        pct, earned, total, quiz.getPassingScore());

                if (passed) {
                    String cert = certificateService.generateCertificate(
                            currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Patient #" + currentUserId,
                            f.getTitle(), earned, total, pct);
                    if (cert != null) msg += "\n\n📜 Certificat généré: " + cert;
                }

                showInfo(msg);
                loadMyFormations();
            });

        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}