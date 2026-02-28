package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import entities.User;
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

public class AdminHome {

    @FXML private Label lblWelcome;
    @FXML private Label lblHeaderAvatar;
    @FXML private Label lblNbPatients;
    @FXML private Label lblNbCoaches;
    @FXML private Label lblNbAdmins;
    @FXML private Label lblNbTotal;
    @FXML private Label lblQuote;
    @FXML private Label lblQuoteAuthor;
    @FXML private Label lblQuoteStatus;

    @FXML private BarChart<String, Number> barChartRoles;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane dashboardPane;
    @FXML private ImageView imgHeaderPhoto;

    // Sidebar nav items
    @FXML private VBox navDashboard;
    @FXML private VBox navUtilisateurs;
    @FXML private VBox navProfil;

    // Orange indicators
    @FXML private HBox indicDashboard;
    @FXML private HBox indicUtilisateurs;
    @FXML private HBox indicProfil;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private VBox currentActiveNav;

    // ─── Styles ──────────────────────────────────────────────
    private static final String NAV_NORMAL =
            "-fx-padding: 12 20 12 20; -fx-cursor: hand; -fx-background-color: transparent; -fx-background-radius: 10;";
    private static final String NAV_ACTIVE =
            "-fx-padding: 12 20 12 20; -fx-cursor: hand; " +
                    "-fx-background-color: rgba(255,255,255,0.13); -fx-background-radius: 10;";
    private static final String INDIC_HIDDEN =
            "-fx-min-width: 4; -fx-max-width: 4; -fx-min-height: 30; -fx-background-color: transparent; -fx-background-radius: 2;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width: 4; -fx-max-width: 4; -fx-min-height: 30; -fx-background-color: #E8956D; -fx-background-radius: 2;";

    @FXML
    void initialize() {
        System.out.println("✅ AdminHome initialized");
    }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        refreshStats(null);
        loadQuoteOfDay();
        setActiveNav(navDashboard, indicDashboard);
    }

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) {
            System.err.println("Refresh error: " + e.getMessage());
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

    // ─── Stats ────────────────────────────────────────────────
    @FXML
    void refreshStats(ActionEvent event) {
        try {
            List<User> users = us.selectALL();
            int p = 0, c = 0, a = 0;
            for (User u : users) {
                switch (u.getRole().toUpperCase()) {
                    case "PATIENT": p++; break;
                    case "COACH":   c++; break;
                    case "ADMIN":   a++; break;
                }
            }
            int fp = p, fc = c, fa = a;
            lblNbPatients.setText(String.valueOf(p));
            lblNbCoaches.setText(String.valueOf(c));
            lblNbAdmins.setText(String.valueOf(a));
            lblNbTotal.setText(String.valueOf(users.size()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Utilisateurs");
            series.getData().add(new XYChart.Data<>("Patients", (Number) fp));
            series.getData().add(new XYChart.Data<>("Coaches",  (Number) fc));
            series.getData().add(new XYChart.Data<>("Admins",   (Number) fa));
            barChartRoles.getData().clear();
            barChartRoles.getData().add(series);
            Platform.runLater(() -> {
                if (series.getData().size() >= 3) {
                    series.getData().get(0).getNode().setStyle("-fx-bar-fill: #E8956D;");
                    series.getData().get(1).getNode().setStyle("-fx-bar-fill: #F5C87A;");
                    series.getData().get(2).getNode().setStyle("-fx-bar-fill: #4A6FA5;");
                }
            });
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les stats.");
        }
    }

    // ─── ZenQuotes API ───────────────────────────────────────
    private void loadQuoteOfDay() {
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
                        .uri(URI.create("https://zenquotes.io/api/random"))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "EchoCare/1.0")
                        .GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String quote  = extractJson(resp.body(), "q");
                String author = extractJson(resp.body(), "a");
                Platform.runLater(() -> {
                    if (lblQuoteStatus != null) lblQuoteStatus.setText("✨ Citation du jour");
                    lblQuote.setText("\"" + quote + "\"");
                    if (lblQuoteAuthor != null) lblQuoteAuthor.setText("— " + author);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblQuoteStatus != null) lblQuoteStatus.setText("💬 Citation");
                    lblQuote.setText("\"La santé mentale est aussi importante que la santé physique.\"");
                    if (lblQuoteAuthor != null) lblQuoteAuthor.setText("— EchoCare");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private String extractJson(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return "";
        i += s.length();
        int end = json.indexOf("\"", i);
        if (end < 0) return "";
        return json.substring(i, end)
                .replace("\\u2019","'").replace("\\u2018","'")
                .replace("\\u201c","\"").replace("\\u201d","\"");
    }

    @FXML
    void refreshQuote(MouseEvent event) { loadQuoteOfDay(); }

    // ─── Navigation (MouseEvent for onMouseClicked) ──────────
    @FXML
    void showDashboard(MouseEvent event) {
        setActiveNav(navDashboard, indicDashboard);
        contentArea.getChildren().setAll(dashboardPane);
        refreshStats(null);
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
            LightDialog.showError("Erreur", "Chargement impossible.");
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
            LightDialog.showError("Erreur", "Chargement impossible.");
        }
    }

    // ─── Active nav + BOTH indicator & bg simultaneously ─────
    private void setActiveNav(VBox nav, HBox indic) {
        // Reset all
        VBox[] navs  = {navDashboard, navUtilisateurs, navProfil};
        HBox[] indics = {indicDashboard, indicUtilisateurs, indicProfil};
        for (VBox n : navs)  { if (n != null) n.setStyle(NAV_NORMAL); }
        for (HBox i : indics){ if (i != null) i.setStyle(INDIC_HIDDEN); }
        // Apply active — BOTH at the same time
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    // ─── Hover effects ────────────────────────────────────────
    @FXML void onNavDashboardEnter(MouseEvent e)  { if(navDashboard  !=currentActiveNav) navDashboard.setStyle(NAV_ACTIVE); }
    @FXML void onNavDashboardExit(MouseEvent e)   { if(navDashboard  !=currentActiveNav) navDashboard.setStyle(NAV_NORMAL); }
    @FXML void onNavUtilsEnter(MouseEvent e)      { if(navUtilisateurs!=currentActiveNav) navUtilisateurs.setStyle(NAV_ACTIVE); }
    @FXML void onNavUtilsExit(MouseEvent e)       { if(navUtilisateurs!=currentActiveNav) navUtilisateurs.setStyle(NAV_NORMAL); }
    @FXML void onNavProfilEnter(MouseEvent e)     { if(navProfil     !=currentActiveNav) navProfil.setStyle(NAV_ACTIVE); }
    @FXML void onNavProfilExit(MouseEvent e)      { if(navProfil     !=currentActiveNav) navProfil.setStyle(NAV_NORMAL); }

    // ─── Map in content area ──────────────────────────────────
    @FXML
    void loadMapSection(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Map.fxml"));
            VBox page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger la carte.");
        }
    }

    // ─── Logout ───────────────────────────────────────────────
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