package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.Formation;
import entities.Participant;
import entities.QuizResult;
import entities.User;
import services.FormationService;
import services.ParticipantService;
import services.QuizResultService;
import services.serviceUser;
import utils.LightDialog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

public class CoachHome {

    // ── Labels ────────────────────────────────────────────────────────────
    @FXML private Label lblWelcome, lblAvatarHeader;
    @FXML private Label lblNom, lblEmail;
    @FXML private Label lblQuote, lblQuoteStatus;

    // ── Dashboard stat labels ─────────────────────────────────────────────
    @FXML private Label lblNbFormations;
    @FXML private Label lblNbPatients;
    @FXML private Label lblNbResultats;
    @FXML private Label lblTauxReussite;

    // ── Layout ────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;
    @FXML private ScrollPane dashboardPane;
    @FXML private ImageView imgHeaderPhoto;

    // ── Nav VBoxes ────────────────────────────────────────────────────────
    @FXML private VBox navDashboard;
    @FXML private VBox navFormations;
    @FXML private VBox navResultats;
    @FXML private VBox navPatients;
    @FXML private VBox navProfil;

    // ── Nav indicators ────────────────────────────────────────────────────
    @FXML private HBox indicDashboard;
    @FXML private HBox indicFormations;
    @FXML private HBox indicResultats;
    @FXML private HBox indicPatients;
    @FXML private HBox indicProfil;

    // ── Services ──────────────────────────────────────────────────────────
    private final serviceUser us = new serviceUser();
    private FormationService formationService;
    private ParticipantService participantService;
    private QuizResultService quizResultService;

    private User currentUser;
    private VBox currentActiveNav;

    // ── Styles ────────────────────────────────────────────────────────────
    private static final String NAV_NORMAL =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:transparent;-fx-background-radius:0 10 10 0;";
    private static final String NAV_ACTIVE =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:0 10 10 0;";
    private static final String INDIC_HIDDEN =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:transparent;-fx-background-radius:0 2 2 0;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:#E8956D;-fx-background-radius:0 2 2 0;";
    private static final String CARD_NORMAL =
            "-fx-background-color:white;-fx-padding:20;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,2);";
    private static final String CARD_HOVER =
            "-fx-background-color:#FFF8F0;-fx-padding:20;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(232,149,109,0.22),14,0,0,5);";

