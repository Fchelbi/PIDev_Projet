package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.*;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

// ✅ FIX: explicit java.util imports — évite le conflit avec controllers.Map

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

    @FXML
    public void showAccueil() {
        setActive(btnAccueil);
        VBox page = new VBox(20);
        page.setPadding(new Insets(20));
        page.getChildren().addAll(
                styled("Tableau de Bord Coach", 24, true),
                styled("Vue d'ensemble des formations et des patients", 13, false),
                new Separator()
        );
        try {
            List<QuizResult> allResults = quizResultService.selectALL();
            List<Formation> allFormations = formationService.selectALL();
            List<Participant> allPartic = participantService.selectALL();
            long passed = allResults.stream().filter(QuizResult::isPassed).count();
            long failed = allResults.size() - passed;
            double avg = allResults.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
            long patients = allPartic.stream().map(Participant::getUserId).distinct().count();
            HBox statRow = new HBox(12);
            statRow.setAlignment(Pos.CENTER_LEFT);
            statRow.getChildren().addAll(
                    card("Formations", String.valueOf(allFormations.size()), "#7fc8f8"),
                    card("Patients", String.valueOf(patients), "#e8a0bf"),
                    card("Tentatives", String.valueOf(allResults.size()), "#fdcb6e"),
                    card("Reussis", String.valueOf(passed), "#00b894"),
                    card("Echoues", String.valueOf(failed), "#d63031"),
                    card("Moyenne", String.format("%.1f%%", avg), "#6c5ce7")
            );
            page.getChildren().add(statRow);
            page.getChildren().addAll(new Separator(), styled("Inscriptions par formation", 16, true));
            VBox bars = new VBox(8);
            bars.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15;");
            long maxC = allFormations.stream()
                    .mapToLong(f -> allPartic.stream().filter(p -> p.getFormationId() == f.getId()).count())
                    .max().orElse(1);
            if (maxC == 0) maxC = 1;
            for (Formation f : allFormations) {
                long c = allPartic.stream().filter(p -> p.getFormationId() == f.getId()).count();
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label nameLbl = new Label(f.getTitle());
                nameLbl.setMinWidth(180);
                Label cntLbl = new Label(c + " inscrits");
                cntLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72;");
                HBox bar = new HBox();
                bar.setPrefHeight(16);
                bar.setPrefWidth((double) c / maxC * 260);
                bar.setStyle("-fx-background-color: #7fc8f8; -fx-background-radius: 3;");
                row.getChildren().addAll(nameLbl, bar, cntLbl);
                bars.getChildren().add(row);
            }
            if (allFormations.isEmpty()) bars.getChildren().add(new Label("Aucune formation."));
            page.getChildren().add(bars);
            page.getChildren().addAll(new Separator(), styled("Derniers resultats", 16, true));
            List<QuizResult> last5 = allResults.stream()
                    .sorted(Comparator.comparing(
                            r -> r.getCompletedAt() == null ? java.time.LocalDateTime.MIN : r.getCompletedAt(),
                            Comparator.reverseOrder()))
                    .limit(5).collect(Collectors.toList());
            for (QuizResult r : last5) page.getChildren().add(resultCard(r, null));
            if (last5.isEmpty()) page.getChildren().add(new Label("Aucun resultat."));
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    @FXML
    public void showFormations() {
        setActive(btnFormations);
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().addAll(styled("Formations", 22, true), new Separator());
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Rechercher...");
        tfSearch.setMaxWidth(320);
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
                    if (!kw.isEmpty() && !f.getTitle().toLowerCase().contains(kw)
                            && !f.getCategory().toLowerCase().contains(kw)) continue;
                    long inscrits = allPartic.stream().filter(p -> p.getFormationId() == f.getId()).count();
                    Quiz quiz = null;
                    try { quiz = quizService.selectByFormation(f.getId()); } catch (SQLException ignored) {}
                    int nbQ = 0;
                    if (quiz != null) {
                        try { nbQ = questionService.selectByQuiz(quiz.getId()).size(); } catch (SQLException ignored) {}
                    }
                    HBox c = new HBox(15);
                    c.setPadding(new Insets(15));
                    c.setAlignment(Pos.CENTER_LEFT);
                    c.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
                    VBox info = new VBox(3);
                    Label t = new Label(f.getTitle());
                    t.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
                    Label desc = new Label(f.getDescription());
                    desc.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
                    desc.setWrapText(true);
                    info.getChildren().addAll(t, desc);
                    HBox spacer = new HBox();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    VBox stats = new VBox(3);
                    stats.setAlignment(Pos.CENTER_RIGHT);
                    Label il = new Label(inscrits + " inscrits");
                    il.setStyle("-fx-text-fill: #7fc8f8; -fx-font-weight: bold;");
                    final Quiz fq = quiz;
                    final int fnbQ = nbQ;
                    String quizText = (fq != null)
                            ? "Quiz OK (" + fq.getPassingScore() + "%) - " + fnbQ + " questions"
                            : "Quiz pas encore cree";
                    Label ql = new Label(quizText);
                    ql.setStyle("-fx-text-fill: " + (fq != null ? "#00b894" : "#d63031") + "; -fx-font-size: 12px;");
                    stats.getChildren().addAll(il, ql);
                    c.getChildren().addAll(info, spacer, stats);
                    list.getChildren().add(c);
                }
                if (list.getChildren().isEmpty()) list.getChildren().add(new Label("Aucune formation."));
            };
            tfSearch.textProperty().addListener((o, ov, nv) -> refresh.run());
            refresh.run();
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    @FXML
    public void showAllResults() {
        setActive(btnResults);
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().addAll(styled("Resultats des Patients", 22, true), new Separator());
        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("Tous", "Reussis", "Echoues");
        cbFilter.setValue("Tous");
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Patient ID...");
        tfSearch.setMaxWidth(160);
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
            Label lblCount = new Label("Total: " + allResults.size() + " resultats");
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
                    if (filter.equals("Reussis") && !r.isPassed()) continue;
                    if (filter.equals("Echoues") && r.isPassed()) continue;
                    if (!kw.isEmpty() && !String.valueOf(r.getUserId()).contains(kw)) continue;
                    list.getChildren().add(resultCard(r, () -> showPatientDetail(r)));
                    count++;
                }
                lblCount.setText("Affiches: " + count + " / " + allResults.size());
                if (list.getChildren().isEmpty()) list.getChildren().add(new Label("Aucun resultat."));
            };
            cbFilter.setOnAction(e -> refresh.run());
            tfSearch.textProperty().addListener((o, ov, nv) -> refresh.run());
            refresh.run();
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    @FXML
    public void showPatients() {
        setActive(btnPatients);
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        page.getChildren().addAll(styled("Suivi des Patients", 22, true), new Separator());
        try {
            List<QuizResult> allResults = quizResultService.selectALL();
            List<Participant> allPartic = participantService.selectALL();
            // ✅ FIX: java.util.Map explicite — pas controllers.Map
            java.util.Map<Integer, List<QuizResult>> byPatient = new LinkedHashMap<>();
            for (QuizResult r : allResults)
                byPatient.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);
            if (byPatient.isEmpty()) {
                page.getChildren().add(new Label("Aucun patient n'a encore passe de quiz."));
                setContent(page); return;
            }
            for (java.util.Map.Entry<Integer, List<QuizResult>> entry : byPatient.entrySet()) {
                int pid = entry.getKey();
                List<QuizResult> results = entry.getValue();
                long passedC = results.stream().filter(QuizResult::isPassed).count();
                double avgP = results.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
                long inscrits = allPartic.stream().filter(p -> p.getUserId() == pid).count();
                HBox c = new HBox(15);
                c.setPadding(new Insets(14));
                c.setAlignment(Pos.CENTER_LEFT);
                c.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-cursor: hand;");
                Circle avatar = new Circle(22);
                avatar.setFill(Color.web(avgP >= 70 ? "#00b894" : "#e8a0bf"));
                Label avLbl = new Label("P" + pid);
                avLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                StackPane avPane = new StackPane(avatar, avLbl);
                VBox info = new VBox(3);
                Label pLbl = new Label("Patient #" + pid);
                pLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");
                Label sLbl = new Label(inscrits + " formation(s) | " + results.size() + " quiz");
                sLbl.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
                info.getChildren().addAll(pLbl, sLbl);
                HBox spacer = new HBox();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                ProgressBar pb = new ProgressBar(avgP / 100.0);
                pb.setPrefWidth(100);
                pb.setStyle("-fx-accent: " + (avgP >= 70 ? "#00b894" : "#d63031") + ";");
                VBox stats = new VBox(2);
                stats.setAlignment(Pos.CENTER_RIGHT);
                Label avgLbl = new Label(String.format("%.0f%%", avgP));
                avgLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                        + (avgP >= 70 ? "#00b894" : "#d63031") + ";");
                Label rsLbl = new Label(passedC + "/" + results.size() + " reussis");
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

    private void showPatientResults(int pid) {
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        Button btnBack = new Button("Retour");
        btnBack.setOnAction(e -> showPatients());
        page.getChildren().addAll(btnBack, styled("Patient #" + pid, 20, true), new Separator());
        try {
            List<QuizResult> results = quizResultService.selectByUser(pid);
            if (results.isEmpty()) page.getChildren().add(new Label("Aucun resultat."));
            for (QuizResult r : results) page.getChildren().add(resultCard(r, () -> showPatientDetail(r)));
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        setContent(page);
    }

    private void showPatientDetail(QuizResult r) {
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));
        Button btnBack = new Button("Retour");
        btnBack.setOnAction(e -> showAllResults());
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;");
        Label patLbl = new Label("Patient #" + r.getUserId());
        patLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label statusLbl = new Label(r.isPassed() ? "REUSSI" : "ECHOUE");
        statusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        Label scoreLbl = new Label(String.format("%.0f%% - %d/%d pts", r.getPercentage(), r.getScore(), r.getTotalPoints()));
        header.getChildren().addAll(patLbl, statusLbl, scoreLbl);
        page.getChildren().addAll(btnBack, header);
        try {
            Quiz quiz = null;
            for (Quiz q : quizService.selectALL())
                if (q.getId() == r.getQuizId()) { quiz = q; break; }
            if (quiz != null) {
                List<Question> questions = questionService.selectByQuiz(quiz.getId());
                int num = 1;
                for (Question q : questions) {
                    VBox qBox = new VBox(8);
                    qBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14;");
                    Label qLbl = new Label("Q" + num + ": " + q.getQuestionText());
                    qLbl.setStyle("-fx-font-weight: bold;");
                    qLbl.setWrapText(true);
                    qBox.getChildren().add(qLbl);
                    for (Reponse rep : reponseService.selectByQuestion(q.getId())) {
                        Label rLbl = new Label((rep.isCorrect() ? "OK " : "X ") + rep.getOptionText());
                        rLbl.setStyle("-fx-text-fill: " + (rep.isCorrect() ? "#00b894" : "#636e72") + ";");
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

    private VBox card(String label, String value, String color) {
        VBox c = new VBox(5);
        c.setAlignment(Pos.CENTER);
        c.setPrefWidth(155); c.setPrefHeight(100);
        c.setPadding(new Insets(12));
        c.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        c.getChildren().addAll(v, l);
        return c;
    }

    private HBox resultCard(QuizResult r, Runnable onClick) {
        HBox c = new HBox(15);
        c.setPadding(new Insets(14));
        c.setAlignment(Pos.CENTER_LEFT);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10;"
                + (onClick != null ? " -fx-cursor: hand;" : ""));
        Label icon = new Label(r.isPassed() ? "OK" : "X");
        icon.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        String qName = "Quiz #" + r.getQuizId();
        try {
            for (Quiz q : quizService.selectALL())
                if (q.getId() == r.getQuizId()) { qName = q.getTitle(); break; }
        } catch (SQLException ignored) {}
        VBox info = new VBox(3);
        Label u = new Label("Patient #" + r.getUserId());
        u.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
        Label qLbl = new Label(qName);
        qLbl.setStyle("-fx-text-fill: #636e72;");
        info.getChildren().addAll(u, qLbl);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label s = new Label(String.format("%.0f%%", r.getPercentage()));
        s.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: "
                + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        c.getChildren().addAll(icon, info, spacer);
        if (onClick != null) c.setOnMouseClicked(e -> onClick.run());
        c.getChildren().add(s);
        return c;
    }

    private void setContent(VBox page) {
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPannable(false);
        sp.setFocusTraversable(false);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        contentArea.getChildren().setAll(sp);
    }

    private Label styled(String text, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: " + size + "px;" + (bold
                ? " -fx-font-weight: bold; -fx-text-fill: #2d3436;"
                : " -fx-text-fill: #636e72;"));
        return l;
    }

    private void setActive(Button btn) {
        Button[] all = {btnAccueil, btnFormations, btnResults, btnPatients};
        for (Button b : all) if (b != null) b.getStyleClass().remove("sidebar-btn-active");
        if (btn != null) btn.getStyleClass().add("sidebar-btn-active");
    }

    @FXML private void handleLogout() { javafx.application.Platform.exit(); }

    @FXML
    public void showManageFormations() {
        setActive(btnFormations);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormationView.fxml"));
            Parent view = loader.load();
            FormationController fc = loader.getController();
            fc.setCoachMode(currentCoachId);
            contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}