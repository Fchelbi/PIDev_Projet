package controllers;

import entities.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.Bienetreservice;

import java.util.List;

/**
 * ✅ BienEtreController
 * Page "Bien-être du Jour" pour le patient
 * - Horoscope via Aztro API (gratuit, zéro clé)
 * - Actualités santé mentale (fallback statique haute qualité)
 */
public class Bienetrecontroller {

    @FXML private VBox  rootContainer;
    @FXML private Label lblDate;
    @FXML private Label lblSignDisplay;
    @FXML private ComboBox<String> cbSign;

    // Horoscope cards
    @FXML private Label lblSignName, lblSignEmoji, lblSignRange;
    @FXML private Label lblMood, lblColor, lblLucky, lblCompatibility;
    @FXML private Label lblHoroDesc;
    @FXML private Label lblWellbeingTip;
    @FXML private HBox  moodBar;
    @FXML private ProgressIndicator loadingHoro;

    // News
    @FXML private VBox  newsContainer;
    @FXML private ProgressIndicator loadingNews;

    private User currentUser;
    private final Bienetreservice service = new Bienetreservice();

    // Signe sélectionné (persistant dans la session)
    private String currentSign = "scorpio";

    public void setUser(User user) {
        this.currentUser = user;
        loadPage();
    }

    @FXML
    void initialize() {
        // Date du jour
        if (lblDate != null)
            lblDate.setText(service.getTodayDate());

        // ComboBox signes
        if (cbSign != null) {
            cbSign.getItems().addAll(
                    "Bélier ♈", "Taureau ♉", "Gémeaux ♊", "Cancer ♋",
                    "Lion ♌", "Vierge ♍", "Balance ♎", "Scorpion ♏",
                    "Sagittaire ♐", "Capricorne ♑", "Verseau ♒", "Poissons ♓"
            );
            cbSign.setValue("Scorpion ♏");
            cbSign.setOnAction(e -> {
                currentSign = getSignKey(cbSign.getValue());
                loadHoroscope();
            });
        }
    }

    private void loadPage() {
        loadHoroscope();
        loadNews();
    }

    // ══════════════════════════════════════════════════════════
    //  HOROSCOPE — Aztro API async
    // ══════════════════════════════════════════════════════════

    private void loadHoroscope() {
        if (loadingHoro != null) { loadingHoro.setVisible(true); loadingHoro.setManaged(true); }

        Task<Bienetreservice.HoroscopeResult> task = new Task<>() {
            @Override protected Bienetreservice.HoroscopeResult call() {
                return service.fetchHoroscope(currentSign);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (loadingHoro != null) { loadingHoro.setVisible(false); loadingHoro.setManaged(false); }
            displayHoroscope(task.getValue());
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (loadingHoro != null) { loadingHoro.setVisible(false); loadingHoro.setManaged(false); }
        }));

        new Thread(task).start();
    }

    private void displayHoroscope(Bienetreservice.HoroscopeResult r) {
        if (r == null) return;

        if (lblSignEmoji != null) lblSignEmoji.setText(r.signEmoji);
        if (lblSignName  != null) lblSignName.setText(service.getSignName(currentSign));
        if (lblSignRange != null) lblSignRange.setText(r.dateRange);
        if (lblHoroDesc  != null) { lblHoroDesc.setText(r.description); lblHoroDesc.setWrapText(true); }
        if (lblWellbeingTip != null) { lblWellbeingTip.setText(r.wellbeingTip); lblWellbeingTip.setWrapText(true); }

        // Mini stats
        if (lblMood          != null) lblMood.setText(r.mood);
        if (lblColor         != null) lblColor.setText(r.color);
        if (lblLucky         != null) lblLucky.setText(r.luckyNumber + "  •  " + r.luckyTime);
        if (lblCompatibility != null) lblCompatibility.setText(r.compatibility);

        // Source badge
        if (lblSignDisplay != null)
            lblSignDisplay.setText(r.fromApi ? "✅ Aztro API" : "📋 Données EchoCare");
    }

    // ══════════════════════════════════════════════════════════
    //  NEWS — async
    // ══════════════════════════════════════════════════════════

    private void loadNews() {
        if (loadingNews != null) { loadingNews.setVisible(true); loadingNews.setManaged(true); }

        Task<List<Bienetreservice.NewsArticle>> task = new Task<>() {
            @Override protected List<Bienetreservice.NewsArticle> call() {
                return service.fetchMentalHealthNews();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (loadingNews != null) { loadingNews.setVisible(false); loadingNews.setManaged(false); }
            displayNews(task.getValue());
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (loadingNews != null) { loadingNews.setVisible(false); loadingNews.setManaged(false); }
        }));

        new Thread(task).start();
    }

    private void displayNews(List<Bienetreservice.NewsArticle> articles) {
        if (newsContainer == null || articles == null) return;
        newsContainer.getChildren().clear();

        for (Bienetreservice.NewsArticle article : articles) {
            newsContainer.getChildren().add(buildArticleCard(article));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  BUILDERS UI (pour les éléments dynamiques)
    // ══════════════════════════════════════════════════════════

    private VBox buildArticleCard(Bienetreservice.NewsArticle a) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 2);");

        // Top row: emoji + category badge + read time
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label emojiLbl = new Label(a.emoji);
        emojiLbl.setStyle("-fx-font-size: 22px;");

        Label catBadge = new Label(a.category);
        catBadge.setStyle("-fx-background-color: " + a.categoryColor + "22; " +
                "-fx-text-fill: " + a.categoryColor + "; -fx-font-size: 10px; " +
                "-fx-font-weight: 700; -fx-padding: 3 10; -fx-background-radius: 20; " +
                "-fx-border-color: " + a.categoryColor + "44; -fx-border-radius: 20;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label readTimeLbl = new Label("\uD83D\uDD50 " + a.readTime);
        readTimeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #A0AEC0;");

        topRow.getChildren().addAll(emojiLbl, catBadge, spacer, readTimeLbl);

        // Title
        Label titleLbl = new Label(a.title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");
        titleLbl.setWrapText(true);

        // Summary
        Label summaryLbl = new Label(a.summary);
        summaryLbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #718096; -fx-line-spacing: 2;");
        summaryLbl.setWrapText(true);
        summaryLbl.setMaxHeight(80);

        // Source
        Label sourceLbl = new Label("Source : " + a.source);
        sourceLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #A0AEC0; -fx-font-style: italic;");

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #EDF2F7;");

        card.getChildren().addAll(topRow, titleLbl, summaryLbl, sep, sourceLbl);
        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  REFRESH BUTTON
    // ══════════════════════════════════════════════════════════

    @FXML
    void handleRefresh() {
        loadPage();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPER
    // ══════════════════════════════════════════════════════════

    private String getSignKey(String displayName) {
        return switch (displayName) {
            case "Bélier ♈"     -> "aries";
            case "Taureau ♉"    -> "taurus";
            case "Gémeaux ♊"    -> "gemini";
            case "Cancer ♋"     -> "cancer";
            case "Lion ♌"       -> "leo";
            case "Vierge ♍"     -> "virgo";
            case "Balance ♎"    -> "libra";
            case "Scorpion ♏"   -> "scorpio";
            case "Sagittaire ♐" -> "sagittarius";
            case "Capricorne ♑" -> "capricorn";
            case "Verseau ♒"    -> "aquarius";
            case "Poissons ♓"   -> "pisces";
            default             -> "scorpio";
        };
    }
}