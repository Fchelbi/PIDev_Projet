package controllers;

import entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import services.*;
import utils.LightDialog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;

public class AdminHome {

    // ── User stat labels ──────────────────────────────────────────────────
    @FXML private Label lblWelcome, lblHeaderAvatar;
    @FXML private Label lblNbPatients, lblNbCoaches, lblNbAdmins, lblNbTotal;
    @FXML private Label lblPctPatients, lblPctCoaches, lblPctAdmins;
    @FXML private ProgressBar pbPatients, pbCoaches, pbAdmins;

    // ── Formation stat labels ─────────────────────────────────────────────
    @FXML private Label lblNbFormations, lblNbAvecVideo, lblNbAvecQuiz, lblNbCategories;

    // ── Charts ────────────────────────────────────────────────────────────
    @FXML private WebView wvUserChart;
    @FXML private WebView wvFormationChart;

    // ── Quote ─────────────────────────────────────────────────────────────
    @FXML private Label lblQuoteStatus, lblQuote, lblQuoteAuthor;

    // ── Layout ────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;
    @FXML private ScrollPane dashboardPane;
    @FXML private ImageView imgHeaderPhoto;

    // ── Nav ───────────────────────────────────────────────────────────────
    @FXML private VBox navDashboard, navUtilisateurs, navFormations, navProfil;
    @FXML private HBox indicDashboard, indicUtilisateurs, indicFormations, indicProfil;

    // ── Services ──────────────────────────────────────────────────────────
    private final serviceUser us = new serviceUser();
    private FormationService   formationService;
    private QuizService        quizService;
    private ParticipantService participantService;
    private QuizResultService  quizResultService;

    private User currentUser;
    private VBox currentActiveNav;

    private static final String NAV_NORMAL  = "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:transparent;-fx-background-radius:0 10 10 0;";
    private static final String NAV_ACTIVE  = "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:0 10 10 0;";
    private static final String INDIC_HIDDEN  = "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:transparent;-fx-background-radius:0 2 2 0;";
    private static final String INDIC_VISIBLE = "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:#E8956D;-fx-background-radius:0 2 2 0;";

