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

/**
 * Toutes les formations disponibles.
 * Implémente PatientController pour que PatientHome.loadSubPage() injecte l'utilisateur.
 */
public class PatientFormationsController implements Initializable, PatientController {

    @FXML private VBox rootContainer;
    @FXML private TextField tfSearch;
    @FXML private FlowPane cardsPane;
    @FXML private Label lblCount;

    private FormationService formationService;
    private ParticipantService participantService;
    private QuizService quizService;
    private QuizResultService quizResultService;

    private User currentUser;
    private int currentUserId = 1;

    // ── Called by PatientHome.loadSubPage() ──────────────────────────────
    @Override
    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) this.currentUserId = user.getId_user();
        loadFormations();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService   = new FormationService();
        participantService = new ParticipantService();
        quizService        = new QuizService();
        quizResultService  = new QuizResultService();
        // Data loads in setUser() once the user is injected
    }

    // ════════════════════════════════════════════════════════════════════
    //  LOAD ALL FORMATIONS
    // ════════════════════════════════════════════════════════════════════

    private void loadFormations() {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();
        try {
            List<Formation> formations = formationService.selectALL();

            if (tfSearch != null) {
                tfSearch.textProperty().addListener((obs, o, n) -> filterFormations(formations, n));
            }

            for (Formation f : formations) {
                cardsPane.getChildren().add(createCard(f));
            }

            if (lblCount != null)
                lblCount.setText(formations.size() + " formations disponibles");

            if (formations.isEmpty()) {
                Label empty = new Label("Aucune formation disponible pour le moment.");
                empty.setStyle("-fx-font-size:15px;-fx-text-fill:#A0AEC0;-fx-padding:40;");
                cardsPane.getChildren().add(empty);
            }
        } catch (SQLException e) {
            cardsPane.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }

    private void filterFormations(List<Formation> all, String keyword) {
        cardsPane.getChildren().clear();
        String kw = keyword == null ? "" : keyword.toLowerCase().trim();
        int count = 0;
        for (Formation f : all) {
            if (kw.isEmpty()
                    || f.getTitle().toLowerCase().contains(kw)
                    || f.getCategory().toLowerCase().contains(kw)
                    || f.getDescription().toLowerCase().contains(kw)) {
                cardsPane.getChildren().add(createCard(f));
                count++;
            }
        }
        if (lblCount != null) lblCount.setText(count + " formations trouvées");
        if (count == 0) {
            Label empty = new Label("Aucune formation trouvée pour : " + keyword);
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#A0AEC0;-fx-padding:30;");
            cardsPane.getChildren().add(empty);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  CARD
    // ════════════════════════════════════════════════════════════════════

    private VBox createCard(Formation f) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");
        card.setPadding(new Insets(20));

        // Category badge
        Label lblCat = new Label(f.getCategory() != null ? f.getCategory() : "—");
        lblCat.setStyle("-fx-background-color:#E8956D;-fx-text-fill:white;"
                + "-fx-padding:3 10;-fx-background-radius:10;-fx-font-size:11px;");

        Label lblTitle = new Label(f.getTitle());
        lblTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2d3436;");
        lblTitle.setWrapText(true);

        Label lblDesc = new Label(f.getDescription());
        lblDesc.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(50);

        // Coach name if available
        if (f.getCoachId() > 0) {
            Label lblCoach = new Label("👤 Coach #" + f.getCoachId());
            lblCoach.setStyle("-fx-text-fill:#4A6FA5;-fx-font-size:11px;");
            card.getChildren().addAll(lblCat, lblTitle, lblDesc, lblCoach);
        } else {
            card.getChildren().addAll(lblCat, lblTitle, lblDesc);
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);

        boolean enrolled = isEnrolled(f.getId());

        if (!enrolled) {
            Button btnReg = new Button("✅ S'inscrire");
            btnReg.setStyle("-fx-background-color:#00b894;-fx-text-fill:white;"
                    + "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 14;");
            btnReg.setOnAction(e -> {
                registerToFormation(f);
                loadFormations(); // refresh cards
            });
            buttons.getChildren().add(btnReg);
        } else {
            Label enrolledLbl = new Label("✅ Inscrit");
            enrolledLbl.setStyle("-fx-text-fill:#00b894;-fx-font-weight:bold;-fx-font-size:12px;");
            buttons.getChildren().add(enrolledLbl);
        }

        // Video button — always visible if video exists
        if (f.getVideoUrl() != null && !f.getVideoUrl().trim().isEmpty()) {
            Button btnVideo = new Button("▶ Vidéo");
            btnVideo.setStyle("-fx-background-color:#6c5ce7;-fx-text-fill:white;"
                    + "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 12;");
            btnVideo.setOnAction(e -> watchVideo(f));
            buttons.getChildren().add(btnVideo);
        }

        // Quiz button — only if enrolled and quiz exists
        if (enrolled) {
            try {
                Quiz quiz = quizService.selectByFormation(f.getId());
                if (quiz != null) {
                    boolean passed = quizResultService.hasUserPassedQuiz(currentUserId, quiz.getId());
                    Button btnQuiz = new Button(passed ? "✅ Quiz réussi" : "📝 Passer le Quiz");
                    btnQuiz.setStyle("-fx-background-color:" + (passed ? "#00b894" : "#E8956D")
                            + ";-fx-text-fill:white;-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 12;");
                    if (!passed) {
                        final Quiz fq = quiz;
                        btnQuiz.setOnAction(e -> takeQuiz(f, fq));
                    }
                    buttons.getChildren().add(btnQuiz);
                }
            } catch (SQLException ignored) {}
        }

        card.getChildren().add(buttons);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    //  ENROLL
    // ════════════════════════════════════════════════════════════════════

    private boolean isEnrolled(int formationId) {
        try { return participantService.isAlreadyRegistered(currentUserId, formationId); }
        catch (SQLException e) { return false; }
    }

    private void registerToFormation(Formation f) {
        try {
            if (participantService.isAlreadyRegistered(currentUserId, f.getId())) {
                showInfo("Vous êtes déjà inscrit à cette formation!");
                return;
            }
            Participant p = new Participant();
            p.setUserId(currentUserId);
            p.setFormationId(f.getId());
            participantService.insertOne(p);
            showInfo("Inscription réussie à : " + f.getTitle() + " ✅");
        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  VIDEO PLAYER
    // ════════════════════════════════════════════════════════════════════

    private void watchVideo(Formation f) {
        String videoUrl = f.getVideoUrl();
        if (videoUrl == null || videoUrl.trim().isEmpty()) { showInfo("Aucune vidéo disponible."); return; }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🎬 " + f.getTitle());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(720, 520);

        VBox container;
        if (VideoPlayerUtil.isYouTubeUrl(videoUrl)) {
            container = VideoPlayerUtil.createYouTubePlayer(videoUrl);
        } else {
            File file = new File(videoUrl);
            if (file.exists() && videoUrl.toLowerCase().endsWith(".mp4"))
                container = VideoPlayerUtil.createLocalPlayer(videoUrl);
            else
                container = VideoPlayerUtil.createErrorMessage("Fichier non trouvé", videoUrl);
        }
        dialog.getDialogPane().setContent(container);
        dialog.setOnCloseRequest(e -> VideoPlayerUtil.stopMedia(container));
        dialog.showAndWait();
    }

    // ════════════════════════════════════════════════════════════════════
    //  QUIZ
    // ════════════════════════════════════════════════════════════════════

    private void takeQuiz(Formation f, Quiz quiz) {
        try {
            QuestionService questionService = new QuestionService();
            ReponseService  reponseService  = new ReponseService();
            List<Question> questions = questionService.selectByQuiz(quiz.getId());
            if (questions.isEmpty()) { showInfo("Ce quiz n'a pas encore de questions."); return; }

            Dialog<int[]> dialog = new Dialog<>();
            dialog.setTitle("📝 Quiz : " + quiz.getTitle());
            ButtonType submitBtn = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(submitBtn, ButtonType.CANCEL);
            dialog.getDialogPane().setPrefWidth(620);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            Label header = new Label(quiz.getTitle() + " | Score minimum : " + quiz.getPassingScore() + "%");
            header.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2D3748;");
            content.getChildren().add(header);

            java.util.Map<Integer, ToggleGroup> groups = new java.util.HashMap<>();
            int num = 1;
            for (Question q : questions) {
                VBox qBox = new VBox(8);
                qBox.setStyle("-fx-background-color:#f8f9fa;-fx-padding:15;-fx-background-radius:8;");
                Label lblQ = new Label("Q" + num + ". " + q.getQuestionText() + " (" + q.getPoints() + " pts)");
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
                if (btn != submitBtn) return null;
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
                try { new QuizResultService().insertOne(result); }
                catch (SQLException e) { System.err.println("Save result error: " + e.getMessage()); }

                String msg = passed
                        ? String.format("🎉 RÉUSSI ! Score : %.0f%% (%d/%d pts)", pct, earned, total)
                        : String.format("❌ ÉCHOUÉ. Score : %.0f%% (%d/%d pts)\nMinimum requis : %d%%",
                        pct, earned, total, quiz.getPassingScore());

                if (passed) {
                    String name = currentUser != null
                            ? currentUser.getPrenom() + " " + currentUser.getNom()
                            : "Patient #" + currentUserId;
                    String cert = new CertificateService().generateCertificate(name, f.getTitle(), earned, total, pct);
                    if (cert != null) msg += "\n\n📜 Certificat généré : " + cert;
                }

                showInfo(msg);
                loadFormations(); // refresh badge
            });

        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