    // ════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void initialize() {
        try { formationService  = new FormationService();  } catch (Exception ignored) {}
        try { participantService = new ParticipantService(); } catch (Exception ignored) {}
        try { quizResultService  = new QuizResultService();  } catch (Exception ignored) {}
        System.out.println("CoachHome initialized");
    }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        refreshDashboardStats();
        loadQuote();
        setActiveNav(navDashboard, indicDashboard);
    }

    // ════════════════════════════════════════════════════════════════════
    //  USER DATA
    // ════════════════════════════════════════════════════════════════════

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) { System.err.println("Refresh: " + e.getMessage()); }
        lblWelcome.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
        lblAvatarHeader.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        if (lblNom   != null) lblNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (lblEmail != null) lblEmail.setText(currentUser.getEmail());
        updateHeaderPhoto();
    }

    private void updateHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(
                        f.toURI() + "?t=" + System.currentTimeMillis(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblAvatarHeader.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblAvatarHeader.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  DASHBOARD STATS
    // ════════════════════════════════════════════════════════════════════

    private void refreshDashboardStats() {
        try {
            // Formations assigned to this coach
            List<Formation> formations = formationService != null
                    ? formationService.selectByCoach(currentUser.getId_user())
                    : List.of();
            if (lblNbFormations != null) lblNbFormations.setText(String.valueOf(formations.size()));

            // Distinct patients enrolled in those formations
            long patients = 0;
            long results  = 0;
            long passed   = 0;
            if (participantService != null && quizResultService != null) {
                List<Participant> allPartic = participantService.selectALL();
                for (Formation f : formations) {
                    patients += allPartic.stream()
                            .filter(p -> p.getFormationId() == f.getId())
                            .map(Participant::getUserId).distinct().count();
                }
                List<QuizResult> allResults = quizResultService.selectALL();
                results = allResults.size();
                passed  = allResults.stream().filter(QuizResult::isPassed).count();
            }
            if (lblNbPatients  != null) lblNbPatients.setText(String.valueOf(patients));
            if (lblNbResultats != null) lblNbResultats.setText(String.valueOf(results));
            if (lblTauxReussite != null) {
                double taux = results > 0 ? (passed * 100.0 / results) : 0;
                lblTauxReussite.setText(String.format("%.0f%%", taux));
            }
        } catch (SQLException e) {
            System.err.println("Dashboard stats: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  QUOTE API (Quotable)
    // ════════════════════════════════════════════════════════════════════

    private void loadQuote() {
        if (lblQuote == null) return;
        Platform.runLater(() -> {
            if (lblQuoteStatus != null) lblQuoteStatus.setText("⏳ Chargement...");
            lblQuote.setText("");
        });
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.quotable.io/random?tags=leadership,wisdom,education"))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "EchoCare/1.0").GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String content = extractValue(resp.body(), "content");
                String author  = extractValue(resp.body(), "author");
                Platform.runLater(() -> {
                    if (lblQuoteStatus != null) lblQuoteStatus.setText("💬 Inspiration du jour");
                    lblQuote.setText("❝  " + content + "  ❞\n— " + author);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblQuoteStatus != null) lblQuoteStatus.setText("💬 Inspiration");
                    lblQuote.setText("❝  Un bon coach croit en ses patients plus qu'ils ne croient en eux-mêmes.  ❞");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private String extractValue(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return "";
        i += s.length();
        int end = json.indexOf("\"", i);
        return end < 0 ? "" : json.substring(i, end).replace("\\u2019","'").replace("\\n"," ");
    }

    @FXML void refreshQuote(MouseEvent e) { loadQuote(); }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION — click handlers
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void showDashboard(MouseEvent event) {
        setActiveNav(navDashboard, indicDashboard);
        contentArea.getChildren().setAll(dashboardPane);
        refreshDashboardStats();
        loadQuote();
    }

    @FXML
    void showFormations(MouseEvent event) {
        setActiveNav(navFormations, indicFormations);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormationView.fxml"));
            Parent page = loader.load();
            FormationController fc = loader.getController();
            fc.setCoachMode(currentUser.getId_user());
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Chargement Formations impossible.");
        }
    }

    @FXML
    void showResultats(MouseEvent event) {
        setActiveNav(navResultats, indicResultats);
        loadCoachView("CoachResultats.fxml", "Résultats des patients");
    }

    @FXML
    void showPatients(MouseEvent event) {
        setActiveNav(navPatients, indicPatients);
        loadCoachView("CoachPatients.fxml", "Suivi des patients");
    }

    @FXML
    void showProfil(MouseEvent event) {
        setActiveNav(navProfil, indicProfil);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            Profil ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setOnPhotoChanged(this::refreshUserData);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le profil.");
        }
    }

    /** Load a coach sub-page, with graceful placeholder fallback */
    private void loadCoachView(String fxmlName, String title) {
        try {
            java.net.URL res = getClass().getResource("/" + fxmlName);
            if (res != null) {
                FXMLLoader loader = new FXMLLoader(res);
                Parent page = loader.load();
                Object ctrl = loader.getController();
                if (ctrl != null) {
                    try { ctrl.getClass().getMethod("setUser", User.class).invoke(ctrl, currentUser); }
                    catch (Exception ignored) {}
                }
                contentArea.getChildren().setAll(page);
            } else {
                contentArea.getChildren().setAll(buildPlaceholder(title));
            }
        } catch (IOException e) {
            e.printStackTrace();
            contentArea.getChildren().setAll(buildPlaceholder(title));
        }
    }

    private ScrollPane buildPlaceholder(String title) {
        VBox box = new VBox(0);
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox();
        header.setStyle("-fx-padding:22 30;-fx-background-color:white;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label h = new Label(title);
        h.setStyle("-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:#2D3748;");
        header.getChildren().add(h);
        VBox content = new VBox(16);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-padding:80;");
        Label icon = new Label("🚧"); icon.setStyle("-fx-font-size:50px;");
        Label msg = new Label("Page en cours de développement");
        msg.setStyle("-fx-font-size:17px;-fx-font-weight:600;-fx-text-fill:#4A5568;");
        Label sub = new Label("Cette section sera bientôt disponible.");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#A0AEC0;");
        content.getChildren().addAll(icon, msg, sub);
        box.getChildren().addAll(header, content);
        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:#F2F0EC;");
        return sp;
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION — hover handlers
    // ════════════════════════════════════════════════════════════════════

    @FXML void onNavDashboardEnter(MouseEvent e)  { if (navDashboard  != currentActiveNav) navDashboard.setStyle(NAV_ACTIVE); }
    @FXML void onNavDashboardExit(MouseEvent e)   { if (navDashboard  != currentActiveNav) navDashboard.setStyle(NAV_NORMAL); }

    @FXML void onNavFormationsEnter(MouseEvent e) { if (navFormations != currentActiveNav) navFormations.setStyle(NAV_ACTIVE); }
    @FXML void onNavFormationsExit(MouseEvent e)  { if (navFormations != currentActiveNav) navFormations.setStyle(NAV_NORMAL); }

    @FXML void onNavResultatsEnter(MouseEvent e)  { if (navResultats  != currentActiveNav) navResultats.setStyle(NAV_ACTIVE); }
    @FXML void onNavResultatsExit(MouseEvent e)   { if (navResultats  != currentActiveNav) navResultats.setStyle(NAV_NORMAL); }

    @FXML void onNavPatientsEnter(MouseEvent e)   { if (navPatients   != currentActiveNav) navPatients.setStyle(NAV_ACTIVE); }
    @FXML void onNavPatientsExit(MouseEvent e)    { if (navPatients   != currentActiveNav) navPatients.setStyle(NAV_NORMAL); }

    @FXML void onNavProfilEnter(MouseEvent e)     { if (navProfil     != currentActiveNav) navProfil.setStyle(NAV_ACTIVE); }
    @FXML void onNavProfilExit(MouseEvent e)      { if (navProfil     != currentActiveNav) navProfil.setStyle(NAV_NORMAL); }

    // ── Card hover ──────────────────────────────────────────────────────
    @FXML void onCardFormationsEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardFormationsExit(MouseEvent e)  { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardResultatsEnter(MouseEvent e)  { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardResultatsExit(MouseEvent e)   { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardPatientsEnter(MouseEvent e)   { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardPatientsExit(MouseEvent e)    { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardProfilEnter(MouseEvent e)     { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardProfilExit(MouseEvent e)      { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }

    // ════════════════════════════════════════════════════════════════════
    //  ACTIVE NAV HELPER
    // ════════════════════════════════════════════════════════════════════

    private void setActiveNav(VBox nav, HBox indic) {
        VBox[]  navs   = { navDashboard, navFormations, navResultats, navPatients, navProfil };
        HBox[]  indics = { indicDashboard, indicFormations, indicResultats, indicPatients, indicProfil };
        for (VBox n : navs)   if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i : indics) if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    // ════════════════════════════════════════════════════════════════════
    //  LOGOUT
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Voulez-vous vraiment quitter ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}
