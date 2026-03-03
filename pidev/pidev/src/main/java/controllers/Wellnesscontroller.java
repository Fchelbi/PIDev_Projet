package controllers;

import entities.User;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Wellnesscontroller {

    @FXML private VBox rootPane;

    private User currentUser;
    private WebEngine webEngine;
    private StackPane tabContent;
    private VBox tabMusique, tabDefis;
    private HBox indicMusique, indicDefis;
    private Label lblTabMusique, lblTabDefis;

    // Defis state
    private final boolean[] defisDone = new boolean[7];
    private int totalDone = 0;
    private int prevBadgeLevel = 0;
    private Label lblStreak, lblProgression, lblBadge;
    private ProgressBar progressBar;
    private Label pctLabel;
    private final List<VBox> badgeCards = new ArrayList<>();

    // Spotify playlists
    private static final Map<String, String[]> PLAYLISTS = new LinkedHashMap<>();
    static {
        PLAYLISTS.put("Calme",      new String[]{"playlist/37i9dQZF1DX3Ogo9pFvBkY","playlist/37i9dQZF1DWZqd5JICZI0u","playlist/37i9dQZF1DX4E3UdUs7fUx"});
        PLAYLISTS.put("Energie",    new String[]{"playlist/37i9dQZF1DX76Wlfdnj7AP","playlist/37i9dQZF1DX0XUsuxWHRQd","playlist/37i9dQZF1DXdxcBWuJkbcy"});
        PLAYLISTS.put("Tristesse",  new String[]{"playlist/37i9dQZF1DX7qK8ma5wgG1","playlist/37i9dQZF1DWVrtsSlLKzro","playlist/37i9dQZF1DX3YSRoSdA634"});
        PLAYLISTS.put("Stress",     new String[]{"playlist/37i9dQZF1DX9uKNf5jGX6m","playlist/37i9dQZF1DWTC29eJKOqGJ","playlist/37i9dQZF1DX4PP3DA4J0N8"});
        PLAYLISTS.put("Focus",      new String[]{"playlist/37i9dQZF1DX8NTLI2TtZa6","playlist/37i9dQZF1DX4sWSpwq3LiO","playlist/37i9dQZF1DWZeNpW2ELTJ7"});
        PLAYLISTS.put("Sommeil",    new String[]{"playlist/37i9dQZF1DWZd79rJ6a7lp","playlist/37i9dQZF1DX4sWSpwq3LiO","playlist/37i9dQZF1DX9RwzeB9EnUK"});
    }

    private static final String[] MOOD_EMOJI  = {"[Calme]", "[Energie]", "[Triste]", "[Stress]", "[Focus]", "[Sommeil]"};
    private static final String[] MOOD_LABEL  = {"Calme & S\u00e9r\u00e9nit\u00e9", "\u00c9nergie & Motivation", "Tristesse & R\u00e9confort", "Stress & Anxi\u00e9t\u00e9", "Focus & Concentration", "Sommeil & D\u00e9tente"};
    private static final String[] MOOD_COLOR  = {"#7B9ED9","#E8956D","#4A6FA5","#52B788","#F5C87A","#9B8EC4"};
    private static final String[] MOOD_KEY    = {"Calme","Energie","Tristesse","Stress","Focus","Sommeil"};

    private static final String[] DEFI_EMOJI = {"[Reveil]","[Eau]","[Marche]","[Journal]","[Phone]","[Connexion]","[Creativite]"};
    private static final String[] DEFI_NAME  = {"R\u00e9veil conscient","Hydratation","10 minutes dehors","Journal de gratitude","Digital detox","Connexion humaine","Cr\u00e9ativit\u00e9 libre"};
    private static final String[] DEFI_DESC  = {
            "Prends 5 minutes ce matin pour respirer profond\u00e9ment avant de regarder ton t\u00e9l\u00e9phone.",
            "Bois 8 verres d'eau aujourd'hui. Commence d\u00e8s maintenant avec un grand verre.",
            "Fais une courte marche de 10 minutes sans \u00e9couteurs. Observe ce qui t'entoure.",
            "\u00c9cris 3 choses pour lesquelles tu es reconnaissant(e) aujourd'hui.",
            "Mets ton t\u00e9l\u00e9phone en mode silencieux pendant 1 heure et profite du moment pr\u00e9sent.",
            "Envoie un message sinc\u00e8re \u00e0 quelqu'un que tu n'as pas contact\u00e9 depuis longtemps.",
            "Consacre 15 minutes \u00e0 une activit\u00e9 cr\u00e9ative : dessiner, \u00e9crire, chanter, cuisiner..."
    };

    private static final String[] BADGE_EMOJI  = {"[G]","[P]","[F]","[E]"};
    private static final String[] BADGE_NAME   = {"Graine","Pousse","Floraison","Etoile"};
    private static final String[] BADGE_COND   = {"1 defi","3 defis","5 defis","7 defis - Parfait!"};
    private static final int[]    BADGE_THRESH = {1,3,5,7};
    private static final String[] BADGE_COLOR  = {"#52B788","#7B9ED9","#E8956D","#F5C87A"};

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    void initialize() {
        buildUI();
        showMusiqueTab();
    }

    // =========================================================
    //  BUILD UI
    // =========================================================

    private void buildUI() {
        rootPane.getChildren().clear();
        rootPane.setStyle("-fx-background-color: #F8F4EF;");

        // Header
        VBox header = new VBox(6);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #3D5A8A, #4A6FA5); -fx-padding: 24 32 18 32;");

        Label title = new Label("Espace Bien-etre");
        title.setStyle("-fx-font-size: 23px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label sub = new Label("Musique th\u00e9rapeutique & d\u00e9fis quotidiens");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7);");

        HBox streakRow = new HBox(10);
        streakRow.setAlignment(Pos.CENTER_LEFT);
        streakRow.setPadding(new Insets(6, 0, 0, 0));

        lblStreak = new Label("Streak : 0 jour");
        lblStreak.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85); -fx-background-color: rgba(255,255,255,0.12); -fx-padding: 4 12; -fx-background-radius: 20;");

        lblBadge = new Label("Badge : Graine");
        lblBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85); -fx-background-color: rgba(255,255,255,0.12); -fx-padding: 4 12; -fx-background-radius: 20;");

        streakRow.getChildren().addAll(lblStreak, lblBadge);
        header.getChildren().addAll(title, sub, streakRow);

        // Tab bar
        HBox tabBar = new HBox(0);
        tabBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");

        tabMusique = makeTab("[Musique]", "Musique", true);
        tabDefis   = makeTab("[Defis]", "D\u00e9fis", false);
        tabMusique.setOnMouseClicked(e -> showMusiqueTab());
        tabDefis.setOnMouseClicked(e -> showDefisTab());
        tabBar.getChildren().addAll(tabMusique, tabDefis);

        tabContent = new StackPane();
        tabContent.setStyle("-fx-background-color: #F8F4EF;");
        VBox.setVgrow(tabContent, Priority.ALWAYS);

        rootPane.getChildren().addAll(header, tabBar, tabContent);
    }

    private VBox makeTab(String emoji, String text, boolean active) {
        VBox tab = new VBox(0);
        tab.setAlignment(Pos.CENTER);
        tab.setPrefWidth(200);
        tab.setCursor(javafx.scene.Cursor.HAND);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(14, 20, 10, 20));

        Label el = new Label(emoji);
        el.setStyle("-fx-font-size: 16px;");

        Label tl = new Label(text);
        tl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + (active ? "#3D5A8A" : "#A0AEC0") + ";");
        row.getChildren().addAll(el, tl);

        HBox indic = new HBox();
        indic.setPrefHeight(3);
        indic.setStyle("-fx-background-color: " + (active ? "#4A6FA5" : "transparent") + "; -fx-background-radius: 2;");

        tab.getChildren().addAll(row, indic);

        if ("[Musique]".equals(emoji)) {
            indicMusique = indic;
            lblTabMusique = tl;
        } else {
            indicDefis = indic;
            lblTabDefis = tl;
        }
        return tab;
    }

    // =========================================================
    //  TAB 1 - MUSIQUE
    // =========================================================

    private void showMusiqueTab() {
        setActiveTab(true);
        tabContent.getChildren().clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #F8F4EF; -fx-background-color: #F8F4EF;");

        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setStyle("-fx-background-color: #F8F4EF;");

        Label q = new Label("Comment te sens-tu en ce moment ?");
        q.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        FlowPane moodGrid = new FlowPane(12, 12);

        WebView webView = new WebView();
        webView.setPrefHeight(310);
        webEngine = webView.getEngine();

        VBox playerBox = new VBox(12);
        playerBox.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 3);");

        Label playerTitle = new Label("Selectionne une humeur");
        playerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        HBox trackBar = new HBox(8);
        trackBar.setAlignment(Pos.CENTER_LEFT);

        playerBox.getChildren().addAll(playerTitle, webView, trackBar);

        for (int m = 0; m < MOOD_KEY.length; m++) {
            final int idx = m;
            final String[] urls = PLAYLISTS.get(MOOD_KEY[m]);
            Button btn = new Button(MOOD_EMOJI[m] + "  " + MOOD_LABEL[m]);
            btn.setStyle(btnMoodNormal());
            btn.setCursor(javafx.scene.Cursor.HAND);
            btn.setOnMouseEntered(e -> btn.setStyle(btnMoodHover()));
            btn.setOnMouseExited(e -> btn.setStyle(btnMoodNormal()));
            btn.setOnAction(e -> {
                moodGrid.getChildren().forEach(n -> { if (n instanceof Button b) b.setStyle(btnMoodNormal()); });
                btn.setStyle("-fx-background-color: " + MOOD_COLOR[idx] + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 10 18; -fx-background-radius: 25; -fx-cursor: hand;");
                playerTitle.setText("" + MOOD_LABEL[idx]);
                loadTrack(urls[0], trackBar, urls);
            });
            moodGrid.getChildren().add(btn);
        }

        page.getChildren().addAll(q, moodGrid, playerBox, buildTips());
        scroll.setContent(page);
        tabContent.getChildren().add(scroll);
    }

    private void loadTrack(String path, HBox trackBar, String[] urls) {
        webEngine.loadContent(spotifyHtml(path));
        trackBar.getChildren().clear();
        Label lbl = new Label("Piste :");
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0AEC0;");
        trackBar.getChildren().add(lbl);
        for (int i = 0; i < urls.length; i++) {
            final String u = urls[i];
            final int num = i + 1;
            Button tb = new Button(num + "");
            tb.setStyle(btnTrackNormal());
            tb.setOnAction(e -> {
                trackBar.getChildren().forEach(n -> { if (n instanceof Button b) b.setStyle(btnTrackNormal()); });
                tb.setStyle(btnTrackActive());
                webEngine.loadContent(spotifyHtml(u));
            });
            trackBar.getChildren().add(tb);
        }
    }

    private String spotifyHtml(String path) {
        String url = "https://open.spotify.com/embed/" + path + "?utm_source=generator&theme=0";
        return "<!DOCTYPE html><html><head>"
                + "<style>*{margin:0;padding:0;}body{background:#121212;}iframe{width:100%;height:300px;border:none;border-radius:12px;}</style>"
                + "</head><body>"
                + "<iframe src='" + url + "' allow='autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture' loading='lazy'></iframe>"
                + "</body></html>";
    }

    private VBox buildTips() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: #EEF4FF; -fx-background-radius: 14; -fx-padding: 18 20; -fx-border-color: #D5E3F5; -fx-border-radius: 14; -fx-border-width: 1;");
        Label t = new Label("Le saviez-vous ?");
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #3D5A8A;");
        box.getChildren().add(t);
        String[] tips = {
                "La musicoth\u00e9rapie r\u00e9duit l'anxi\u00e9t\u00e9 de 65% selon l'American Music Therapy Association.",
                "Ecouter 30 min de musique apaisante diminue le cortisol de fa\u00e7on mesurable.",
                "Les fr\u00e9quences Binaural Beats 1-4 Hz favorisent un sommeil profond.",
                "\u26A1  La musique ryhtm\u00e9e \u00e0 120-140 BPM am\u00e9liore les performances cognitives."
        };
        for (String tip : tips) {
            Label l = new Label(tip);
            l.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #4A5568;");
            l.setWrapText(true);
            box.getChildren().add(l);
        }
        return box;
    }

    // =========================================================
    //  TAB 2 - DEFIS
    // =========================================================

    private void showDefisTab() {
        setActiveTab(false);
        tabContent.getChildren().clear();
        badgeCards.clear();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #F8F4EF; -fx-background-color: #F8F4EF;");

        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 28, 40, 28));
        page.setStyle("-fx-background-color: #F8F4EF;");

        page.getChildren().add(buildProgressCard());
        page.getChildren().add(buildWeekRow());

        VBox defiList = new VBox(12);
        for (int i = 0; i < 7; i++) {
            defiList.getChildren().add(buildDefiCard(i));
        }
        page.getChildren().add(defiList);
        page.getChildren().add(buildBadgesSection());

        scroll.setContent(page);
        tabContent.getChildren().add(scroll);
        updateProgress();
    }

    private VBox buildProgressCard() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #3D5A8A, #4A6FA5); -fx-background-radius: 16; -fx-padding: 20 24;");

        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(4);
        HBox.setHgrow(left, Priority.ALWAYS);
        Label t1 = new Label("Ta progression cette semaine");
        t1.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: white;");
        lblProgression = new Label("0 / 7 defis accomplis");
        lblProgression.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.75);");
        left.getChildren().addAll(t1, lblProgression);

        StackPane circle = new StackPane();
        Circle bg = new Circle(30);
        bg.setFill(Color.web("rgba(255,255,255,0.15)"));
        pctLabel = new Label("0%");
        pctLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white;");
        circle.getChildren().addAll(bg, pctLabel);

        row.getChildren().addAll(left, circle);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.setStyle("-fx-accent: #E8956D; -fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 4;");

        card.getChildren().addAll(row, progressBar);
        return card;
    }

    private HBox buildWeekRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Defis de la semaine");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox days = new HBox(5);
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        DayOfWeek[] week = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        for (int i = 0; i < 7; i++) {
            boolean isToday = week[i] == today;
            boolean done = defisDone[i];
            StackPane pill = new StackPane();
            Circle c = new Circle(14);
            c.setFill(Color.web(done ? "#52B788" : isToday ? "#4A6FA5" : "#EDF2F7"));
            String dayChar = week[i].getDisplayName(TextStyle.NARROW, Locale.FRENCH).substring(0, 1).toUpperCase();
            Label l = new Label(done ? "\u2713" : dayChar);
            l.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: " + ((done || isToday) ? "white" : "#A0AEC0") + ";");
            pill.getChildren().addAll(c, l);
            days.getChildren().add(pill);
        }

        row.getChildren().addAll(title, sp, days);
        return row;
    }

    private VBox buildDefiCard(int i) {
        boolean done = defisDone[i];
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(done ? styleDone() : styleNormal());

        HBox top = new HBox(14);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane ec = new StackPane();
        Circle bg = new Circle(20);
        bg.setFill(Color.web(done ? "#EAF7EF" : "#EEF4FF"));
        Label el = new Label(DEFI_EMOJI[i]);
        el.setStyle("-fx-font-size: 18px;");
        ec.getChildren().addAll(bg, el);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label((done ? "\u2705 " : "") + DEFI_NAME[i]);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (done ? "#276749" : "#2D3748") + ";");
        Label desc = new Label(DEFI_DESC[i]);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (done ? "#52B788" : "#718096") + ";");
        desc.setWrapText(true);
        info.getChildren().addAll(name, desc);

        Button btn = new Button(done ? "\u2713 Accompli" : "Marquer fait");
        btn.setStyle(done ? btnDone() : btnTodo());
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setOnAction(e -> toggleDefi(i, card, name, desc, btn));

        top.getChildren().addAll(ec, info, btn);
        card.getChildren().add(top);
        return card;
    }

    private void toggleDefi(int i, VBox card, Label name, Label desc, Button btn) {
        defisDone[i] = !defisDone[i];
        boolean done = defisDone[i];

        ScaleTransition st = new ScaleTransition(Duration.millis(120), card);
        st.setFromX(1.0); st.setToX(0.97);
        st.setFromY(1.0); st.setToY(0.97);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        card.setStyle(done ? styleDone() : styleNormal());
        name.setText((done ? "\u2705 " : "") + DEFI_NAME[i]);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (done ? "#276749" : "#2D3748") + ";");
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (done ? "#52B788" : "#718096") + ";");
        btn.setText(done ? "\u2713 Accompli" : "Marquer fait");
        btn.setStyle(done ? btnDone() : btnTodo());

        updateProgress();
    }

    private void updateProgress() {
        totalDone = 0;
        for (boolean b : defisDone) {
            if (b) totalDone++;
        }

        double pct = totalDone / 7.0;
        if (progressBar   != null) progressBar.setProgress(pct);
        if (pctLabel      != null) pctLabel.setText((int)(pct * 100) + "%");
        if (lblProgression != null) lblProgression.setText(totalDone + " / 7 defis accomplis");

        int newLevel;
        if (totalDone >= 7)      newLevel = 3;
        else if (totalDone >= 5) newLevel = 2;
        else if (totalDone >= 3) newLevel = 1;
        else                     newLevel = 0;

        if (lblStreak != null) lblStreak.setText("Streak : " + totalDone + (totalDone > 1 ? " jours" : " jour"));
        if (lblBadge  != null) lblBadge.setText("Badge : " + BADGE_EMOJI[newLevel] + " " + BADGE_NAME[newLevel]);

        if (newLevel > prevBadgeLevel) {
            prevBadgeLevel = newLevel;
            if (newLevel < badgeCards.size()) {
                VBox bc = badgeCards.get(newLevel);
                bc.setStyle(styleBadgeEarned(BADGE_COLOR[newLevel]));
                bc.setOpacity(1.0);
                animateBounce(bc);
            }
            if (totalDone == 7) launchConfetti();
        } else if (newLevel < prevBadgeLevel) {
            prevBadgeLevel = newLevel;
            for (int i = newLevel + 1; i < badgeCards.size(); i++) {
                badgeCards.get(i).setStyle(styleBadgeLocked());
                badgeCards.get(i).setOpacity(0.45);
            }
        }
    }

    private VBox buildBadgesSection() {
        VBox section = new VBox(12);
        Label title = new Label("Badges");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");
        section.getChildren().add(title);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 4; i++) {
            boolean earned = totalDone >= BADGE_THRESH[i];

            VBox card = new VBox(6);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(14, 18, 14, 18));
            card.setStyle(earned ? styleBadgeEarned(BADGE_COLOR[i]) : styleBadgeLocked());
            card.setOpacity(earned ? 1.0 : 0.45);

            // Slide-in staggered
            card.setTranslateY(40);
            card.setOpacity(0);
            Duration delay = Duration.millis(i * 120);

            TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
            tt.setFromY(40);
            tt.setToY(0);
            tt.setDelay(delay);
            tt.setInterpolator(Interpolator.EASE_OUT);

            FadeTransition ft = new FadeTransition(Duration.millis(350), card);
            ft.setFromValue(0);
            ft.setToValue(earned ? 1.0 : 0.45);
            ft.setDelay(delay);

            ParallelTransition pt = new ParallelTransition(tt, ft);
            pt.play();

            Label el = new Label(BADGE_EMOJI[i]);
            el.setStyle("-fx-font-size: 28px;");

            Label nl = new Label(BADGE_NAME[i]);
            nl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (earned ? "#2D3748" : "#A0AEC0") + ";");

            Label cl = new Label(BADGE_COND[i]);
            cl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (earned ? BADGE_COLOR[i] : "#A0AEC0") + ";");

            if (earned) {
                card.getChildren().addAll(el, nl, cl);
                animatePulse(el);
            } else {
                Label lock = new Label("[x]");
                lock.setStyle("-fx-font-size: 11px;");
                card.getChildren().addAll(el, nl, cl, lock);
            }

            badgeCards.add(card);
            row.getChildren().add(card);
        }

        section.getChildren().add(row);
        return section;
    }

    // =========================================================
    //  ANIMATIONS
    // =========================================================

    private void animatePulse(Label emoji) {
        ScaleTransition p = new ScaleTransition(Duration.millis(950), emoji);
        p.setFromX(1.0);
        p.setToX(1.18);
        p.setFromY(1.0);
        p.setToY(1.18);
        p.setAutoReverse(true);
        p.setCycleCount(Animation.INDEFINITE);
        p.setInterpolator(Interpolator.EASE_BOTH);
        p.play();
    }

    private void animateBounce(VBox card) {
        ScaleTransition up = new ScaleTransition(Duration.millis(320), card);
        up.setFromX(0.3);
        up.setToX(1.15);
        up.setFromY(0.3);
        up.setToY(1.15);
        up.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition down = new ScaleTransition(Duration.millis(180), card);
        down.setFromX(1.15);
        down.setToX(1.0);
        down.setFromY(1.15);
        down.setToY(1.0);
        down.setInterpolator(Interpolator.EASE_IN);

        new SequentialTransition(up, down).play();

        FadeTransition flash = new FadeTransition(Duration.millis(160), card);
        flash.setFromValue(0.3);
        flash.setToValue(1.0);
        flash.setCycleCount(2);
        flash.setAutoReverse(true);
        flash.play();
    }

    private void launchConfetti() {
        if (tabContent == null) return;
        double W = tabContent.getWidth() > 0 ? tabContent.getWidth() : 900;
        Canvas canvas = new Canvas(W, 600);
        canvas.setMouseTransparent(true);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        tabContent.getChildren().add(canvas);

        String[] cols = {"#E8956D","#4A6FA5","#52B788","#F5C87A","#7B9ED9","#FF6B6B"};
        Random rnd = new Random();
        int N = 80;
        double[] x  = new double[N];
        double[] y  = new double[N];
        double[] vx = new double[N];
        double[] vy = new double[N];
        double[] rot = new double[N];
        double[] sz  = new double[N];
        int[]    ci  = new int[N];

        for (int i = 0; i < N; i++) {
            x[i]   = rnd.nextDouble() * W;
            y[i]   = -10 - rnd.nextDouble() * 150;
            vx[i]  = (rnd.nextDouble() - 0.5) * 4;
            vy[i]  = 3 + rnd.nextDouble() * 5;
            rot[i] = rnd.nextDouble() * 360;
            ci[i]  = rnd.nextInt(cols.length);
            sz[i]  = 6 + rnd.nextDouble() * 8;
        }

        Timeline anim = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            gc.clearRect(0, 0, W, 600);
            for (int i = 0; i < N; i++) {
                x[i]   += vx[i];
                y[i]   += vy[i];
                rot[i] += 4;
                vy[i]  += 0.08;
                gc.setFill(Paint.valueOf(cols[ci[i]]));
                gc.save();
                gc.translate(x[i], y[i]);
                gc.rotate(rot[i]);
                gc.fillRoundRect(-sz[i] / 2, -sz[i] / 2, sz[i], sz[i] * 0.6, 3, 3);
                gc.restore();
            }
        }));
        anim.setCycleCount(140);
        anim.setOnFinished(e -> tabContent.getChildren().remove(canvas));
        anim.play();
    }

    // =========================================================
    //  TAB SWITCH
    // =========================================================

    private void setActiveTab(boolean musique) {
        if (indicMusique != null)
            indicMusique.setStyle("-fx-background-color: " + (musique ? "#4A6FA5" : "transparent") + "; -fx-background-radius: 2;");
        if (indicDefis != null)
            indicDefis.setStyle("-fx-background-color: " + (!musique ? "#4A6FA5" : "transparent") + "; -fx-background-radius: 2;");
        if (lblTabMusique != null)
            lblTabMusique.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + (musique ? "#3D5A8A" : "#A0AEC0") + ";");
        if (lblTabDefis != null)
            lblTabDefis.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + (!musique ? "#3D5A8A" : "#A0AEC0") + ";");
    }

    // =========================================================
    //  STYLE HELPERS  (pure ASCII, no unicode in style strings)
    // =========================================================

    private String btnMoodNormal() {
        return "-fx-background-color: white; -fx-text-fill: #4A5568; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10 18; -fx-background-radius: 25; -fx-cursor: hand; -fx-border-color: #E2E8F0; -fx-border-radius: 25; -fx-border-width: 1.5;";
    }
    private String btnMoodHover() {
        return "-fx-background-color: #EEF4FF; -fx-text-fill: #3D5A8A; -fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 10 18; -fx-background-radius: 25; -fx-cursor: hand; -fx-border-color: #4A6FA5; -fx-border-radius: 25; -fx-border-width: 1.5;";
    }
    private String btnTrackNormal() {
        return "-fx-background-color: #EEF4FF; -fx-text-fill: #4A6FA5; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 12; -fx-background-radius: 20; -fx-cursor: hand;";
    }
    private String btnTrackActive() {
        return "-fx-background-color: #4A6FA5; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 5 12; -fx-background-radius: 20; -fx-cursor: hand;";
    }
    private String styleNormal() {
        return "-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 2);";
    }
    private String styleDone() {
        return "-fx-background-color: #EAF7EF; -fx-background-radius: 14; -fx-border-color: #B8E8CE; -fx-border-radius: 14; -fx-border-width: 1;";
    }
    private String btnTodo() {
        return "-fx-background-color: #EEF4FF; -fx-text-fill: #4A6FA5; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 7 16; -fx-background-radius: 20; -fx-cursor: hand;";
    }
    private String btnDone() {
        return "-fx-background-color: #52B788; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 7 16; -fx-background-radius: 20; -fx-cursor: hand;";
    }
    private String styleBadgeEarned(String color) {
        return "-fx-background-color: white; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, " + color + ", 14, 0.3, 0, 0); -fx-border-color: " + color + "44; -fx-border-radius: 14; -fx-border-width: 1.5;";
    }
    private String styleBadgeLocked() {
        return "-fx-background-color: #F7FAFC; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 6, 0, 0, 1);";
    }
}