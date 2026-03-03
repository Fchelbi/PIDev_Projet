package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.util.ArrayList;
import java.util.List;

public class Wellnesscontroller {

    @FXML private VBox rootPane;
    private User currentUser;
    private WebEngine webEngine;
    private List<Button> moodButtons = new ArrayList<>();

    public void setUser(User user) { this.currentUser = user; }

    @FXML
    void initialize() { buildUI(); }

    private void buildUI() {
        rootPane.getChildren().clear();
        rootPane.setStyle("-fx-background-color: #F8F4EF;");

        VBox header = new VBox(4);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #3D5A8A, #4A6FA5); -fx-padding: 22 32 16 32;");
        Label title = new Label("Espace Bien-etre - Musique");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: white;");
        Label sub = new Label("Musique therapeutique pour chaque humeur");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7);");
        header.getChildren().add(title);
        header.getChildren().add(sub);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #F8F4EF; -fx-background-color: #F8F4EF;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox page = new VBox(20);
        page.setPadding(new Insets(26, 28, 40, 28));
        page.setStyle("-fx-background-color: #F8F4EF;");

        Label q = new Label("Comment te sens-tu en ce moment ?");
        q.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        HBox row1 = new HBox(12);
        HBox row2 = new HBox(12);

        WebView webView = new WebView();
        webView.setPrefHeight(310);
        webEngine = webView.getEngine();

        VBox playerBox = new VBox(12);
        playerBox.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");

        Label playerTitle = new Label("Selectionne une humeur ci-dessus");
        playerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2D3748;");

        HBox trackBar = new HBox(8);
        trackBar.setAlignment(Pos.CENTER_LEFT);

        Label placeholder = new Label("Choisis une humeur pour lancer la musique");
        placeholder.setStyle("-fx-font-size: 12px; -fx-text-fill: #A0AEC0; -fx-padding: 40 0;");
        placeholder.setMaxWidth(Double.MAX_VALUE);
        placeholder.setAlignment(Pos.CENTER);

        playerBox.getChildren().add(playerTitle);
        playerBox.getChildren().add(placeholder);
        playerBox.getChildren().add(trackBar);

        String[] p0 = {"playlist/37i9dQZF1DX3Ogo9pFvBkY","playlist/37i9dQZF1DWZqd5JICZI0u","playlist/37i9dQZF1DX4E3UdUs7fUx"};
        String[] p1 = {"playlist/37i9dQZF1DX76Wlfdnj7AP","playlist/37i9dQZF1DX0XUsuxWHRQd","playlist/37i9dQZF1DXdxcBWuJkbcy"};
        String[] p2 = {"playlist/37i9dQZF1DX7qK8ma5wgG1","playlist/37i9dQZF1DWVrtsSlLKzro","playlist/37i9dQZF1DX3YSRoSdA634"};
        String[] p3 = {"playlist/37i9dQZF1DX9uKNf5jGX6m","playlist/37i9dQZF1DWTC29eJKOqGJ","playlist/37i9dQZF1DX4PP3DA4J0N8"};
        String[] p4 = {"playlist/37i9dQZF1DX8NTLI2TtZa6","playlist/37i9dQZF1DX4sWSpwq3LiO","playlist/37i9dQZF1DWZeNpW2ELTJ7"};
        String[] p5 = {"playlist/37i9dQZF1DWZd79rJ6a7lp","playlist/37i9dQZF1DX4sWSpwq3LiO","playlist/37i9dQZF1DX9RwzeB9EnUK"};

        row1.getChildren().add(makeBtn("Calme et Serenite",      "#7B9ED9", p0, playerBox, playerTitle, placeholder, webView, trackBar));
        row1.getChildren().add(makeBtn("Energie et Motivation",  "#E8956D", p1, playerBox, playerTitle, placeholder, webView, trackBar));
        row1.getChildren().add(makeBtn("Tristesse et Reconfort", "#4A6FA5", p2, playerBox, playerTitle, placeholder, webView, trackBar));
        row2.getChildren().add(makeBtn("Stress et Anxiete",      "#52B788", p3, playerBox, playerTitle, placeholder, webView, trackBar));
        row2.getChildren().add(makeBtn("Focus et Concentration", "#F5C87A", p4, playerBox, playerTitle, placeholder, webView, trackBar));
        row2.getChildren().add(makeBtn("Sommeil et Detente",     "#9B8EC4", p5, playerBox, playerTitle, placeholder, webView, trackBar));

        page.getChildren().add(q);
        page.getChildren().add(row1);
        page.getChildren().add(row2);
        page.getChildren().add(playerBox);
        page.getChildren().add(buildTips());