    // ════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void initialize() {
        try { formationService   = new FormationService();   } catch (Exception ignored) {}
        try { quizService        = new QuizService();        } catch (Exception ignored) {}
        try { participantService = new ParticipantService(); } catch (Exception ignored) {}
        try { quizResultService  = new QuizResultService();  } catch (Exception ignored) {}
    }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        refreshStats(null);
        loadQuotableQuote();
        setActiveNav(navDashboard, indicDashboard);
    }

    // ════════════════════════════════════════════════════════════════════
    //  USER DATA
    // ════════════════════════════════════════════════════════════════════

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) {
            System.err.println("refreshUserData: " + e.getMessage());
        }
        lblWelcome.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        lblHeaderAvatar.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        updateHeaderPhoto();
    }

    private void updateHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(f.toURI() + "?t=" + System.currentTimeMillis(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblHeaderAvatar.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblHeaderAvatar.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  STATS
    // ════════════════════════════════════════════════════════════════════

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
                if (pbPatients    != null) pbPatients.setProgress(pp);
                if (pbCoaches     != null) pbCoaches.setProgress(pc);
                if (pbAdmins      != null) pbAdmins.setProgress(pa);
                if (lblPctPatients != null) lblPctPatients.setText(String.format("%.0f%%", pp * 100));
                if (lblPctCoaches  != null) lblPctCoaches.setText(String.format("%.0f%%",  pc * 100));
                if (lblPctAdmins   != null) lblPctAdmins.setText(String.format("%.0f%%",   pa * 100));
            }
            loadUserChart(p, c, a);
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les stats utilisateurs.");
        }
    }

    private void loadFormationStats() {
        if (formationService == null) return;
        try {
            List<Formation> formations = formationService.selectALL();
            int total = formations.size();
            int avecVideo = 0, avecQuiz = 0;
            Set<String> categories = new HashSet<>();

            for (Formation f : formations) {
                if (f.getVideoUrl() != null && !f.getVideoUrl().isBlank()) avecVideo++;
                if (f.getCategory() != null && !f.getCategory().isBlank()) categories.add(f.getCategory().trim());
                if (quizService != null) {
                    try { if (quizService.selectByFormation(f.getId()) != null) avecQuiz++; } catch (Exception ignored) {}
                }
            }

            if (lblNbFormations != null) lblNbFormations.setText(String.valueOf(total));
            if (lblNbAvecVideo  != null) lblNbAvecVideo.setText(String.valueOf(avecVideo));
            if (lblNbAvecQuiz   != null) lblNbAvecQuiz.setText(String.valueOf(avecQuiz));
            if (lblNbCategories != null) lblNbCategories.setText(String.valueOf(categories.size()));

            loadFormationDonutChart(formations);

        } catch (SQLException e) {
            System.err.println("Formation stats error: " + e.getMessage());
            for (Label lbl : new Label[]{lblNbFormations, lblNbAvecVideo, lblNbAvecQuiz, lblNbCategories})
                if (lbl != null) lbl.setText("—");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  CHARTS
    //  - User chart: donut (Patients / Coaches / Admins)
    //  - Formation chart: donut (Avec vidéo / Sans vidéo / Avec quiz)
    // ════════════════════════════════════════════════════════════════════

    private void loadUserChart(int patients, int coaches, int admins) {
        if (wvUserChart == null) return;
        int total = patients + coaches + admins;
        if (total == 0) { wvUserChart.getEngine().loadContent("<html><body style='background:transparent;color:#718096;font-family:sans-serif;padding:20px'>Aucune donnée</body></html>"); return; }
        String html = """
        <!DOCTYPE html><html>
        <head>
        <script src="https://www.gstatic.com/charts/loader.js"></script>
        <script>
          google.charts.load('current', {packages: ['corechart']});
          google.charts.setOnLoadCallback(draw);
          function draw() {
            var data = google.visualization.arrayToDataTable([
              ['Rôle', 'Nombre'],
              ['Patients', %d],
              ['Coaches',  %d],
              ['Admins',   %d]
            ]);
            var options = {
              pieHole: 0.45,
              legend: { position: 'bottom', textStyle: { color: '#4A5568', fontSize: 12 } },
              backgroundColor: 'transparent',
              colors: ['#E8956D', '#4A6FA5', '#6C5CE7'],
              chartArea: { width: '90%%', height: '80%%' },
              pieSliceTextStyle: { color: 'white', fontSize: 12 }
            };
            new google.visualization.PieChart(document.getElementById('chart')).draw(data, options);
          }
        </script>
        </head>
        <body style="margin:0;background:transparent;">
          <div id="chart" style="width:100%%;height:270px;"></div>
        </body></html>
        """.formatted(patients, coaches, admins);
        wvUserChart.getEngine().loadContent(html);
    }

    /**
     * Formation donut chart: splits formations by category with enrollment counts.
     * Also shows "avec quiz" vs "sans quiz" breakdown in the legend.
     */
    private void loadFormationDonutChart(List<Formation> formations) {
        if (wvFormationChart == null) return;
        if (formations.isEmpty()) {
            wvFormationChart.getEngine().loadContent("<html><body style='background:transparent;color:#718096;font-family:sans-serif;padding:20px'>Aucune formation</body></html>");
            return;
        }

        // Group formations by category and count enrollments
        Map<String, Long> byCategory = new LinkedHashMap<>();
        try {
            List<Participant> allParts = participantService != null ? participantService.selectALL() : List.of();
            for (Formation f : formations) {
                String cat = f.getCategory() != null && !f.getCategory().isBlank() ? f.getCategory() : "Autre";
                long enrolled = allParts.stream().filter(p -> p.getFormationId() == f.getId()).count();
                byCategory.merge(cat, enrolled + 1, Long::sum); // +1 so empty categories still appear
            }
        } catch (Exception e) {
            for (Formation f : formations) {
                String cat = f.getCategory() != null && !f.getCategory().isBlank() ? f.getCategory() : "Autre";
                byCategory.merge(cat, 1L, Long::sum);
            }
        }

        StringBuilder rows = new StringBuilder();
        byCategory.forEach((cat, count) ->
                rows.append(String.format("['%s', %d],\n", cat.replace("'", "\\'"), count)));

        String html = """
        <!DOCTYPE html><html>
        <head>
        <script src="https://www.gstatic.com/charts/loader.js"></script>
        <script>
          google.charts.load('current', {packages: ['corechart']});
          google.charts.setOnLoadCallback(draw);
          function draw() {
            var data = google.visualization.arrayToDataTable([
              ['Catégorie', 'Inscriptions'],
              %s
            ]);
            var options = {
              pieHole: 0.4,
              legend: { position: 'right', textStyle: { color: '#4A5568', fontSize: 11 } },
              backgroundColor: 'transparent',
              colors: ['#E8956D','#4A6FA5','#6C5CE7','#00B894','#FDCB6E','#E17055','#74B9FF'],
              chartArea: { width: '85%%', height: '82%%' },
              pieSliceTextStyle: { color: 'white', fontSize: 11 },
              title: 'Inscriptions par catégorie',
              titleTextStyle: { color: '#2D3748', fontSize: 12, bold: true }
            };
            new google.visualization.PieChart(document.getElementById('chart')).draw(data, options);
          }
        </script>
        </head>
        <body style="margin:0;background:transparent;">
          <div id="chart" style="width:100%%;height:280px;"></div>
        </body></html>
        """.formatted(rows.toString());
        wvFormationChart.getEngine().loadContent(html);
    }

    // ════════════════════════════════════════════════════════════════════
    //  API 1 : Quotable.io — Citation du jour
    // ════════════════════════════════════════════════════════════════════

    private void loadQuotableQuote() {
        if (lblQuote == null) return;
        Platform.runLater(() -> {
            if (lblQuoteStatus != null) lblQuoteStatus.setText("⏳ Chargement...");
            lblQuote.setText("");
            if (lblQuoteAuthor != null) lblQuoteAuthor.setText("");
        });
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpResponse<String> resp = client.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("https://api.quotable.io/random?tags=inspirational,leadership,wisdom"))
                                .timeout(Duration.ofSeconds(8)).header("User-Agent", "EchoCare/1.0").GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                String content = extractJsonValue(resp.body(), "content");
                String author  = extractJsonValue(resp.body(), "author");
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
        }, "quote-thread").start();
    }

    @FXML void refreshQuote(MouseEvent event) { loadQuotableQuote(); }

    // ════════════════════════════════════════════════════════════════════
    //  API 2 : NumbersAPI — Fun fact about total users count
    // ════════════════════════════════════════════════════════════════════

    private void showNumberFact(int number) {
        new Thread(() -> {
            try {
                HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpResponse<String> resp = c.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://numbersapi.com/" + number + "/trivia?json"))
                                .timeout(Duration.ofSeconds(6)).header("User-Agent", "EchoCare/1.0").GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                String text = extractJsonValue(resp.body(), "text");
                if (!text.isBlank()) {
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("💡 Le saviez-vous ?");
                        a.setHeaderText("À propos du chiffre " + number + " :");
                        a.setContentText(text);
                        a.showAndWait();
                    });
                }
            } catch (Exception ignored) {}
        }, "numbers-thread").start();
    }

    // ════════════════════════════════════════════════════════════════════
    //  API 3 + IA : OpenRouter — AI analysis of platform stats
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void analyzeWithAI(MouseEvent event) {
        try {
            int nbFormations = formationService != null ? formationService.selectALL().size() : 0;
            List<User> users = us.selectALL();
            long nbPatients = users.stream().filter(u -> "PATIENT".equalsIgnoreCase(u.getRole())).count();
            long nbCoaches  = users.stream().filter(u -> "COACH".equalsIgnoreCase(u.getRole())).count();
            long nbResults  = quizResultService != null ? quizResultService.selectALL().size() : 0;
            long nbPassed   = quizResultService != null
                    ? quizResultService.selectALL().stream().filter(QuizResult::isPassed).count() : 0;
            double taux     = nbResults > 0 ? (nbPassed * 100.0 / nbResults) : 0;

            String prompt = String.format(
                    "Tu es un consultant en e-learning. Voici les statistiques de la plateforme EchoCare : " +
                            "%d formations, %d patients, %d coaches, %d tentatives de quiz, taux de réussite %.0f%%. " +
                            "Donne en 3-4 phrases une analyse et 2 recommandations concrètes. Réponds en français, de façon concise.",
                    nbFormations, nbPatients, nbCoaches, nbResults, taux);

            Alert loading = new Alert(Alert.AlertType.INFORMATION);
            loading.setTitle("🤖 Analyse IA");
            loading.setHeaderText("Analyse en cours...");
            loading.setContentText("⏳ L'IA analyse vos statistiques...");
            loading.show();

            new Thread(() -> {
                try {
                    String body = "{\"model\":\"google/gemma-3-1b-it:free\","
                            + "\"messages\":[{\"role\":\"user\",\"content\":\""
                            + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}],"
                            + "\"max_tokens\":400}";
                    HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
                    HttpResponse<String> resp = c.send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                                    .timeout(Duration.ofSeconds(30))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", "Bearer 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")
                                    .header("HTTP-Referer", "https://echocare.app")
                                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                            HttpResponse.BodyHandlers.ofString());

                    String analysis = parseAIResponse(resp.body());
                    Platform.runLater(() -> {
                        loading.close();
                        Alert result = new Alert(Alert.AlertType.INFORMATION);
                        result.setTitle("🤖 Analyse IA — EchoCare");
                        result.setHeaderText("Analyse de vos statistiques");
                        result.setContentText(analysis);
                        result.getDialogPane().setPrefWidth(550);
                        result.showAndWait();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        loading.close();
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Erreur IA");
                        err.setContentText("Impossible de contacter l'IA : " + e.getMessage());
                        err.showAndWait();
                    });
                }
            }, "ai-analysis-thread").start();

        } catch (SQLException e) {
            LightDialog.showError("Erreur", e.getMessage());
        }
    }

    private String parseAIResponse(String raw) {
        int ci = raw.indexOf("\"content\":\"");
        if (ci < 0) return "Analyse non disponible.";
        ci += 11;
        StringBuilder sb = new StringBuilder();
        while (ci < raw.length()) {
            char ch = raw.charAt(ci);
            if (ch == '\\' && ci + 1 < raw.length()) {
                char next = raw.charAt(ci + 1);
                if (next == '"')  { sb.append('"');  ci += 2; continue; }
                if (next == 'n')  { sb.append('\n'); ci += 2; continue; }
                if (next == '\\') { sb.append('\\'); ci += 2; continue; }
            } else if (ch == '"') break;
            sb.append(ch); ci++;
        }
        return sb.toString().isBlank() ? "Analyse non disponible." : sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════
    //  FONCTIONNALITÉ AVANCÉE : Export rapport statistiques texte
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void exportReport(MouseEvent event) {
        try {
            List<User> users = us.selectALL();
            long nbP = users.stream().filter(u -> "PATIENT".equalsIgnoreCase(u.getRole())).count();
            long nbC = users.stream().filter(u -> "COACH".equalsIgnoreCase(u.getRole())).count();
            long nbA = users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).count();
            List<Formation> formations = formationService != null ? formationService.selectALL() : List.of();
            long nbResults = quizResultService != null ? quizResultService.selectALL().size() : 0;
            long nbPassed  = quizResultService != null
                    ? quizResultService.selectALL().stream().filter(QuizResult::isPassed).count() : 0;
            double taux = nbResults > 0 ? (nbPassed * 100.0 / nbResults) : 0;

            String report = String.format("""
                ╔══════════════════════════════════════════╗
                ║        RAPPORT STATISTIQUES ECHOCARE     ║
                ╠══════════════════════════════════════════╣
                ║  Généré le : %s
                ║  Par       : %s %s
                ╠══════════════════════════════════════════╣
                ║  UTILISATEURS                            ║
                ║  • Total        : %d
                ║  • Patients     : %d
                ║  • Coaches      : %d
                ║  • Admins       : %d
                ╠══════════════════════════════════════════╣
                ║  FORMATIONS                              ║
                ║  • Total        : %d
                ╠══════════════════════════════════════════╣
                ║  QUIZ                                    ║
                ║  • Tentatives   : %d
                ║  • Réussites    : %d
                ║  • Taux réussite: %.0f%%
                ╚══════════════════════════════════════════╝
                """,
                    java.time.LocalDate.now(),
                    currentUser.getPrenom(), currentUser.getNom(),
                    users.size(), nbP, nbC, nbA,
                    formations.size(),
                    nbResults, nbPassed, taux);

            String path = System.getProperty("user.home") + "/EchoCare_Rapport_" + java.time.LocalDate.now() + ".txt";
            java.nio.file.Files.writeString(java.nio.file.Path.of(path), report);

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("📄 Rapport exporté");
            a.setHeaderText("Rapport enregistré !");
            a.setContentText("Fichier : " + path);
            ButtonType openBtn = new ButtonType("📂 Ouvrir");
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(openBtn, ok);
            a.showAndWait().ifPresent(b -> {
                if (b == openBtn) {
                    try { java.awt.Desktop.getDesktop().open(new java.io.File(path)); }
                    catch (Exception ignored) {}
                }
            });
            // Also show a fun fact about total user count
            showNumberFact(users.size());

        } catch (Exception e) {
            LightDialog.showError("Erreur export", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════════════

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
            ctrl.setOnBackToAccueil(this::showDashboardFromProfil);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Chargement Profil impossible.");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  HOVER HANDLERS
    // ════════════════════════════════════════════════════════════════════

    @FXML void onNavDashboardEnter(MouseEvent e)   { if (navDashboard    != currentActiveNav) navDashboard.setStyle(NAV_ACTIVE); }
    @FXML void onNavDashboardExit(MouseEvent e)    { if (navDashboard    != currentActiveNav) navDashboard.setStyle(NAV_NORMAL); }
    @FXML void onNavUtilsEnter(MouseEvent e)       { if (navUtilisateurs != currentActiveNav) navUtilisateurs.setStyle(NAV_ACTIVE); }
    @FXML void onNavUtilsExit(MouseEvent e)        { if (navUtilisateurs != currentActiveNav) navUtilisateurs.setStyle(NAV_NORMAL); }
    @FXML void onNavFormationsEnter(MouseEvent e)  { if (navFormations   != currentActiveNav) navFormations.setStyle(NAV_ACTIVE); }
    @FXML void onNavFormationsExit(MouseEvent e)   { if (navFormations   != currentActiveNav) navFormations.setStyle(NAV_NORMAL); }
    @FXML void onNavProfilEnter(MouseEvent e)      { if (navProfil       != currentActiveNav) navProfil.setStyle(NAV_ACTIVE); }
    @FXML void onNavProfilExit(MouseEvent e)       { if (navProfil       != currentActiveNav) navProfil.setStyle(NAV_NORMAL); }

    // ════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════

    private void showDashboardFromProfil() {
        setActiveNav(navDashboard, indicDashboard);
        contentArea.getChildren().setAll(dashboardPane);
        refreshStats(null);
    }

    private void setActiveNav(VBox nav, HBox indic) {
        VBox[] navs   = {navDashboard, navUtilisateurs, navFormations, navProfil};
        HBox[] indics = {indicDashboard, indicUtilisateurs, indicFormations, indicProfil};
        for (VBox n : navs)   if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i : indics) if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    private String extractJsonValue(String json, String key) {
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