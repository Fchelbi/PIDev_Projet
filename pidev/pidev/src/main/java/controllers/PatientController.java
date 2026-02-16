package controllers;

import entities.Formation;
import entities.Participant;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import services.FormationService;
import services.ParticipantService;
import services.QuizService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class PatientController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button btnAccueil;
    @FXML private Button btnAllFormations;
    @FXML private Button btnMyFormations;
    @FXML private Button btnJournal;
    @FXML private Button btnConsultation;
    @FXML private Label lblPatientName;

    private FormationService formationService;
    private ParticipantService participantService;
    private QuizService quizService;
    private int currentUserId = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        participantService = new ParticipantService();
        quizService = new QuizService();
        showAccueil();
    }

    // ═══════════════════════════════
    //  ACCUEIL
    // ═══════════════════════════════
    @FXML
    private void showAccueil() {
        setActiveButton(btnAccueil);
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

            statsRow.getChildren().addAll(
                    createStatCard("📚", "Formations\nDisponibles",
                            String.valueOf(allFormations.size()), "#7fc8f8"),
                    createStatCard("📖", "Mes\nFormations",
                            String.valueOf(myCount), "#e8a0bf"),
                    createStatCard("📝", "Quiz\nComplétés", "0", "#a0e8af"),
                    createStatCard("⭐", "Mon\nScore", "—", "#f8d07f")
            );
        } catch (SQLException e) {
            statsRow.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }

        Label actionsTitle = new Label("Actions Rapides");
        actionsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);

        Button btnGoFormations = new Button("📚 Voir les Formations");
        btnGoFormations.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15 30;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnGoFormations.setOnAction(e -> showAllFormations());

        Button btnGoMy = new Button("📖 Mes Formations");
        btnGoMy.setStyle("-fx-background-color: #e8a0bf; -fx-text-fill: white;" +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15 30;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btnGoMy.setOnAction(e -> showMyFormations());

        actions.getChildren().addAll(btnGoFormations, btnGoMy);

        page.getChildren().addAll(welcome, subtitle, statsRow, actionsTitle, actions);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    private VBox createStatCard(String icon, String label, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(180);
        card.setPrefHeight(120);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        card.setPadding(new Insets(15));

        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size: 28px;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72;");
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
        VBox page = new VBox(15);
        page.getChildren().add(createTitle("📚 Toutes les Formations"));
        try {
            List<Formation> formations = formationService.selectALL();
            FlowPane cards = new FlowPane(15, 15);
            for (Formation f : formations)
                cards.getChildren().add(createFormationCard(f, false));
            if (cards.getChildren().isEmpty())
                cards.getChildren().add(new Label("Aucune formation disponible"));
            ScrollPane scroll = new ScrollPane(cards);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            page.getChildren().add(scroll);
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    // ═══════════════════════════════
    //  MY FORMATIONS
    // ═══════════════════════════════
    @FXML
    private void showMyFormations() {
        setActiveButton(btnMyFormations);
        VBox page = new VBox(15);
        page.getChildren().add(createTitle("📖 Mes Formations"));
        try {
            List<Participant> myList = participantService.selectALL();
            List<Formation> allFormations = formationService.selectALL();
            FlowPane cards = new FlowPane(15, 15);
            for (Participant p : myList) {
                if (p.getUserId() == currentUserId) {
                    for (Formation f : allFormations) {
                        if (f.getId() == p.getFormationId())
                            cards.getChildren().add(createFormationCard(f, true));
                    }
                }
            }
            if (cards.getChildren().isEmpty())
                cards.getChildren().add(new Label("Vous n'êtes inscrit à aucune formation"));
            ScrollPane scroll = new ScrollPane(cards);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            page.getChildren().add(scroll);
        } catch (SQLException e) {
            page.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    @FXML
    private void showJournal() {
        setActiveButton(btnJournal);
        showComingSoon("Mon Journal — Votre coéquipier va l'implémenter!");
    }

    @FXML
    private void showConsultation() {
        setActiveButton(btnConsultation);
        showComingSoon("Mes Consultations — Votre coéquipier va l'implémenter!");
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
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        lblTitle.setWrapText(true);

        Label lblDesc = new Label(f.getDescription());
        lblDesc.setStyle("-fx-text-fill: #636e72; -fx-font-size: 12px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(60);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button btnWatch = new Button("▶️ Voir");
        btnWatch.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white;" +
                "-fx-cursor: hand; -fx-background-radius: 5;");
        btnWatch.setOnAction(e -> watchVideo(f));
        buttons.getChildren().add(btnWatch);

        if (enrolled) {
            Button btnQuiz = new Button("📝 Quiz");
            btnQuiz.setStyle("-fx-background-color: #e8a0bf; -fx-text-fill: white;" +
                    "-fx-cursor: hand; -fx-background-radius: 5;");
            btnQuiz.setOnAction(e -> takeQuiz(f));
            buttons.getChildren().add(btnQuiz);
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
    //  WATCH VIDEO
    // ═══════════════════════════════
    private void watchVideo(Formation f) {
        VBox page = new VBox(15);
        page.setPadding(new Insets(10));

        Button btnBack = new Button("↩️ Retour");
        btnBack.setStyle("-fx-background-color: #7fc8f8; -fx-text-fill: white;" +
                "-fx-cursor: hand; -fx-background-radius: 5;");
        btnBack.setOnAction(e -> showAllFormations());

        Label title = new Label("🎬 " + f.getTitle());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        WebView webView = new WebView();
        webView.setPrefHeight(400);

        String videoUrl = f.getVideoUrl();
        if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
            webView.getEngine().load(
                    "https://www.youtube.com/embed/" + extractYouTubeId(videoUrl));
        } else {
            File file = new File(videoUrl);
            if (file.exists()) {
                String html = "<html><body style='margin:0;background:black;'>" +
                        "<video width='100%' height='100%' controls>" +
                        "<source src='" + file.toURI().toString() + "'/>" +
                        "</video></body></html>";
                webView.getEngine().loadContent(html);
            } else {
                webView.getEngine().loadContent(
                        "<html><body style='color:white;background:black;text-align:center;" +
                                "padding-top:180px;'><h2>Vidéo non trouvée</h2></body></html>");
            }
        }

        Label desc = new Label(f.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #636e72;");

        page.getChildren().addAll(btnBack, title, webView, desc);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    // ═══════════════════════════════
    //  REGISTER
    // ═══════════════════════════════
    private void registerToFormation(Formation f) {
        try {
            if (participantService.isAlreadyRegistered(currentUserId, f.getId())) {
                showInfo("Déjà inscrit!");
                return;
            }
            Participant p = new Participant();
            p.setUserId(currentUserId);
            p.setFormationId(f.getId());
            participantService.insertOne(p);
            showInfo("Inscription réussie! ✅");
            showAllFormations();
        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    private void takeQuiz(Formation f) {
        showInfo("Quiz pour " + f.getTitle() + " — Bientôt!");
    }

    // ═══════════════════════════════
    //  SWITCH TO ADMIN
    // ═══════════════════════════════
    @FXML
    private void switchToAdmin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AdminDashboard.fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════
    //  HELPERS
    // ═══════════════════════════════
    private String extractYouTubeId(String url) {
        String id = "";
        if (url.contains("v=")) id = url.split("v=")[1];
        else if (url.contains("youtu.be/")) id = url.split("youtu.be/")[1];
        if (id.contains("&")) id = id.split("&")[0];
        return id;
    }

    private Label createTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        return lbl;
    }

    private void setActiveButton(Button btn) {
        Button[] all = {btnAccueil, btnAllFormations, btnMyFormations, btnJournal, btnConsultation};
        for (Button b : all) if (b != null) b.getStyleClass().remove("sidebar-btn-active");
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
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #636e72;");
        page.getChildren().add(lbl);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    @FXML
    private void handleLogout() {
        System.exit(0);
    }
}