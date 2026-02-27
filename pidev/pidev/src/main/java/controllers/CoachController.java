package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CoachController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnAccueil;
    @FXML private Button btnFormations;
    @FXML private Button btnResults;
    @FXML private Button btnPatients;
    @FXML private Label lblCoachName;

    private FormationService formationService;
    private QuizService quizService;
    private QuizResultService quizResultService;
    private ParticipantService participantService;
    private QuestionService questionService;
    private ReponseService reponseService;
    private int currentCoachId = 2;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        quizService = new QuizService();
        quizResultService = new QuizResultService();
        participantService = new ParticipantService();
        questionService = new QuestionService();
        reponseService = new ReponseService();
        showAccueil();
    }

    // ══════════════════════════════════════════════════════
    //  1. TABLEAU DE BORD
    // ══════════════════════════════════════════════════════
    @FXML
    public void showAccueil() {
        setActive(btnAccueil);

        VBox page = new VBox(20);
        page.setPadding(new Insets(20));

        page.getChildren().addAll(
                styled("👨‍⚕️ Tableau de Bord Coach", 24, true),
                styled("Vue d'ensemble des formations et des patients", 13, false),
                new Separator()
        );

        try {
            List<QuizResult> allResults = quizResultService.selectALL();
            List<Formation> allFormations = formationService.selectALL();
            List<Participant> allPartic = participantService.selectALL();

            long passed = allResults.stream().filter(QuizResult::isPassed).count();
            long failed = allResults.size() - passed;
            double avg = allResults.stream()
                    .mapToDouble(QuizResult::getPercentage).average().orElse(0);
            long patients = allPartic.stream()
                    .map(Participant::getUserId).distinct().count();

            // Stats cards
            HBox statRow = new HBox(12);
            statRow.setAlignment(Pos.CENTER_LEFT);
            statRow.getChildren().addAll(
                    card("📚", "Formations", String.valueOf(allFormations.size()), "#7fc8f8"),
                    card("👥", "Patients", String.valueOf(patients), "#e8a0bf"),
                    card("📝", "Tentatives", String.valueOf(allResults.size()), "#fdcb6e"),
                    card("✅", "Réussis", String.valueOf(passed), "#00b894"),
                    card("❌", "Échoués", String.valueOf(failed), "#d63031"),
                    card("📈", "Moyenne", String.format("%.1f%%", avg), "#6c5ce7")
            );
            page.getChildren().add(statRow);

            // Bar chart - enrollments per formation
            page.getChildren().addAll(new Separator(),
                    styled("📊 Inscriptions par formation", 16, true));

            VBox bars = new VBox(8);
            bars.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-padding: 15;");
            long maxC = allFormations.stream()
                    .mapToLong(f -> allPartic.stream()
                            .filter(p -> p.getFormationId() == f.getId()).count())
                    .max().orElse(1);
            if (maxC == 0) maxC = 1;

            for (Formation f : allFormations) {
                long c = allPartic.stream()
                        .filter(p -> p.getFormationId() == f.getId()).count();
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label nameLbl = new Label(f.getTitle());
                nameLbl.setMinWidth(180);
                nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3436;");

                HBox bar = new HBox();
                bar.setPrefHeight(16);
                bar.setPrefWidth((double) c / maxC * 260);
                bar.setStyle("-fx-background-color: #7fc8f8; -fx-background-radius: 3;");

                Label cntLbl = new Label(c + " inscrits");
                cntLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72;");

                row.getChildren().addAll(nameLbl, bar, cntLbl);
                bars.getChildren().add(row);
            }
            if (allFormations.isEmpty())
                bars.getChildren().add(new Label("Aucune formation."));
            page.getChildren().add(bars);

            // Last 5 results
            page.getChildren().addAll(new Separator(),
                    styled("🕐 Derniers résultats", 16, true));

            List<QuizResult> last5 = allResults.stream()
                    .sorted(Comparator.comparing(
                            r -> r.getCompletedAt() == null
                                    ? java.time.LocalDateTime.MIN : r.getCompletedAt(),
                            Comparator.reverseOrder()))
                    .limit(5).collect(Collectors.toList());

            for (QuizResult r : last5)
                page.getChildren().add(resultCard(r, null));
            if (last5.isEmpty())
                page.getChildren().add(new Label("Aucun résultat pour l'instant."));

        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        setContent(page);
    }

    // ══════════════════════════════════════════════════════
    //  2. FORMATIONS (view only for coach)
    // ══════════════════════════════════════════════════════
    @FXML
    public void showFormations() {
        setActive(btnFormations);

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().addAll(
                styled("📚 Formations", 22, true),
                styled("Les formations créées par l'admin", 12, false),
                new Separator()
        );

        // Search
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("🔍 Rechercher...");
        tfSearch.setMaxWidth(320);
        tfSearch.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; " +
                "-fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 7 12;");
        page.getChildren().add(tfSearch);

        try {
            List<Formation> formations = formationService.selectALL();
            List<Participant> allPartic = participantService.selectALL();

            VBox list = new VBox(10);
            page.getChildren().add(list);

            Runnable refresh = () -> {
                list.getChildren().clear();
                String kw = tfSearch.getText().trim().toLowerCase();
                for (Formation f : formations) {
                    if (!kw.isEmpty()
                            && !f.getTitle().toLowerCase().contains(kw)
                            && !f.getCategory().toLowerCase().contains(kw))
                        continue;

                    long inscrits = allPartic.stream()
                            .filter(p -> p.getFormationId() == f.getId()).count();
                    Quiz quiz = null;
                    try {
                        quiz = quizService.selectByFormation(f.getId());
                    } catch (SQLException ignored) {}

                    // Number of questions
                    int nbQuestions = 0;
                    if (quiz != null) {
                        try {
                            nbQuestions = questionService.selectByQuiz(quiz.getId()).size();
                        } catch (SQLException ignored) {}
                    }

                    HBox c = new HBox(15);
                    c.setPadding(new Insets(15));
                    c.setAlignment(Pos.CENTER_LEFT);
                    c.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

                    Label badge = new Label(f.getCategory());
                    badge.setStyle("-fx-background-color: #e8a0bf; -fx-text-fill: white; " +
                            "-fx-padding: 2 8; -fx-background-radius: 8; -fx-font-size: 11px;");

                    VBox info = new VBox(3);
                    Label t = new Label(f.getTitle());
                    t.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
                    Label desc = new Label(f.getDescription());
                    desc.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
                    desc.setWrapText(true);
                    desc.setMaxWidth(350);
                    desc.setMaxHeight(40);
                    info.getChildren().addAll(t, badge, desc);

                    HBox spacer = new HBox();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    VBox stats = new VBox(3);
                    stats.setAlignment(Pos.CENTER_RIGHT);
                    Label il = new Label(inscrits + " inscrits");
                    il.setStyle("-fx-text-fill: #7fc8f8; -fx-font-weight: bold;");

                    String quizText;
                    String quizColor;
                    if (quiz != null) {
                        quizText = "Quiz ✅ (" + quiz.getPassingScore() + "%) — "
                                + nbQuestions + " questions";
                        quizColor = "#00b894";
                    } else {
                        quizText = "Quiz ❌ Pas encore créé";
                        quizColor = "#d63031";
                    }
                    Label ql = new Label(quizText);
                    ql.setStyle("-fx-text-fill: " + quizColor + "; -fx-font-size: 12px;");

                    // Video indicator
                    String videoUrl = f.getVideoUrl();
                    String videoText = (videoUrl != null && !videoUrl.trim().isEmpty())
                            ? "🎬 Vidéo disponible" : "🎬 Pas de vidéo";
                    Label vl = new Label(videoText);
                    vl.setStyle("-fx-text-fill: #636e72; -fx-font-size: 11px;");

                    stats.getChildren().addAll(il, ql, vl);

                    c.getChildren().addAll(info, spacer, stats);
                    list.getChildren().add(c);
                }
                if (list.getChildren().isEmpty())
                    list.getChildren().add(new Label("Aucune formation trouvée."));
            };

            tfSearch.textProperty().addListener((o, ov, nv) -> refresh.run());
            refresh.run();

        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        setContent(page);
    }

    // ══════════════════════════════════════════════════════
    //  3. RÉSULTATS PATIENTS
    // ══════════════════════════════════════════════════════
    @FXML
    public void showAllResults() {
        setActive(btnResults);

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().addAll(
                styled("📊 Résultats des Patients", 22, true),
                styled("Cliquez sur un résultat pour voir la révision complète", 12, false),
                new Separator()
        );

        // Filter
        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("Tous", "✅ Réussis", "❌ Échoués");
        cbFilter.setValue("Tous");

        // Search by patient ID (numbers only = contrôle de saisie)
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("🔍 Patient ID...");
        tfSearch.setMaxWidth(160);
        tfSearch.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 6 10;");
        // ══ CONTRÔLE DE SAISIE: only digits allowed ══
        tfSearch.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) tfSearch.setText(n.replaceAll("[^\\d]", ""));
        });

        HBox filters = new HBox(10, new Label("Filtre:"), cbFilter, tfSearch);
        filters.setAlignment(Pos.CENTER_LEFT);
        page.getChildren().add(filters);

        try {
            List<QuizResult> allResults = quizResultService.selectALL();
            allResults.sort((a, b) -> {
                if (a.getCompletedAt() == null) return 1;
                if (b.getCompletedAt() == null) return -1;
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            });

            Label lblCount = new Label("Total: " + allResults.size() + " résultats");
            lblCount.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
            page.getChildren().add(lblCount);

            VBox list = new VBox(8);
            page.getChildren().add(list);

            Runnable refresh = () -> {
                list.getChildren().clear();
                String filter = cbFilter.getValue();
                String kw = tfSearch.getText().trim();
                int count = 0;
                for (QuizResult r : allResults) {
                    if (filter.equals("✅ Réussis") && !r.isPassed()) continue;
                    if (filter.equals("❌ Échoués") && r.isPassed()) continue;
                    if (!kw.isEmpty() && !String.valueOf(r.getUserId()).contains(kw))
                        continue;
                    list.getChildren().add(resultCard(r, () -> showPatientDetail(r)));
                    count++;
                }
                lblCount.setText("Affichés: " + count + " / " + allResults.size());
                if (list.getChildren().isEmpty())
                    list.getChildren().add(new Label("Aucun résultat."));
            };

            cbFilter.setOnAction(e -> refresh.run());
            tfSearch.textProperty().addListener((o, ov, nv) -> refresh.run());
            refresh.run();

        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        setContent(page);
    }

    // ══════════════════════════════════════════════════════
    //  4. SUIVI PATIENTS
    // ══════════════════════════════════════════════════════
    @FXML
    public void showPatients() {
        setActive(btnPatients);

        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().addAll(
                styled("👥 Suivi des Patients", 22, true),
                styled("Performances par patient — cliquez pour voir le détail", 12, false),
                new Separator()
        );

        try {
            List<QuizResult> allResults = quizResultService.selectALL();
            List<Participant> allPartic = participantService.selectALL();

            java.util.Map<Integer, List<QuizResult>> byPatient = new java.util.LinkedHashMap<>();
            for (QuizResult r : allResults)
                byPatient.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);

            if (byPatient.isEmpty()) {
                page.getChildren().add(
                        new Label("Aucun patient n'a encore passé de quiz."));
                setContent(page);
                return;
            }
            for (java.util.Map.Entry<Integer, List<QuizResult>> entry : byPatient.entrySet()) {
                int pid = entry.getKey();
                List<QuizResult> results = entry.getValue();
                long passedC = results.stream().filter(QuizResult::isPassed).count();
                double avgP = results.stream()
                        .mapToDouble(QuizResult::getPercentage).average().orElse(0);
                long inscrits = allPartic.stream()
                        .filter(p -> p.getUserId() == pid).count();
                HBox c = new HBox(15);
                c.setPadding(new Insets(14));
                c.setAlignment(Pos.CENTER_LEFT);
                c.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 8, 0, 0, 2); " +
                        "-fx-cursor: hand;");

                // Avatar circle
                Circle avatar = new Circle(22);
                avatar.setFill(Color.web(avgP >= 70 ? "#00b894" : "#e8a0bf"));
                Label avLbl = new Label("P" + pid);
                avLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-font-size: 13px;");
                StackPane avPane = new StackPane(avatar, avLbl);

                VBox info = new VBox(3);
                Label pLbl = new Label("Patient #" + pid);
                pLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-text-fill: #2d3436;");
                Label sLbl = new Label(inscrits + " formation(s) | "
                        + results.size() + " quiz passé(s)");
                sLbl.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
                info.getChildren().addAll(pLbl, sLbl);

                HBox spacer = new HBox();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                ProgressBar pb = new ProgressBar(avgP / 100.0);
                pb.setPrefWidth(100);
                pb.setStyle("-fx-accent: " + (avgP >= 70 ? "#00b894" : "#d63031") + ";");

                VBox stats = new VBox(2);
                stats.setAlignment(Pos.CENTER_RIGHT);
                Label avgLbl = new Label(String.format("%.0f%%", avgP) + " moy.");
                avgLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + (avgP >= 70 ? "#00b894" : "#d63031") + ";");
                Label rsLbl = new Label(passedC + "/" + results.size() + " réussis");
                rsLbl.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
                stats.getChildren().addAll(avgLbl, rsLbl);

                c.getChildren().addAll(avPane, info, spacer, pb, stats);
                c.setOnMouseClicked(e -> showPatientResults(pid));
                page.getChildren().add(c);
            }

        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        setContent(page);
    }

    // ── Results for a single patient ──
    private void showPatientResults(int pid) {
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));

        Button btnBack = new Button("↩️ Retour");
        btnBack.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; " +
                "-fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 15;");
        btnBack.setOnAction(e -> showPatients());

        page.getChildren().addAll(btnBack,
                styled("📊 Patient #" + pid + " — Résultats", 20, true),
                new Separator()
        );

        try {
            List<QuizResult> results = quizResultService.selectByUser(pid);
            if (results.isEmpty())
                page.getChildren().add(new Label("Aucun résultat."));
            for (QuizResult r : results)
                page.getChildren().add(resultCard(r, () -> showPatientDetail(r)));
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        setContent(page);
    }

    // ── Full review of a quiz result ──
    private void showPatientDetail(QuizResult r) {
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));

        Button btnBack = new Button("↩️ Retour aux résultats");
        btnBack.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; " +
                "-fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 15;");
        btnBack.setOnAction(e -> showAllResults());

        String sc = r.isPassed() ? "#00b894" : "#d63031";

        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-padding: 20; -fx-effect: dropshadow(three-pass-box, " +
                "rgba(0,0,0,0.07), 8, 0, 0, 2);");

        Label iconLbl = new Label(r.isPassed() ? "🎉" : "😔");
        iconLbl.setStyle("-fx-font-size: 48px;");
        Label patLbl = new Label("Patient #" + r.getUserId());
        patLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        Label statusLbl = new Label(r.isPassed() ? "RÉUSSI" : "ÉCHOUÉ");
        statusLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + sc + ";");
        Label scoreLbl = new Label(String.format("%.0f%% — %d / %d pts",
                r.getPercentage(), r.getScore(), r.getTotalPoints()));
        scoreLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #636e72;");
        Label dateLbl = new Label(r.getCompletedAt() != null
                ? r.getCompletedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #b2bec3;");
        header.getChildren().addAll(iconLbl, patLbl, statusLbl, scoreLbl, dateLbl);

        page.getChildren().addAll(btnBack, header);

        // Show quiz questions and correct answers
        try {
            Quiz quiz = null;
            for (Quiz q : quizService.selectALL())
                if (q.getId() == r.getQuizId()) {
                    quiz = q;
                    break;
                }

            if (quiz != null) {
                Label qTitle = new Label("📋 Quiz: " + quiz.getTitle());
                qTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                        "-fx-text-fill: #2d3436;");
                page.getChildren().add(qTitle);

                List<Question> questions = questionService.selectByQuiz(quiz.getId());
                int num = 1;
                for (Question q : questions) {
                    VBox qBox = new VBox(8);
                    qBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                            "-fx-padding: 14; -fx-effect: dropshadow(three-pass-box, " +
                            "rgba(0,0,0,0.05), 5, 0, 0, 2);");

                    Label qLbl = new Label("Q" + num + ": " + q.getQuestionText()
                            + " (" + q.getPoints() + " pts)");
                    qLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; " +
                            "-fx-text-fill: #2d3436;");
                    qLbl.setWrapText(true);
                    qBox.getChildren().add(qLbl);

                    for (Reponse rep : reponseService.selectByQuestion(q.getId())) {
                        Label rLbl = new Label(
                                (rep.isCorrect() ? "✅ " : "❌ ") + rep.getOptionText());
                        rLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: "
                                + (rep.isCorrect() ? "#00b894" : "#636e72") + ";");
                        rLbl.setWrapText(true);
                        qBox.getChildren().add(rLbl);
                    }

                    page.getChildren().add(qBox);
                    num++;
                }
            }
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        setContent(page);
    }

    // ══════════════════════════════════════════════════════
    //  UI BUILDERS
    // ══════════════════════════════════════════════════════

    private VBox card(String icon, String label, String value, String color) {
        VBox c = new VBox(5);
        c.setAlignment(Pos.CENTER);
        c.setPrefWidth(155);
        c.setPrefHeight(100);
        c.setPadding(new Insets(12));
        c.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 22px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        c.getChildren().addAll(i, v, l);
        return c;
    }

    private HBox resultCard(QuizResult r, Runnable onClick) {
        HBox c = new HBox(15);
        c.setPadding(new Insets(14));
        c.setAlignment(Pos.CENTER_LEFT);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);" +
                (onClick != null ? " -fx-cursor: hand;" : ""));

        Label icon = new Label(r.isPassed() ? "✅" : "❌");
        icon.setStyle("-fx-font-size: 28px;");

        String qName = "Quiz #" + r.getQuizId();
        try {
            for (Quiz q : quizService.selectALL())
                if (q.getId() == r.getQuizId()) {
                    qName = q.getTitle();
                    break;
                }
        } catch (SQLException ignored) {}

        VBox info = new VBox(3);
        Label u = new Label("Patient ID: " + r.getUserId());
        u.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
        Label q = new Label("Quiz: " + qName);
        q.setStyle("-fx-text-fill: #636e72;");
        Label d = new Label(r.getCompletedAt() != null
                ? r.getCompletedAt().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        d.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 11px;");
        info.getChildren().addAll(u, q, d);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label s = new Label(String.format("%.0f%%", r.getPercentage()));
        s.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: "
                + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        Label p = new Label(r.getScore() + "/" + r.getTotalPoints() + " pts");
        p.setStyle("-fx-text-fill: #636e72; -fx-font-size: 11px;");
        scoreBox.getChildren().addAll(s, p);

        c.getChildren().addAll(icon, info, spacer);

        if (onClick != null) {
            Label hint = new Label("👁 Voir révision →");
            hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #a29bfe;");
            c.getChildren().add(hint);
            c.setOnMouseClicked(e -> onClick.run());
        }

        c.getChildren().add(scoreBox);
        return c;
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS - FIX CLICK ISSUE
    // ══════════════════════════════════════════════════════

    /**
     * FIXES THE CLICK PROBLEM:
     * setPannable(false) = ScrollPane won't steal mouse events from buttons
     * setFocusTraversable(false) = ScrollPane won't capture keyboard focus
     */
    private void setContent(VBox page) {
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPannable(false);
        sp.setFocusTraversable(false);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        contentArea.getChildren().setAll(sp);
    }

    private Label styled(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px;"
                + (bold ? " -fx-font-weight: bold; -fx-text-fill: #2d3436;"
                : " -fx-text-fill: #636e72;"));
        return l;
    }

    private void setActive(Button btn) {
        Button[] all = {btnAccueil, btnFormations, btnResults, btnPatients};
        for (Button b : all) if (b != null) b.getStyleClass().remove("sidebar-btn-active");
        if (btn != null) btn.getStyleClass().add("sidebar-btn-active");
    }

    @FXML
    private void handleLogout() {
        javafx.application.Platform.exit();
    }
    // Add a new button in CoachDashboard.fxml sidebar:
// <Button fx:id="btnManageFormations" text="   ⚙️  Gérer mes Formations"
//         onAction="#showManageFormations" styleClass="sidebar-btn"
//         maxWidth="Infinity" alignment="CENTER_LEFT"/>

    @FXML
    public void showManageFormations() {
        setActive(btnFormations);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FormationView.fxml"));
            Parent formationView = loader.load();

            // Get the controller and set coach mode
            FormationController fc = loader.getController();
            fc.setCoachMode(currentCoachId); // Only shows coach's formations

            contentArea.getChildren().setAll(formationView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}