        scroll.setContent(page);
        rootPane.getChildren().add(header);
        rootPane.getChildren().add(scroll);
    }

    private Button makeBtn(String label, String color, String[] urls,
                           VBox playerBox, Label playerTitle, Label placeholder,
                           WebView webView, HBox trackBar) {
        Button btn = new Button(label);
        btn.setStyle(moodNormal());
        btn.setCursor(javafx.scene.Cursor.HAND);
        moodButtons.add(btn);
        btn.setOnMouseEntered(e -> { if (!btn.getStyle().contains(color)) btn.setStyle(moodHover()); });
        btn.setOnMouseExited(e ->  { if (!btn.getStyle().contains(color)) btn.setStyle(moodNormal()); });
        btn.setOnAction(e -> {
            for (int i = 0; i < moodButtons.size(); i++) moodButtons.get(i).setStyle(moodNormal());
            btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:700;-fx-padding:10 16;-fx-background-radius:25;-fx-cursor:hand;");
            playerTitle.setText(label);
            playerBox.getChildren().remove(placeholder);
            if (!playerBox.getChildren().contains(webView)) playerBox.getChildren().add(1, webView);
            loadTrack(urls[0], trackBar, urls);
        });
        return btn;
    }

    private void loadTrack(String path, HBox trackBar, String[] urls) {
        webEngine.loadContent(html(path));
        trackBar.getChildren().clear();
        Label l = new Label("Piste :");
        l.setStyle("-fx-font-size:11px;-fx-text-fill:#A0AEC0;");
        trackBar.getChildren().add(l);
        for (int i = 0; i < urls.length; i++) {
            final String u = urls[i];
            final int n = i + 1;
            Button tb = new Button(String.valueOf(n));
            tb.setStyle(trackNormal());
            tb.setCursor(javafx.scene.Cursor.HAND);
            tb.setOnAction(e -> {
                for (int j = 0; j < trackBar.getChildren().size(); j++) {
                    Node nd = trackBar.getChildren().get(j);
                    if (nd instanceof Button) ((Button) nd).setStyle(trackNormal());
                }
                tb.setStyle(trackActive());
                webEngine.loadContent(html(u));
            });
            trackBar.getChildren().add(tb);
        }
        if (trackBar.getChildren().size() > 1) {
            Node f = trackBar.getChildren().get(1);
            if (f instanceof Button) ((Button) f).setStyle(trackActive());
        }
    }

    private String html(String path) {
        String url = "https://open.spotify.com/embed/" + path + "?utm_source=generator&theme=0";
        return "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;}body{background:#121212;}iframe{width:100%;height:300px;border:none;border-radius:12px;}</style></head><body>"
                + "<iframe src='" + url + "' allow='autoplay;clipboard-write;encrypted-media;fullscreen;picture-in-picture' loading='lazy'></iframe></body></html>";
    }

    private VBox buildTips() {
        VBox box = new VBox(10);
        box.setStyle("-fx-background-color:#EEF4FF;-fx-background-radius:14;-fx-padding:18 20;-fx-border-color:#D5E3F5;-fx-border-radius:14;-fx-border-width:1;");
        Label t = new Label("Le saviez-vous ?");
        t.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:#3D5A8A;");
        box.getChildren().add(t);
        String s = "-fx-font-size:11.5px;-fx-text-fill:#4A5568;";
        Label l0 = new Label("- La musicotherapie reduit l'anxiete de 65%.");
        Label l1 = new Label("- Ecouter 30 min de musique apaisante diminue le cortisol.");
        Label l2 = new Label("- Les frequences Binaural Beats 1-4 Hz favorisent un sommeil profond.");
        Label l3 = new Label("- La musique rythmee a 120-140 BPM ameliore les performances.");
        l0.setStyle(s); l0.setWrapText(true);
        l1.setStyle(s); l1.setWrapText(true);
        l2.setStyle(s); l2.setWrapText(true);
        l3.setStyle(s); l3.setWrapText(true);
        box.getChildren().add(l0);
        box.getChildren().add(l1);
        box.getChildren().add(l2);
        box.getChildren().add(l3);
        return box;
    }

    private String moodNormal() { return "-fx-background-color:white;-fx-text-fill:#4A5568;-fx-font-size:12px;-fx-font-weight:600;-fx-padding:10 16;-fx-background-radius:25;-fx-cursor:hand;-fx-border-color:#E2E8F0;-fx-border-radius:25;-fx-border-width:1.5;"; }
    private String moodHover()  { return "-fx-background-color:#EEF4FF;-fx-text-fill:#3D5A8A;-fx-font-size:12px;-fx-font-weight:700;-fx-padding:10 16;-fx-background-radius:25;-fx-cursor:hand;-fx-border-color:#4A6FA5;-fx-border-radius:25;-fx-border-width:1.5;"; }
    private String trackNormal(){ return "-fx-background-color:#EEF4FF;-fx-text-fill:#4A6FA5;-fx-font-size:11px;-fx-font-weight:600;-fx-padding:5 12;-fx-background-radius:20;-fx-cursor:hand;"; }
    private String trackActive(){ return "-fx-background-color:#4A6FA5;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:600;-fx-padding:5 12;-fx-background-radius:20;-fx-cursor:hand;"; }
}