package controllers;

import entities.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import services.*;
import utils.VideoPlayerUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

// ✅ FIX: explicit java.util imports — évite le conflit avec controllers.Map

public class PatientController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnAccueil;
    @FXML private Button btnAllFormations;
    @FXML private Button btnMyFormations;
    @FXML private Button btnMyResults;
    @FXML private Button btnMyStats;
    @FXML private Button btnJournal;
    @FXML private Button btnConsultation;
    @FXML private Label lblPatientName;

    private FormationService formationService;
    private ParticipantService participantService;
    private QuizService quizService;
    private QuestionService questionService;
    private ReponseService reponseService;
    private QuizResultService quizResultService;
    private CertificateService certificateService;
    private VBox currentVideoContainer = null;

    private Timeline quizTimeline;
    private int timeRemainingSeconds;
    private Label lblTimer;

    private int currentUserId = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        participantService = new ParticipantService();
        quizService = new QuizService();
        questionService = new QuestionService();
        reponseService = new ReponseService();
        quizResultService = new QuizResultService();
        certificateService = new CertificateService();
        showAccueil();
    }

    @FXML
    private void showAccueil() {
        setActiveButton(btnAccueil);
        stopCurrentVideo();
        stopQuizTimer();
        VBox page = new VBox(25);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(20));
        Label welcome = new Label("Bienvenue sur EchoCare!");
        welcome.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        Label subtitle = new Label("Votre plateforme de developpement en soft skills");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #636e72;");
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        try {
            List<Formation> allFormations = formationService.selectALL();
            List<Participant> myList = participantService.selectALL();
            long myCount = myList.stream().filter(p -> p.getUserId() == currentUserId).count();
            List<QuizResult> myResults = quizResultService.selectByUser(currentUserId);
            long passedCount = myResults.stream().filter(QuizResult::isPassed).count();
            String bestScore = myResults.isEmpty() ? "-" :
                    String.format("%.0f%%", myResults.stream().mapToDouble(QuizResult::getPercentage).max().orElse(0));
            statsRow.getChildren().addAll(
                    createStatCard("Formations Disponibles", String.valueOf(allFormations.size()), "#7fc8f8"),
                    createStatCard("Mes Formations", String.valueOf(myCount), "#e8a0bf"),
                    createStatCard("Quiz Reussis", String.valueOf(passedCount), "#a0e8af"),
                    createStatCard("Meilleur Score", bestScore, "#f8d07f")
            );
        } catch (SQLException e) {
            statsRow.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        Label actionsTitle = new Label("Actions Rapides");
        actionsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        actions.getChildren().addAll(
                createActionButton("Formations", "#7fc8f8", e -> showAllFormations()),
                createActionButton("Mes Formations", "#e8a0bf", e -> showMyFormations()),
                createActionButton("Resultats", "#a0e8af", e -> showMyResults())
        );
        page.getChildren().addAll(welcome, subtitle, statsRow, actionsTitle, actions);
        setContent(page);
    }

    private Button createActionButton(String text, String color,
                                      javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15 25;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btn.setOnAction(handler);
        return btn;
    }

    private VBox createStatCard(String label, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(170);
        card.setPrefHeight(110);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");
        card.setPadding(new Insets(15));
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        lblLabel.setAlignment(Pos.CENTER);
        card.getChildren().addAll(lblValue, lblLabel);
        return card;
    }

    @FXML
    private void showAllFormations() {
        setActiveButton(btnAllFormations);
        stopCurrentVideo();
        stopQuizTimer();
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().add(createTitle("Toutes les Formations"));
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher une formation...");
        searchField.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; " +
                "-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 15;");
        searchField.setMaxWidth(400);
        try {
            List<Formation> formations = formationService.selectALL();
            FlowPane cards = new FlowPane(15, 15);
            cards.setPadding(new Insets(10));
            for (Formation f : formations) {
                cards.getChildren().add(createFormationCard(f, isEnrolled(f.getId())));
            }
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                cards.getChildren().clear();
                String kw = newVal.toLowerCase().trim();
                for (Formation f : formations) {
                    if (kw.isEmpty() || f.getTitle().toLowerCase().contains(kw)
                            || f.getCategory().toLowerCase().contains(kw)) {
                        cards.getChildren().add(createFormationCard(f, isEnrolled(f.getId())));
                    }
                }
                if (cards.getChildren().isEmpty())
                    cards.getChildren().add(new Label("Aucune formation trouvee pour: " + kw));
            });
            if (formations.isEmpty())
                cards.getChildren().add(new Label("Aucune formation disponible"));
            ScrollPane scroll = new ScrollPane(cards);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            page.getChildren().addAll(searchField, scroll);
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    private boolean isEnrolled(int formationId) {
        try { return participantService.isAlreadyRegistered(currentUserId, formationId); }
        catch (SQLException e) { return false; }
    }

    @FXML
    private void showMyFormations() {
        setActiveButton(btnMyFormations);
        stopCurrentVideo();
        stopQuizTimer();
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().add(createTitle("Mes Formations"));
        try {
            List<Participant> myList = participantService.selectALL();
            List<Formation> allFormations = formationService.selectALL();
            FlowPane cards = new FlowPane(15, 15);
            cards.setPadding(new Insets(10));
            for (Participant p : myList) {
                if (p.getUserId() == currentUserId) {
                    for (Formation f : allFormations) {
                        if (f.getId() == p.getFormationId())
                            cards.getChildren().add(createFormationCard(f, true));
                    }
                }
            }
            if (cards.getChildren().isEmpty()) {
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(50));
                Label emptyText = new Label("Vous n'etes inscrit a aucune formation");
                emptyText.setStyle("-fx-font-size: 16px; -fx-text-fill: #636e72;");
                Button goBtn = new Button("Voir les formations disponibles");
                goBtn.setOnAction(e -> showAllFormations());
                emptyBox.getChildren().addAll(emptyText, goBtn);
                cards.getChildren().add(emptyBox);
            }
            ScrollPane scroll = new ScrollPane(cards);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            page.getChildren().add(scroll);
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    @FXML
    private void showMyResults() {
        setActiveButton(btnMyResults);
        stopCurrentVideo();
        stopQuizTimer();
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().add(createTitle("Mes Resultats de Quiz"));
        try {
            List<QuizResult> results = quizResultService.selectByUser(currentUserId);
            if (results.isEmpty()) {
                Label emptyText = new Label("Aucun resultat. Passez un quiz pour commencer!");
                emptyText.setStyle("-fx-font-size: 16px; -fx-text-fill: #636e72;");
                page.getChildren().add(emptyText);
            } else {
                VBox resultsList = new VBox(10);
                for (QuizResult r : results) resultsList.getChildren().add(createResultCard(r));
                ScrollPane scroll = new ScrollPane(resultsList);
                scroll.setFitToWidth(true);
                scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                VBox.setVgrow(scroll, Priority.ALWAYS);
                page.getChildren().add(scroll);
            }
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    private HBox createResultCard(QuizResult r) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        Label icon = new Label(r.isPassed() ? "OK" : "X");
        icon.setStyle("-fx-font-size: 20px;");
        String quizName = "Quiz #" + r.getQuizId();
        try {
            List<Quiz> allQuizzes = quizService.selectALL();
            for (Quiz q : allQuizzes) {
                if (q.getId() == r.getQuizId()) { quizName = q.getTitle(); break; }
            }
        } catch (SQLException ignored) {}
        VBox info = new VBox(3);
        Label lblQuiz = new Label(quizName);
        lblQuiz.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
        Label lblDate = new Label(r.getCompletedAt() != null
                ? r.getCompletedAt().toString().substring(0, 16) : "N/A");
        lblDate.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 11px;");
        info.getChildren().addAll(lblQuiz, lblDate);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox scoreBox = new VBox(3);
        scoreBox.setAlignment(Pos.CENTER);
        Label lblScore = new Label(String.format("%.0f%%", r.getPercentage()));
        lblScore.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: "
                + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        Label lblStatus = new Label(r.isPassed() ? "REUSSI" : "ECHOUE");
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: "
                + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        scoreBox.getChildren().addAll(lblScore, lblStatus);
        card.getChildren().addAll(icon, info, spacer, scoreBox);
        return card;
    }

    private VBox createFormationCard(Formation f, boolean enrolled) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        card.setPadding(new Insets(20));
        Label lblCat = new Label(f.getCategory());
        lblCat.setStyle("-fx-background-color: #e8a0bf; -fx-text-fill: white;" +
                "-fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 11px;");
        Label lblTitle = new Label(f.getTitle());
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        lblTitle.setWrapText(true);
        Label lblDesc = new Label(f.getDescription());
        lblDesc.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
        lblDesc.setWrapText(true);
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        Button btnWatch = new Button("Voir");
        btnWatch.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white; -fx-cursor: hand;");
        btnWatch.setOnAction(e -> watchVideo(f));
        buttons.getChildren().add(btnWatch);
        if (enrolled) {
            try {
                Quiz quiz = quizService.selectByFormation(f.getId());
                if (quiz != null) {
                    boolean passed = quizResultService.hasUserPassedQuiz(currentUserId, quiz.getId());
                    Button btnQuiz = new Button(passed ? "Reussi" : "Quiz");
                    btnQuiz.setStyle("-fx-background-color: " + (passed ? "#00b894" : "#e8a0bf")
                            + "; -fx-text-fill: white; -fx-cursor: hand;");
                    btnQuiz.setOnAction(e -> takeQuiz(f));
                    buttons.getChildren().add(btnQuiz);
                }
            } catch (SQLException ignored) {}
        } else {
            Button btnReg = new Button("S'inscrire");
            btnReg.setStyle("-fx-background-color: #a0e8af; -fx-text-fill: white; -fx-cursor: hand;");
            btnReg.setOnAction(e -> registerToFormation(f));
            buttons.getChildren().add(btnReg);
        }
        card.getChildren().addAll(lblCat, lblTitle, lblDesc, buttons);
        return card;
    }

    private void watchVideo(Formation f) {
        stopCurrentVideo();
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        Button btnBack = new Button("Retour");
        btnBack.setOnAction(e -> { stopCurrentVideo(); showAllFormations(); });
        Label title = new Label(f.getTitle());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        String videoUrl = f.getVideoUrl();
        VBox playerContainer;
        if (videoUrl == null || videoUrl.trim().isEmpty())
            playerContainer = VideoPlayerUtil.createNoVideoMessage();
        else if (VideoPlayerUtil.isYouTubeUrl(videoUrl))
            playerContainer = VideoPlayerUtil.createYouTubePlayer(videoUrl);
        else {
            File file = new File(videoUrl);
            if (!file.exists()) playerContainer = VideoPlayerUtil.createErrorMessage("Fichier non trouve", videoUrl);
            else if (videoUrl.toLowerCase().endsWith(".mp4")) playerContainer = VideoPlayerUtil.createLocalPlayer(videoUrl);
            else playerContainer = VideoPlayerUtil.createWebViewLocalPlayer(videoUrl);
        }
        currentVideoContainer = playerContainer;
        VBox.setVgrow(playerContainer, Priority.ALWAYS);
        page.getChildren().addAll(btnBack, title, playerContainer);
        setContent(page);
    }

    private void registerToFormation(Formation f) {
        try {
            if (participantService.isAlreadyRegistered(currentUserId, f.getId())) {
                showInfo("Vous etes deja inscrit a cette formation!");
                return;
            }
            Participant p = new Participant();
            p.setUserId(currentUserId);
            p.setFormationId(f.getId());
            participantService.insertOne(p);
            showInfo("Inscription reussie!");
            showAllFormations();
        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    private void takeQuiz(Formation f) {
        stopCurrentVideo();
        stopQuizTimer();
        try {
            Quiz quiz = quizService.selectByFormation(f.getId());
            if (quiz == null) { showInfo("Aucun quiz disponible."); return; }
            List<Question> questions = questionService.selectByQuiz(quiz.getId());
            if (questions.isEmpty()) { showInfo("Ce quiz n'a pas de questions."); return; }
            int totalTimeSeconds = questions.size() * 120;
            timeRemainingSeconds = totalTimeSeconds;
            VBox page = new VBox(15);
            page.setPadding(new Insets(20));
            HBox topBar = new HBox(15);
            topBar.setAlignment(Pos.CENTER_LEFT);
            Button btnBack = new Button("Annuler");
            btnBack.setOnAction(e -> { stopQuizTimer(); showMyFormations(); });
            HBox spacerTop = new HBox();
            HBox.setHgrow(spacerTop, Priority.ALWAYS);
            lblTimer = new Label(formatTime(timeRemainingSeconds));
            lblTimer.setStyle("-fx-text-fill: #00b894; -fx-font-size: 18px; -fx-font-weight: bold;");
            topBar.getChildren().addAll(btnBack, spacerTop, lblTimer);
            Label title = new Label("Quiz: " + quiz.getTitle());
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
            ProgressBar progressBar = new ProgressBar(1.0);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.setStyle("-fx-accent: #00b894;");
            VBox questionsBox = new VBox(15);
            // ✅ FIX: explicit java.util.Map — not controllers.Map
            java.util.Map<Integer, ToggleGroup> answerGroups = new HashMap<>();
            int num = 1;
            for (Question q : questions) {
                VBox qBox = new VBox(10);
                qBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18;");
                Label lblQ = new Label("Q" + num + ". " + q.getQuestionText() + " (" + q.getPoints() + " pts)");
                lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                lblQ.setWrapText(true);
                ToggleGroup group = new ToggleGroup();
                answerGroups.put(q.getId(), group);
                VBox optionsBox = new VBox(8);
                optionsBox.setPadding(new Insets(5, 0, 0, 10));
                for (Reponse r : reponseService.selectByQuestion(q.getId())) {
                    RadioButton rb = new RadioButton(r.getOptionText());
                    rb.setToggleGroup(group);
                    rb.setUserData(r);
                    rb.setStyle("-fx-font-size: 13px;");
                    rb.setWrapText(true);
                    optionsBox.getChildren().add(rb);
                }
                qBox.getChildren().addAll(lblQ, optionsBox);
                questionsBox.getChildren().add(qBox);
                num++;
            }
            Button btnSubmit = new Button("Soumettre le Quiz");
            btnSubmit.setStyle("-fx-background-color: #00b894; -fx-text-fill: white;" +
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 30; -fx-cursor: hand;");
            btnSubmit.setOnAction(e -> { stopQuizTimer(); submitQuiz(f, quiz, questions, answerGroups); });
            HBox submitRow = new HBox(btnSubmit);
            submitRow.setAlignment(Pos.CENTER);
            VBox all = new VBox(15, topBar, title, progressBar, new Separator(), questionsBox, submitRow);
            all.setPadding(new Insets(10));
            ScrollPane scroll = new ScrollPane(all);
            scroll.setFitToWidth(true);
            contentArea.getChildren().setAll(scroll);
            quizTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                timeRemainingSeconds--;
                lblTimer.setText(formatTime(timeRemainingSeconds));
                progressBar.setProgress((double) timeRemainingSeconds / totalTimeSeconds);
                if (timeRemainingSeconds <= 30)
                    lblTimer.setStyle("-fx-text-fill: #d63031; -fx-font-size: 18px; -fx-font-weight: bold;");
                else if (timeRemainingSeconds <= 60)
                    lblTimer.setStyle("-fx-text-fill: #fdcb6e; -fx-font-size: 18px; -fx-font-weight: bold;");
                if (timeRemainingSeconds <= 0) {
                    stopQuizTimer();
                    showInfo("Temps ecoule! Quiz soumis automatiquement.");
                    submitQuiz(f, quiz, questions, answerGroups);
                }
            }));
            quizTimeline.setCycleCount(totalTimeSeconds);
            quizTimeline.play();
        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    private String formatTime(int totalSeconds) {
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void stopQuizTimer() {
        if (quizTimeline != null) { quizTimeline.stop(); quizTimeline = null; }
    }

    private void submitQuiz(Formation f, Quiz quiz, List<Question> questions,
                            java.util.Map<Integer, ToggleGroup> answerGroups) {
        stopQuizTimer();
        boolean allAnswered = questions.stream()
                .allMatch(q -> answerGroups.get(q.getId()).getSelectedToggle() != null);
        if (!allAnswered && timeRemainingSeconds > 0) {
            showInfo("Veuillez repondre a toutes les questions!");
            return;
        }
        int totalPoints = 0, earnedPoints = 0;
        for (Question q : questions) {
            totalPoints += q.getPoints();
            ToggleGroup group = answerGroups.get(q.getId());
            if (group.getSelectedToggle() != null) {
                Reponse chosen = (Reponse) ((RadioButton) group.getSelectedToggle()).getUserData();
                if (chosen.isCorrect()) earnedPoints += q.getPoints();
            }
        }
        double percentage = totalPoints > 0 ? (earnedPoints * 100.0) / totalPoints : 0;
        boolean passed = percentage >= quiz.getPassingScore();
        QuizResult result = new QuizResult();
        result.setQuizId(quiz.getId());
        result.setUserId(currentUserId);
        result.setScore(earnedPoints);
        result.setTotalPoints(totalPoints);
        result.setPassed(passed);
        try { quizResultService.insertOne(result); }
        catch (SQLException e) { showInfo("Erreur sauvegarde: " + e.getMessage()); return; }
        if (passed) {
            String certPath = certificateService.generateCertificate(
                    "Patient #" + currentUserId, f.getTitle(), earnedPoints, totalPoints, percentage);
            if (certPath != null) {
                Alert certAlert = new Alert(Alert.AlertType.INFORMATION);
                certAlert.setTitle("Certificat Genere!");
                certAlert.setContentText("Certificat PDF sauvegarde:\n" + certPath);
                ButtonType openBtn = new ButtonType("Ouvrir PDF");
                ButtonType laterBtn = new ButtonType("Plus tard", ButtonBar.ButtonData.CANCEL_CLOSE);
                certAlert.getButtonTypes().setAll(openBtn, laterBtn);
                Optional<ButtonType> certResult = certAlert.showAndWait();
                if (certResult.isPresent() && certResult.get() == openBtn) {
                    try { java.awt.Desktop.getDesktop().open(new File(certPath)); }
                    catch (Exception ex) { System.err.println("Cannot open PDF: " + ex.getMessage()); }
                }
            }
        }
        showQuizResult(f, quiz, questions, answerGroups, earnedPoints, totalPoints, percentage, passed);
    }

    private void showQuizResult(Formation f, Quiz quiz, List<Question> questions,
                                java.util.Map<Integer, ToggleGroup> answerGroups,
                                int earned, int total, double percentage, boolean passed) {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));
        page.setAlignment(Pos.TOP_CENTER);
        Label lblResult = new Label(passed ? "QUIZ REUSSI!" : "QUIZ ECHOUE");
        lblResult.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: "
                + (passed ? "#00b894" : "#d63031") + ";");
        Label lblScore = new Label(String.format("%.0f%%", percentage));
        lblScore.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: "
                + (passed ? "#00b894" : "#d63031") + ";");
        Label lblDetail = new Label(earned + " / " + total + " points | Minimum: " + quiz.getPassingScore() + "%");
        lblDetail.setStyle("-fx-text-fill: #636e72; -fx-font-size: 14px;");
        VBox headerBox = new VBox(10, lblResult, lblScore, lblDetail);
        headerBox.setAlignment(Pos.CENTER);
        Label lblReview = new Label("Revision des reponses:");
        lblReview.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        VBox reviewBox = new VBox(10);
        int num = 1;
        for (Question q : questions) {
            ToggleGroup group = answerGroups.get(q.getId());
            VBox qReview = new VBox(5);
            boolean answeredCorrectly = false;
            String yourAnswer = "Non repondu";
            if (group.getSelectedToggle() != null) {
                Reponse chosen = (Reponse) ((RadioButton) group.getSelectedToggle()).getUserData();
                yourAnswer = chosen.getOptionText();
                answeredCorrectly = chosen.isCorrect();
            }
            qReview.setStyle("-fx-background-color: " + (answeredCorrectly ? "#f0fff0" : "#fff0f0")
                    + "; -fx-padding: 12; -fx-background-radius: 8;");
            Label lblQ = new Label("Q" + num + ": " + q.getQuestionText());
            lblQ.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
            lblQ.setWrapText(true);
            Label lblYour = new Label("Votre reponse: " + yourAnswer);
            lblYour.setStyle("-fx-text-fill: " + (answeredCorrectly ? "#00b894" : "#d63031") + ";");
            lblYour.setWrapText(true);
            qReview.getChildren().addAll(lblQ, lblYour);
            if (!answeredCorrectly) {
                try {
                    for (Reponse r : reponseService.selectByQuestion(q.getId())) {
                        if (r.isCorrect()) {
                            Label lblCorrect = new Label("Bonne reponse: " + r.getOptionText());
                            lblCorrect.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                            lblCorrect.setWrapText(true);
                            qReview.getChildren().add(lblCorrect);
                            break;
                        }
                    }
                } catch (SQLException ignored) {}
            }
            reviewBox.getChildren().add(qReview);
            num++;
        }
        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);
        Button btnRetry = new Button("Reessayer");
        btnRetry.setOnAction(e -> takeQuiz(f));
        Button btnHome = new Button("Accueil");
        btnHome.setOnAction(e -> showAccueil());
        Button btnResultsBtn = new Button("Mes resultats");
        btnResultsBtn.setOnAction(e -> showMyResults());
        buttons.getChildren().addAll(btnRetry, btnHome, btnResultsBtn);
        VBox all = new VBox(15, headerBox, new Separator(), lblReview, reviewBox, buttons);
        all.setAlignment(Pos.TOP_CENTER);
        all.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(all);
        scroll.setFitToWidth(true);
        contentArea.getChildren().setAll(scroll);
    }

    @FXML private void showJournal() { setActiveButton(btnJournal); showComingSoon("Mon Journal"); }
    @FXML private void showConsultation() { setActiveButton(btnConsultation); showComingSoon("Mes Consultations"); }

    @FXML
    private void switchToAdmin() {
        stopCurrentVideo(); stopQuizTimer();
        try { Parent root = FXMLLoader.load(getClass().getResource("/AdminDashboard.fxml")); contentArea.getScene().setRoot(root); }
        catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void switchToCoach() {
        stopCurrentVideo(); stopQuizTimer();
        try { Parent root = FXMLLoader.load(getClass().getResource("/CoachDashboard.fxml")); contentArea.getScene().setRoot(root); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void stopCurrentVideo() {
        if (currentVideoContainer != null) { VideoPlayerUtil.stopMedia(currentVideoContainer); currentVideoContainer = null; }
    }

    private void setContent(VBox page) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    private Label createTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        return lbl;
    }

    private void setActiveButton(Button btn) {
        Button[] all = {btnAccueil, btnAllFormations, btnMyFormations, btnMyResults, btnMyStats, btnJournal, btnConsultation};
        for (Button b : all) if (b != null) b.getStyleClass().remove("sidebar-btn-active");
        if (btn != null) btn.getStyleClass().add("sidebar-btn-active");
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showComingSoon(String msg) {
        VBox page = new VBox(20);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(50));
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #636e72;");
        page.getChildren().add(lbl);
        setContent(page);
    }

    @FXML
    private void handleLogout() {
        stopCurrentVideo(); stopQuizTimer(); System.exit(0);
    }
}