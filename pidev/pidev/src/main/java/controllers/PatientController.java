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
import java.util.*;

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

    // ═══════════════════════════════
    //  QUIZ TIMER FIELDS (Métier Avancé)
    // ═══════════════════════════════
    private Timeline quizTimeline;
    private int timeRemainingSeconds;
    private Label lblTimer;

    private int currentUserId = 1; // Will come from User module after integration

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

    // ═══════════════════════════════
    //  ACCUEIL
    // ═══════════════════════════════
    @FXML
    private void showAccueil() {
        setActiveButton(btnAccueil);
        stopCurrentVideo();
        stopQuizTimer();

        VBox page = new VBox(25);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(20));

        Label welcome = new Label("Bienvenue sur EchoCare! 👋");
        welcome.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        Label subtitle = new Label("Votre plateforme de développement en soft skills");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #636e72;");

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);

        try {
            List<Formation> allFormations = formationService.selectALL();
            List<Participant> myList = participantService.selectALL();
            long myCount = myList.stream()
                    .filter(p -> p.getUserId() == currentUserId).count();
            List<QuizResult> myResults = quizResultService.selectByUser(currentUserId);
            long passedCount = myResults.stream().filter(QuizResult::isPassed).count();
            String bestScore = myResults.isEmpty() ? "—" :
                    String.format("%.0f%%", myResults.stream()
                            .mapToDouble(QuizResult::getPercentage).max().orElse(0));

            statsRow.getChildren().addAll(
                    createStatCard("📚", "Formations\nDisponibles",
                            String.valueOf(allFormations.size()), "#7fc8f8"),
                    createStatCard("📖", "Mes\nFormations",
                            String.valueOf(myCount), "#e8a0bf"),
                    createStatCard("📝", "Quiz\nRéussis",
                            String.valueOf(passedCount), "#a0e8af"),
                    createStatCard("⭐", "Meilleur\nScore", bestScore, "#f8d07f")
            );
        } catch (SQLException e) {
            statsRow.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        Label actionsTitle = new Label("Actions Rapides");
        actionsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);

        actions.getChildren().addAll(
                createActionButton("📚 Formations", "#7fc8f8", e -> showAllFormations()),
                createActionButton("📖 Mes Formations", "#e8a0bf", e -> showMyFormations()),
                createActionButton("📊 Résultats", "#a0e8af", e -> showMyResults())
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

    private VBox createStatCard(String icon, String label, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(170);
        card.setPrefHeight(110);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        card.setPadding(new Insets(15));

        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size: 24px;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        lblLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lblIcon, lblValue, lblLabel);
        return card;
    }

    // ═══════════════════════════════
    //  ALL FORMATIONS
    // ═══════════════════════════════
    @FXML
    private void showAllFormations() {
        setActiveButton(btnAllFormations);
        stopCurrentVideo();
        stopQuizTimer();

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().add(createTitle("📚 Toutes les Formations"));

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher une formation...");
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
                    if (kw.isEmpty() || f.getTitle().toLowerCase().contains(kw) ||
                            f.getCategory().toLowerCase().contains(kw) ||
                            f.getDescription().toLowerCase().contains(kw)) {
                        cards.getChildren().add(createFormationCard(f, isEnrolled(f.getId())));
                    }
                }
                if (cards.getChildren().isEmpty()) {
                    Label noResult = new Label("Aucune formation trouvée pour: " + kw);
                    noResult.setStyle("-fx-text-fill: #636e72;");
                    cards.getChildren().add(noResult);
                }
            });

            if (formations.isEmpty()) {
                Label empty = new Label("Aucune formation disponible");
                empty.setStyle("-fx-text-fill: #636e72;");
                cards.getChildren().add(empty);
            }

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
        try {
            return participantService.isAlreadyRegistered(currentUserId, formationId);
        } catch (SQLException e) {
            return false;
        }
    }

    // ═══════════════════════════════
    //  MY FORMATIONS
    // ═══════════════════════════════
    @FXML
    private void showMyFormations() {
        setActiveButton(btnMyFormations);
        stopCurrentVideo();
        stopQuizTimer();

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().add(createTitle("📖 Mes Formations"));

        try {
            List<Participant> myList = participantService.selectALL();
            List<Formation> allFormations = formationService.selectALL();
            FlowPane cards = new FlowPane(15, 15);
            cards.setPadding(new Insets(10));

            for (Participant p : myList) {
                if (p.getUserId() == currentUserId) {
                    for (Formation f : allFormations) {
                        if (f.getId() == p.getFormationId()) {
                            cards.getChildren().add(createFormationCard(f, true));
                        }
                    }
                }
            }

            if (cards.getChildren().isEmpty()) {
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(50));
                Label emptyIcon = new Label("📭");
                emptyIcon.setStyle("-fx-font-size: 50px;");
                Label emptyText = new Label("Vous n'êtes inscrit à aucune formation");
                emptyText.setStyle("-fx-font-size: 16px; -fx-text-fill: #636e72;");
                Button goBtn = new Button("📚 Voir les formations disponibles");
                goBtn.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 10; " +
                        "-fx-cursor: hand;");
                goBtn.setOnAction(e -> showAllFormations());
                emptyBox.getChildren().addAll(emptyIcon, emptyText, goBtn);
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

    // ═══════════════════════════════
    //  MY RESULTS
    // ═══════════════════════════════
    @FXML
    private void showMyResults() {
        setActiveButton(btnMyResults);
        stopCurrentVideo();
        stopQuizTimer();

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().add(createTitle("📊 Mes Résultats de Quiz"));

        try {
            List<QuizResult> results = quizResultService.selectByUser(currentUserId);

            if (results.isEmpty()) {
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(50));
                Label emptyIcon = new Label("📝");
                emptyIcon.setStyle("-fx-font-size: 50px;");
                Label emptyText = new Label("Aucun résultat. Passez un quiz pour commencer!");
                emptyText.setStyle("-fx-font-size: 16px; -fx-text-fill: #636e72;");
                emptyBox.getChildren().addAll(emptyIcon, emptyText);
                page.getChildren().add(emptyBox);
            } else {
                VBox resultsList = new VBox(10);
                for (QuizResult r : results) {
                    resultsList.getChildren().add(createResultCard(r));
                }
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
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        Label icon = new Label(r.isPassed() ? "✅" : "❌");
        icon.setStyle("-fx-font-size: 30px;");

        String quizName = "Quiz #" + r.getQuizId();
        String formationName = "";
        try {
            List<Quiz> allQuizzes = quizService.selectALL();
            for (Quiz q : allQuizzes) {
                if (q.getId() == r.getQuizId()) {
                    quizName = q.getTitle();
                    List<Formation> formations = formationService.selectALL();
                    for (Formation f : formations) {
                        if (f.getId() == q.getFormationId()) {
                            formationName = f.getTitle();
                            break;
                        }
                    }
                    break;
                }
            }
        } catch (SQLException ignored) {}

        VBox info = new VBox(3);
        Label lblQuiz = new Label(quizName);
        lblQuiz.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
        Label lblFormation = new Label("Formation: " + formationName);
        lblFormation.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
        Label lblDate = new Label("Date: " + (r.getCompletedAt() != null ?
                r.getCompletedAt().toString().substring(0, 16) : "N/A"));
        lblDate.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 11px;");
        info.getChildren().addAll(lblQuiz, lblFormation, lblDate);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox scoreBox = new VBox(3);
        scoreBox.setAlignment(Pos.CENTER);
        Label lblScore = new Label(String.format("%.0f%%", r.getPercentage()));
        lblScore.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " +
                (r.isPassed() ? "#00b894" : "#d63031") + ";");
        Label lblDetail = new Label(r.getScore() + "/" + r.getTotalPoints() + " pts");
        lblDetail.setStyle("-fx-text-fill: #636e72;");
        Label lblStatus = new Label(r.isPassed() ? "RÉUSSI" : "ÉCHOUÉ");
        lblStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " +
                (r.isPassed() ? "#00b894" : "#d63031") + ";");
        scoreBox.getChildren().addAll(lblScore, lblDetail, lblStatus);

        card.getChildren().addAll(icon, info, spacer, scoreBox);
        return card;
    }

    // ═══════════════════════════════
    //  FORMATION CARD
    // ═══════════════════════════════
    private VBox createFormationCard(Formation f, boolean enrolled) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 2);");
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
        lblDesc.setMaxHeight(60);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button btnWatch = new Button("▶️ Voir");
        btnWatch.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white;" +
                "-fx-cursor: hand; -fx-background-radius: 5;");
        btnWatch.setOnAction(e -> watchVideo(f));
        buttons.getChildren().add(btnWatch);

        if (enrolled) {
            try {
                Quiz quiz = quizService.selectByFormation(f.getId());
                if (quiz != null) {
                    boolean passed = quizResultService.hasUserPassedQuiz(currentUserId, quiz.getId());
                    Button btnQuiz = new Button(passed ? "✅ Réussi" : "📝 Quiz");
                    btnQuiz.setStyle("-fx-background-color: " +
                            (passed ? "#00b894" : "#e8a0bf") +
                            "; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnQuiz.setOnAction(e -> takeQuiz(f));
                    buttons.getChildren().add(btnQuiz);
                }
            } catch (SQLException ignored) {}
        } else {
            Button btnReg = new Button("📥 S'inscrire");
            btnReg.setStyle("-fx-background-color: #a0e8af; -fx-text-fill: white;" +
                    "-fx-cursor: hand; -fx-background-radius: 5;");
            btnReg.setOnAction(e -> registerToFormation(f));
            buttons.getChildren().add(btnReg);
        }

        card.getChildren().addAll(lblCat, lblTitle, lblDesc, buttons);
        return card;
    }

    // ═══════════════════════════════
    //  WATCH VIDEO (FIXED YouTube)
    // ═══════════════════════════════
    private void watchVideo(Formation f) {
        stopCurrentVideo();
        stopQuizTimer();

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.setStyle("-fx-background-color: #fef0f5;");

        Button btnBack = new Button("↩️ Retour");
        btnBack.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white;" +
                "-fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
        btnBack.setOnAction(e -> {
            stopCurrentVideo();
            showAllFormations();
        });

        Label title = new Label("🎬 " + f.getTitle());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        // Create player based on URL type
        String videoUrl = f.getVideoUrl();
        VBox playerContainer;

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            playerContainer = VideoPlayerUtil.createNoVideoMessage();
        } else if (VideoPlayerUtil.isYouTubeUrl(videoUrl)) {
            // ═══ FIXED: YouTube player with proper sizing ═══
            playerContainer = VideoPlayerUtil.createYouTubePlayer(videoUrl);
        } else {
            File file = new File(videoUrl);
            if (!file.exists()) {
                playerContainer = VideoPlayerUtil.createErrorMessage("Fichier non trouvé", videoUrl);
            } else if (videoUrl.toLowerCase().endsWith(".mp4")) {
                playerContainer = VideoPlayerUtil.createLocalPlayer(videoUrl);
            } else {
                playerContainer = VideoPlayerUtil.createWebViewLocalPlayer(videoUrl);
            }
        }

        currentVideoContainer = playerContainer;
        VBox.setVgrow(playerContainer, Priority.ALWAYS);

        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #636e72;");

        // Actions after video
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        if (isEnrolled(f.getId())) {
            try {
                Quiz quiz = quizService.selectByFormation(f.getId());
                if (quiz != null) {
                    Button btnQuiz = new Button("📝 Passer le Quiz maintenant");
                    btnQuiz.setStyle("-fx-background-color: #e8a0bf; -fx-text-fill: white;" +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20;" +
                            "-fx-background-radius: 10; -fx-cursor: hand;");
                    btnQuiz.setOnAction(e -> {
                        stopCurrentVideo();
                        takeQuiz(f);
                    });
                    actions.getChildren().add(btnQuiz);
                }
            } catch (SQLException ignored) {}
        }

        page.getChildren().addAll(btnBack, title, playerContainer, desc, actions);
        setContent(page);
    }

    // ═══════════════════════════════
    //  REGISTER TO FORMATION
    // ═══════════════════════════════
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
            showInfo("Inscription réussie! ✅\nVous pouvez maintenant passer le quiz.");
            showAllFormations();
        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    //  TAKE QUIZ WITH TIMER (Métier Avancé)
    // ═══════════════════════════════════════════════
    private void takeQuiz(Formation f) {
        stopCurrentVideo();
        stopQuizTimer();

        try {
            Quiz quiz = quizService.selectByFormation(f.getId());
            if (quiz == null) {
                showInfo("Aucun quiz disponible pour cette formation.");
                return;
            }

            List<Question> questions = questionService.selectByQuiz(quiz.getId());
            if (questions.isEmpty()) {
                showInfo("Ce quiz n'a pas encore de questions.");
                return;
            }

            // ═══════════════════════════════════════
            //  QUIZ TIMER SETUP - 2 minutes per question
            // ═══════════════════════════════════════
            int totalTimeSeconds = questions.size() * 120; // 2 min per question
            timeRemainingSeconds = totalTimeSeconds;

            VBox page = new VBox(15);
            page.setPadding(new Insets(20));
            page.setStyle("-fx-background-color: #fef0f5;");

            // ── Top bar with back button and timer ──
            HBox topBar = new HBox(15);
            topBar.setAlignment(Pos.CENTER_LEFT);

            Button btnBack = new Button("↩️ Annuler");
            btnBack.setStyle("-fx-background-color: #636e72; -fx-text-fill: white;" +
                    "-fx-cursor: hand; -fx-background-radius: 5; -fx-font-weight: bold;");
            btnBack.setOnAction(e -> {
                stopQuizTimer();
                showMyFormations();
            });

            HBox spacerTop = new HBox();
            HBox.setHgrow(spacerTop, Priority.ALWAYS);

            // ── Timer display ──
            HBox timerBox = new HBox(8);
            timerBox.setAlignment(Pos.CENTER);
            timerBox.setPadding(new Insets(8, 15, 8, 15));
            timerBox.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 20;");

            Label timerIcon = new Label("⏱️");
            timerIcon.setStyle("-fx-font-size: 18px;");

            lblTimer = new Label(formatTimerTime(timeRemainingSeconds));
            lblTimer.setStyle("-fx-text-fill: #00b894; -fx-font-size: 18px; " +
                    "-fx-font-weight: bold; -fx-font-family: 'Courier New';");

            timerBox.getChildren().addAll(timerIcon, lblTimer);
            topBar.getChildren().addAll(btnBack, spacerTop, timerBox);

            // ── Quiz title and info ──
            Label title = new Label("📝 Quiz: " + quiz.getTitle());
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

            Label info = new Label("Formation: " + f.getTitle() +
                    " | Score minimum: " + quiz.getPassingScore() + "% | " +
                    questions.size() + " questions | Temps: " +
                    formatTimerTime(totalTimeSeconds));
            info.setStyle("-fx-text-fill: #636e72; -fx-font-size: 13px;");
            info.setWrapText(true);

            // ── Progress bar ──
            ProgressBar progressBar = new ProgressBar(1.0);
            progressBar.setPrefWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(8);
            progressBar.setStyle("-fx-accent: #00b894;");

            // ── Questions ──
            VBox questionsBox = new VBox(15);
            Map<Integer, ToggleGroup> answerGroups = new HashMap<>();

            int num = 1;
            for (Question q : questions) {
                VBox qBox = new VBox(10);
                qBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                        "-fx-padding: 18; -fx-effect: dropshadow(three-pass-box, " +
                        "rgba(0,0,0,0.05), 5, 0, 0, 2);");

                Label lblQ = new Label("Q" + num + ". " + q.getQuestionText() +
                        " (" + q.getPoints() + " pts)");
                lblQ.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
                lblQ.setWrapText(true);

                ToggleGroup group = new ToggleGroup();
                answerGroups.put(q.getId(), group);

                List<Reponse> reponses = reponseService.selectByQuestion(q.getId());
                VBox optionsBox = new VBox(8);
                optionsBox.setPadding(new Insets(5, 0, 0, 10));

                for (Reponse r : reponses) {
                    RadioButton rb = new RadioButton(r.getOptionText());
                    rb.setToggleGroup(group);
                    rb.setUserData(r);
                    rb.setStyle("-fx-font-size: 13px; -fx-text-fill: #2d3436;");
                    rb.setWrapText(true);
                    optionsBox.getChildren().add(rb);
                }

                qBox.getChildren().addAll(lblQ, optionsBox);
                questionsBox.getChildren().add(qBox);
                num++;
            }

            // ── Submit button ──
            Button btnSubmit = new Button("📤 Soumettre le Quiz");
            btnSubmit.setStyle("-fx-background-color: #00b894; -fx-text-fill: white;" +
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 30;" +
                    "-fx-background-radius: 10; -fx-cursor: hand;");
            btnSubmit.setOnAction(e -> {
                stopQuizTimer();
                submitQuiz(f, quiz, questions, answerGroups);
            });

            HBox submitRow = new HBox(btnSubmit);
            submitRow.setAlignment(Pos.CENTER);
            submitRow.setPadding(new Insets(10));

            VBox all = new VBox(15, topBar, title, info, progressBar,
                    new Separator(), questionsBox, submitRow);
            all.setPadding(new Insets(10));
            all.setStyle("-fx-background-color: #fef0f5;");

            ScrollPane scroll = new ScrollPane(all);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: #fef0f5; -fx-background-color: #fef0f5;");

            contentArea.getChildren().clear();
            contentArea.getChildren().add(scroll);

            // ═══════════════════════════════════════
            //  START THE TIMER (Métier Avancé)
            // ═══════════════════════════════════════
            quizTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                timeRemainingSeconds--;

                // Update timer label
                lblTimer.setText(formatTimerTime(timeRemainingSeconds));

                // Update progress bar
                double progress = (double) timeRemainingSeconds / totalTimeSeconds;
                progressBar.setProgress(progress);

                // Change color based on time remaining
                if (timeRemainingSeconds <= 30) {
                    // Last 30 seconds - RED and blinking
                    lblTimer.setStyle("-fx-text-fill: #d63031; -fx-font-size: 18px; " +
                            "-fx-font-weight: bold; -fx-font-family: 'Courier New';");
                    progressBar.setStyle("-fx-accent: #d63031;");
                    timerBox.setStyle("-fx-background-color: #2d3436; " +
                            "-fx-background-radius: 20; -fx-border-color: #d63031; " +
                            "-fx-border-radius: 20; -fx-border-width: 2;");
                } else if (timeRemainingSeconds <= 60) {
                    // Last minute - ORANGE
                    lblTimer.setStyle("-fx-text-fill: #fdcb6e; -fx-font-size: 18px; " +
                            "-fx-font-weight: bold; -fx-font-family: 'Courier New';");
                    progressBar.setStyle("-fx-accent: #fdcb6e;");
                }

                // Time's up!
                if (timeRemainingSeconds <= 0) {
                    stopQuizTimer();
                    // Auto-submit with a warning
                    showInfo("⏰ Temps écoulé! Le quiz est soumis automatiquement.");
                    submitQuiz(f, quiz, questions, answerGroups);
                }
            }));
            quizTimeline.setCycleCount(totalTimeSeconds);
            quizTimeline.play();

        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    // ═══════════════════════════════
    //  TIMER HELPERS
    // ═══════════════════════════════
    private String formatTimerTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void stopQuizTimer() {
        if (quizTimeline != null) {
            quizTimeline.stop();
            quizTimeline = null;
        }
    }

    // ═══════════════════════════════
    //  SUBMIT QUIZ
    // ═══════════════════════════════
    private void submitQuiz(Formation f, Quiz quiz, List<Question> questions,
                            Map<Integer, ToggleGroup> answerGroups) {
        stopQuizTimer();

        // Check all answered (allow unanswered if timer expired)
        boolean allAnswered = true;
        for (Question q : questions) {
            ToggleGroup group = answerGroups.get(q.getId());
            if (group.getSelectedToggle() == null) {
                allAnswered = false;
                break;
            }
        }

        if (!allAnswered && timeRemainingSeconds > 0) {
            showInfo("⚠️ Veuillez répondre à toutes les questions!");
            return;
        }

        // Calculate score
        int totalPoints = 0;
        int earnedPoints = 0;

        for (Question q : questions) {
            totalPoints += q.getPoints();
            ToggleGroup group = answerGroups.get(q.getId());
            if (group.getSelectedToggle() != null) {
                RadioButton selected = (RadioButton) group.getSelectedToggle();
                Reponse chosen = (Reponse) selected.getUserData();
                if (chosen.isCorrect()) {
                    earnedPoints += q.getPoints();
                }
            }
        }

        double percentage = totalPoints > 0 ? (earnedPoints * 100.0) / totalPoints : 0;
        boolean passed = percentage >= quiz.getPassingScore();

        // Save result
        QuizResult result = new QuizResult();
        result.setQuizId(quiz.getId());
        result.setUserId(currentUserId);
        result.setScore(earnedPoints);
        result.setTotalPoints(totalPoints);
        result.setPassed(passed);

        try {
            quizResultService.insertOne(result);
        } catch (SQLException e) {
            showInfo("Erreur sauvegarde: " + e.getMessage());
            return;
        }

        // ═══════════════════════════════════════════════════════
        //  GENERATE PDF CERTIFICATE IF PASSED (Métier Avancé)
        // ═══════════════════════════════════════════════════════
        if (passed) {
            String certPath = certificateService.generateCertificate(
                    "Patient #" + currentUserId,
                    f.getTitle(),
                    earnedPoints,
                    totalPoints,
                    percentage
            );

            if (certPath != null) {
                Alert certAlert = new Alert(Alert.AlertType.INFORMATION);
                certAlert.setTitle("🎉 Certificat Généré!");
                certAlert.setHeaderText("Félicitations! Vous avez réussi!");
                certAlert.setContentText("Certificat PDF sauvegardé sur votre Bureau:\n" + certPath);

                ButtonType openBtn = new ButtonType("📄 Ouvrir PDF");
                ButtonType laterBtn = new ButtonType("Plus tard",
                        ButtonBar.ButtonData.CANCEL_CLOSE);
                certAlert.getButtonTypes().setAll(openBtn, laterBtn);

                Optional<ButtonType> certResult = certAlert.showAndWait();
                if (certResult.isPresent() && certResult.get() == openBtn) {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(certPath));
                    } catch (Exception ex) {
                        System.err.println("Cannot open PDF: " + ex.getMessage());
                    }
                }
            }
        }

        // Show result page
        showQuizResult(f, quiz, questions, answerGroups, earnedPoints,
                totalPoints, percentage, passed);
    }

    // ═══════════════════════════════
    //  QUIZ RESULT PAGE
    // ═══════════════════════════════
    private void showQuizResult(Formation f, Quiz quiz, List<Question> questions,
                                Map<Integer, ToggleGroup> answerGroups,
                                int earned, int total, double percentage, boolean passed) {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));
        page.setAlignment(Pos.TOP_CENTER);
        page.setStyle("-fx-background-color: #fef0f5;");

        Label icon = new Label(passed ? "🎉" : "😔");
        icon.setStyle("-fx-font-size: 60px;");

        Label lblResult = new Label(passed ? "QUIZ RÉUSSI!" : "QUIZ ÉCHOUÉ");
        lblResult.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " +
                (passed ? "#00b894" : "#d63031") + ";");

        Label lblScore = new Label(String.format("%.0f%%", percentage));
        lblScore.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " +
                (passed ? "#00b894" : "#d63031") + ";");

        Label lblDetail = new Label(earned + " / " + total + " points | Minimum: " +
                quiz.getPassingScore() + "%");
        lblDetail.setStyle("-fx-text-fill: #636e72; -fx-font-size: 14px;");

        VBox headerBox = new VBox(10, icon, lblResult, lblScore, lblDetail);
        headerBox.setAlignment(Pos.CENTER);

        if (passed) {
            Label certInfo = new Label("📄 Un certificat PDF a été généré sur votre Bureau!");
            certInfo.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold; -fx-font-size: 14px;");
            headerBox.getChildren().add(certInfo);
        }

        // Review answers
        Label lblReview = new Label("📋 Révision des réponses:");
        lblReview.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        VBox reviewBox = new VBox(10);
        int num = 1;
        for (Question q : questions) {
            ToggleGroup group = answerGroups.get(q.getId());

            VBox qReview = new VBox(5);

            boolean answeredCorrectly = false;
            String yourAnswer = "— Non répondu —";

            if (group.getSelectedToggle() != null) {
                RadioButton selected = (RadioButton) group.getSelectedToggle();
                Reponse chosen = (Reponse) selected.getUserData();
                yourAnswer = chosen.getOptionText();
                answeredCorrectly = chosen.isCorrect();
            }

            qReview.setStyle("-fx-background-color: " +
                    (answeredCorrectly ? "#f0fff0" : "#fff0f0") +
                    "; -fx-padding: 12; -fx-background-radius: 8;");

            Label lblQ = new Label("Q" + num + ": " + q.getQuestionText());
            lblQ.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
            lblQ.setWrapText(true);

            Label lblYour = new Label("Votre réponse: " + yourAnswer);
            lblYour.setStyle("-fx-text-fill: " + (answeredCorrectly ? "#00b894" : "#d63031") + ";");
            lblYour.setWrapText(true);

            qReview.getChildren().addAll(lblQ, lblYour);

            if (!answeredCorrectly) {
                try {
                    List<Reponse> allReponses = reponseService.selectByQuestion(q.getId());
                    for (Reponse r : allReponses) {
                        if (r.isCorrect()) {
                            Label lblCorrect = new Label("✅ Bonne réponse: " + r.getOptionText());
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

        // Buttons
        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10));

        Button btnRetry = new Button("🔄 Réessayer");
        btnRetry.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnRetry.setOnAction(e -> takeQuiz(f));

        Button btnHome = new Button("🏠 Accueil");
        btnHome.setStyle("-fx-background-color: #a0e8af; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnHome.setOnAction(e -> showAccueil());

        Button btnResults = new Button("📊 Mes résultats");
        btnResults.setStyle("-fx-background-color: #e8a0bf; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnResults.setOnAction(e -> showMyResults());

        buttons.getChildren().addAll(btnRetry, btnHome, btnResults);

        VBox all = new VBox(15, headerBox, new Separator(), lblReview, reviewBox, buttons);
        all.setAlignment(Pos.TOP_CENTER);
        all.setPadding(new Insets(10));
        all.setStyle("-fx-background-color: #fef0f5;");

        ScrollPane scroll = new ScrollPane(all);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #fef0f5; -fx-background-color: #fef0f5;");

        contentArea.getChildren().clear();
        contentArea.getChildren().add(scroll);
    }

    // ═══════════════════════════════
    //  OTHER MODULE PLACEHOLDERS
    // ═══════════════════════════════
    @FXML
    private void showJournal() {
        setActiveButton(btnJournal);
        stopCurrentVideo();
        stopQuizTimer();
        showComingSoon("📰 Mon Journal — Module de votre coéquipier");
    }

    @FXML
    private void showConsultation() {
        setActiveButton(btnConsultation);
        stopCurrentVideo();
        stopQuizTimer();
        showComingSoon("🩺 Mes Consultations — Module de votre coéquipier");
    }

    @FXML
    private void switchToAdmin() {
        stopCurrentVideo();
        stopQuizTimer();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AdminDashboard.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void switchToCoach() {
        stopCurrentVideo();
        stopQuizTimer();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/CoachDashboard.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════
    //  HELPERS
    // ═══════════════════════════════
    private void stopCurrentVideo() {
        if (currentVideoContainer != null) {
            VideoPlayerUtil.stopMedia(currentVideoContainer);
            currentVideoContainer = null;
        }
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
        Button[] all = {btnAccueil, btnAllFormations, btnMyFormations,
                btnMyResults, btnMyStats, btnJournal, btnConsultation};
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("sidebar-btn-active");
        }
        if (btn != null) btn.getStyleClass().add("sidebar-btn-active");
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showComingSoon(String msg) {
        VBox page = new VBox(20);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(50));
        Label iconLbl = new Label("🔧");
        iconLbl.setStyle("-fx-font-size: 50px;");
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #636e72;");
        Label sub = new Label("Ce module sera disponible après l'intégration avec Git");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #b2bec3;");
        page.getChildren().addAll(iconLbl, lbl, sub);
        setContent(page);
    }

    @FXML
    private void handleLogout() {
        stopCurrentVideo();
        stopQuizTimer();
        System.exit(0);
    }
}