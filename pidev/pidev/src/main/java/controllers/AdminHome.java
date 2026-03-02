package controllers;

import entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.FormationService;
import services.QuizService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminHome {

    // ── User stats ──────────────────────────────────────────────────────────
    @FXML private Label lblWelcome, lblHeaderAvatar;
    @FXML private Label lblNbPatients, lblNbCoaches, lblNbAdmins, lblNbTotal;
    @FXML private Label lblPctPatients, lblPctCoaches, lblPctAdmins;
    @FXML private ProgressBar pbPatients, pbCoaches, pbAdmins;

    // ── Formation stats ─────────────────────────────────────────────────────
    @FXML private Label lblNbFormations, lblNbAvecVideo, lblNbAvecQuiz, lblNbCategories;

    // ── Quote API ───────────────────────────────────────────────────────────
    @FXML private Label lblQuoteStatus, lblQuote, lblQuoteAuthor;

    // ── Layout ──────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;
    @FXML private ScrollPane dashboardPane;
    @FXML private ImageView imgHeaderPhoto;

    // ── Nav VBoxes ──────────────────────────────────────────────────────────
    @FXML private VBox navDashboard;
    @FXML private VBox navUtilisateurs;
    @FXML private VBox navFormations;
    @FXML private VBox navProfil;

    // ── Nav indicators ──────────────────────────────────────────────────────
    @FXML private HBox indicDashboard;
    @FXML private HBox indicUtilisateurs;
    @FXML private HBox indicFormations;
    @FXML private HBox indicProfil;

    // ── Services ────────────────────────────────────────────────────────────
    private final serviceUser us = new serviceUser();
    private FormationService formationService;
    private QuizService quizService;

    private User currentUser;
    private VBox currentActiveNav;

    // ── Nav styles ──────────────────────────────────────────────────────────
    private static final String NAV_NORMAL  =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;" +
                    "-fx-background-color:transparent;-fx-background-radius:0 10 10 0;";
    private static final String NAV_ACTIVE  =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;" +
                    "-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:0 10 10 0;";
    private static final String INDIC_HIDDEN  =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;" +
                    "-fx-background-color:transparent;-fx-background-radius:0 2 2 0;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;" +
                    "-fx-background-color:#E8956D;-fx-background-radius:0 2 2 0;";

    // ════════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    void initialize() {
        try { formationService = new FormationService(); } catch (Exception ignored) {}
        try { quizService      = new QuizService();      } catch (Exception ignored) {}
        System.out.println("AdminHome initialized");
    }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        refreshStats(null);
        loadQuotableQuote();
        setActiveNav(navDashboard, indicDashboard);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  USER DATA
    // ════════════════════════════════════════════════════════════════════════

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) { System.err.println("refreshUserData: " + e.getMessage()); }
        lblWelcome.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        lblHeaderAvatar.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        updateHeaderPhoto();
    }

    private void updateHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(
                        f.toURI() + "?t=" + System.currentTimeMillis(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblHeaderAvatar.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblHeaderAvatar.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STATS
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    void refreshStats(ActionEvent event) {
        loadUserStats();
        loadFormationStats();
    }

    private void loadUserStats() {
        try {
            List<User> users = us.selectALL();
            int p = 0, c = 0, a = 0;
            for (User u : users) {
                switch (u.getRole().toUpperCase()) {
                    case "PATIENT" -> p++;
                    case "COACH"   -> c++;
                    case "ADMIN"   -> a++;
                }
            }
            int total = users.size();
            lblNbPatients.setText(String.valueOf(p));
            lblNbCoaches.setText(String.valueOf(c));
            lblNbAdmins.setText(String.valueOf(a));
            lblNbTotal.setText(String.valueOf(total));
            if (total > 0) {
                double pp = p / (double) total;
                double pc = c / (double) total;
                double pa = a / (double) total;
                if (pbPatients     != null) pbPatients.setProgress(pp);
                if (pbCoaches      != null) pbCoaches.setProgress(pc);
                if (pbAdmins       != null) pbAdmins.setProgress(pa);
                if (lblPctPatients != null) lblPctPatients.setText(String.format("%.0f%%", pp * 100));
                if (lblPctCoaches  != null) lblPctCoaches.setText(String.format("%.0f%%", pc * 100));
                if (lblPctAdmins   != null) lblPctAdmins.setText(String.format("%.0f%%", pa * 100));
            }
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les stats utilisateurs.");
        }
    }

    private void loadFormationStats() {
        if (formationService == null) return;
        try {
            List<Formation> formations = formationService.selectALL();
            int total     = formations.size();
            int avecVideo = 0;
            int avecQuiz  = 0;
            Set<String> categories = new HashSet<>();
            for (Formation f : formations) {
                if (f.getVideoUrl() != null && !f.getVideoUrl().trim().isEmpty()) avecVideo++;
                if (f.getCategory() != null && !f.getCategory().trim().isEmpty())
                    categories.add(f.getCategory().trim());
                if (quizService != null) {
                    try { if (quizService.selectByFormation(f.getId()) != null) avecQuiz++; }
                    catch (Exception ignored) {}
                }
            }
            if (lblNbFormations != null) lblNbFormations.setText(String.valueOf(total));
            if (lblNbAvecVideo  != null) lblNbAvecVideo.setText(String.valueOf(avecVideo));
            if (lblNbAvecQuiz   != null) lblNbAvecQuiz.setText(String.valueOf(avecQuiz));
            if (lblNbCategories != null) lblNbCategories.setText(String.valueOf(categories.size()));
        } catch (SQLException e) {
            if (lblNbFormations != null) lblNbFormations.setText("—");
            if (lblNbAvecVideo  != null) lblNbAvecVideo.setText("—");
            if (lblNbAvecQuiz   != null) lblNbAvecQuiz.setText("—");
            if (lblNbCategories != null) lblNbCategories.setText("—");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  QUOTABLE API
    // ════════════════════════════════════════════════════════════════════════

    private void loadQuotableQuote() {
        if (lblQuote == null) return;
        Platform.runLater(() -> {
            if (lblQuoteStatus != null) lblQuoteStatus.setText("⏳ Chargement...");
            lblQuote.setText("");
            if (lblQuoteAuthor != null) lblQuoteAuthor.setText("");
        });
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.quotable.io/random?tags=inspirational,leadership,wisdom"))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "EchoCare/1.0").GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String content = extractValue(resp.body(), "content");
                String author  = extractValue(resp.body(), "author");
                Platform.runLater(() -> {
                    if (lblQuoteStatus != null) lblQuoteStatus.setText("💬 Citation du jour");
                    lblQuote.setText("❝  " + content + "  ❞");
                    if (lblQuoteAuthor != null) lblQuoteAuthor.setText("— " + author);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblQuoteStatus != null) lblQuoteStatus.setText("💬 Citation");
                    lblQuote.setText("❝  Le succès n'est pas la clé du bonheur. Le bonheur est la clé du succès.  ❞");
                    if (lblQuoteAuthor != null) lblQuoteAuthor.setText("— Albert Schweitzer");
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
        return end < 0 ? "" : json.substring(i, end)
                .replace("\\u2019", "'").replace("\\u2018", "'")
                .replace("\\u201c", "\"").replace("\\u201d", "\"")
                .replace("\\n", " ");
    }

    @FXML void refreshQuote(MouseEvent event) { loadQuotableQuote(); }

    // ════════════════════════════════════════════════════════════════════════
    //  NAVIGATION — click handlers
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    void showDashboard(MouseEvent event) {
        setActiveNav(navDashboard, indicDashboard);
        contentArea.getChildren().setAll(dashboardPane);
        refreshStats(null);
        loadQuotableQuote();
    }

    @FXML
    void showUtilisateurs(MouseEvent event) {
        try {
            setActiveNav(navUtilisateurs, indicUtilisateurs);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionUtilisateurs.fxml"));
            VBox page = loader.load();
            ((GestionUtilisateurs) loader.getController()).setCurrentUser(currentUser);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Chargement Utilisateurs impossible.");
        }
    }

    @FXML
    void showFormations(MouseEvent event) {
        try {
            setActiveNav(navFormations, indicFormations);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormationView.fxml"));
            Parent page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Chargement Formations impossible.");
        }
    }

    @FXML
    void showProfil(MouseEvent event) {
        try {
            setActiveNav(navProfil, indicProfil);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            Profil ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setOnPhotoChanged(this::refreshUserData);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Chargement Profil impossible.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NAVIGATION — hover handlers
    // ════════════════════════════════════════════════════════════════════════

    @FXML void onNavDashboardEnter(MouseEvent e) {
        if (navDashboard != currentActiveNav) navDashboard.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavDashboardExit(MouseEvent e) {
        if (navDashboard != currentActiveNav) navDashboard.setStyle(NAV_NORMAL);
    }

    @FXML void onNavUtilsEnter(MouseEvent e) {
        if (navUtilisateurs != currentActiveNav) navUtilisateurs.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavUtilsExit(MouseEvent e) {
        if (navUtilisateurs != currentActiveNav) navUtilisateurs.setStyle(NAV_NORMAL);
    }

    @FXML void onNavFormationsEnter(MouseEvent e) {
        if (navFormations != currentActiveNav) navFormations.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavFormationsExit(MouseEvent e) {
        if (navFormations != currentActiveNav) navFormations.setStyle(NAV_NORMAL);
    }

    @FXML void onNavProfilEnter(MouseEvent e) {
        if (navProfil != currentActiveNav) navProfil.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavProfilExit(MouseEvent e) {
        if (navProfil != currentActiveNav) navProfil.setStyle(NAV_NORMAL);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ACTIVE NAV HELPER
    // ════════════════════════════════════════════════════════════════════════

    private void setActiveNav(VBox nav, HBox indic) {
        VBox[] navs   = { navDashboard, navUtilisateurs, navFormations, navProfil };
        HBox[] indics = { indicDashboard, indicUtilisateurs, indicFormations, indicProfil };
        for (VBox n  : navs)   if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i  : indics) if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LOGOUT
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Voulez-vous vraiment quitter ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHART — commented out until Participant & QuizResult entities exist
    // ════════════════════════════════════════════════════════════════════════
    /*
    private VBox buildFormationChart() throws SQLException {
        // TODO: Uncomment when Participant and QuizResult entities/services are created
        // Requires: participantService, quizResultService, Participant entity, QuizResult entity
    }
    */
}