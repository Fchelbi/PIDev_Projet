package utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.io.File;

public class VideoPlayerUtil {

    public static boolean isYouTubeUrl(String url) {
        if (url == null) return false;
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    public static String extractYouTubeId(String url) {
        if (url == null) return "";
        String id = "";
        try {
            if (url.contains("v=")) {
                id = url.split("v=")[1];
            } else if (url.contains("youtu.be/")) {
                id = url.split("youtu.be/")[1];
            } else if (url.contains("embed/")) {
                id = url.split("embed/")[1];
            }
            if (id.contains("&")) id = id.split("&")[0];
            if (id.contains("?")) id = id.split("\\?")[0];
        } catch (Exception e) {
            return "";
        }
        return id.trim();
    }

    /**
     * YouTube Player — uses embed iframe HTML loaded directly into WebView.
     * This is the ONLY reliable way to play YouTube inside a JavaFX WebView.
     * The watch page blocks embedding; the embed URL works perfectly.
     */
    public static VBox createYouTubePlayer(String url) {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: #000000;");
        container.setAlignment(Pos.CENTER);

        String videoId = extractYouTubeId(url);
        if (videoId.isEmpty()) {
            return createErrorMessage("URL YouTube invalide", url);
        }

        String embedUrl = "https://www.youtube.com/embed/" + videoId
                + "?autoplay=0&rel=0&modestbranding=1";
        String watchUrl = "https://www.youtube.com/watch?v=" + videoId;

        try {
            WebView webView = new WebView();
            webView.setPrefHeight(500);
            webView.setMinHeight(420);
            webView.setContextMenuEnabled(false);

            WebEngine engine = webView.getEngine();
            engine.setJavaScriptEnabled(true);

            // Build a simple HTML page that embeds the YouTube iframe
            String html = "<!DOCTYPE html>"
                    + "<html>"
                    + "<head>"
                    + "<meta charset='UTF-8'/>"
                    + "<style>"
                    + "  * { margin: 0; padding: 0; box-sizing: border-box; }"
                    + "  body { background: #000; width: 100%; height: 100vh;"
                    + "         display: flex; flex-direction: column;"
                    + "         align-items: center; justify-content: center; }"
                    + "  .video-wrapper { position: relative; width: 100%; padding-bottom: 56.25%; height: 0; }"
                    + "  .video-wrapper iframe {"
                    + "    position: absolute; top: 0; left: 0;"
                    + "    width: 100%; height: 100%; border: 0;"
                    + "  }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "  <div class='video-wrapper'>"
                    + "    <iframe src='" + embedUrl + "'"
                    + "      allow='accelerometer; autoplay; clipboard-write;"
                    + "             encrypted-media; gyroscope; picture-in-picture'"
                    + "      allowfullscreen>"
                    + "    </iframe>"
                    + "  </div>"
                    + "</body>"
                    + "</html>";

            engine.loadContent(html, "text/html");

            engine.setOnError(event ->
                    System.err.println("WebView error: " + event.getMessage()));

            VBox.setVgrow(webView, Priority.ALWAYS);

            // Controls bar
            HBox controlsBar = new HBox(12);
            controlsBar.setAlignment(Pos.CENTER);
            controlsBar.setPadding(new Insets(8, 15, 8, 15));
            controlsBar.setStyle("-fx-background-color: #2d3436;");

            Button btnBrowser = new Button("🌐 Ouvrir dans le navigateur");
            btnBrowser.setStyle(
                    "-fx-background-color: #d63031; -fx-text-fill: white; "
                            + "-fx-font-weight: bold; -fx-cursor: hand; "
                            + "-fx-background-radius: 5; -fx-padding: 8 15;"
            );
            btnBrowser.setOnAction(e -> openInBrowser(watchUrl));

            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblInfo = new Label("🎬 YouTube — " + videoId);
            lblInfo.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 11px;");

            controlsBar.getChildren().addAll(btnBrowser, spacer, lblInfo);

            container.getChildren().addAll(webView, controlsBar);
            container.setUserData(webView);

        } catch (Exception e) {
            System.err.println("YouTube player error: " + e.getMessage());
            return createYouTubeFallback(videoId, url);
        }

        return container;
    }

    private static VBox createYouTubeFallback(String videoId, String originalUrl) {
        VBox fallback = new VBox(15);
        fallback.setAlignment(Pos.CENTER);
        fallback.setPrefHeight(450);
        fallback.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 10;");
        fallback.setPadding(new Insets(30));

        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size: 60px;");

        Label title = new Label("Vidéo YouTube");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label idLabel = new Label("ID: " + videoId);
        idLabel.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 13px;");

        Label explanation = new Label(
                "Cliquez ci-dessous pour ouvrir la vidéo dans votre navigateur."
        );
        explanation.setStyle(
                "-fx-text-fill: #dfe6e9; -fx-font-size: 13px; -fx-text-alignment: center;"
        );
        explanation.setWrapText(true);
        explanation.setMaxWidth(400);

        Button btnOpen = new Button("▶️ Ouvrir dans le Navigateur");
        btnOpen.setStyle(
                "-fx-background-color: #d63031; -fx-text-fill: white; "
                        + "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 12 30; "
                        + "-fx-background-radius: 10; -fx-cursor: hand;"
        );
        btnOpen.setOnAction(e -> openInBrowser(
                "https://www.youtube.com/watch?v=" + videoId));

        fallback.getChildren().addAll(icon, title, idLabel, explanation, btnOpen);
        return fallback;
    }

    private static void openInBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"open", url});
                } else {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
            } catch (Exception ex) {
                System.err.println("Cannot open browser: " + ex.getMessage());
            }
        }
    }

    /**
     * Local MP4 player using JavaFX MediaPlayer
     */
    public static VBox createLocalPlayer(String filePath) {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: black;");

        File file = new File(filePath);
        if (!file.exists()) return createErrorMessage("Fichier non trouvé", filePath);

        try {
            Media media = new Media(file.toURI().toString());
            media.setOnError(() -> javafx.application.Platform.runLater(() -> {
                container.getChildren().clear();
                container.getChildren().add(createWebViewLocalPlayer(filePath));
            }));

            MediaPlayer mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);

            StackPane videoPane = new StackPane(mediaView);
            videoPane.setStyle("-fx-background-color: black;");
            videoPane.setMinHeight(400);
            videoPane.setPrefHeight(400);
            VBox.setVgrow(videoPane, Priority.ALWAYS);

            Label lblLoading = new Label("⏳ Chargement...");
            lblLoading.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
            videoPane.getChildren().add(lblLoading);

            videoPane.widthProperty().addListener((o, ov, nv) -> mediaView.setFitWidth(nv.doubleValue()));
            videoPane.heightProperty().addListener((o, ov, nv) -> mediaView.setFitHeight(nv.doubleValue()));

            Button btnPlay = new Button("▶ Lecture");
            btnPlay.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;");
            btnPlay.setDisable(true);

            Button btnStop = new Button("⏹ Stop");
            btnStop.setStyle("-fx-background-color: #d63031; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;");
            btnStop.setDisable(true);

            Slider timeSlider = new Slider(0, 100, 0);
            HBox.setHgrow(timeSlider, Priority.ALWAYS);
            timeSlider.setDisable(true);

            Label lblTime = new Label("00:00 / 00:00");
            lblTime.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            lblTime.setMinWidth(110);

            Label lblStatus = new Label("⏳ Chargement...");
            lblStatus.setStyle("-fx-text-fill: #fdcb6e; -fx-font-size: 12px;");

            Slider volumeSlider = new Slider(0, 1, 0.7);
            volumeSlider.setPrefWidth(80);

            mediaPlayer.setOnReady(() -> {
                videoPane.getChildren().remove(lblLoading);
                btnPlay.setDisable(false);
                btnStop.setDisable(false);
                timeSlider.setDisable(false);
                timeSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
                lblTime.setText("00:00 / " + formatTime(mediaPlayer.getTotalDuration()));
                lblStatus.setText("✅ Prêt");
                lblStatus.setStyle("-fx-text-fill: #00b894; -fx-font-size: 12px;");
            });

            btnPlay.setOnAction(e -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    btnPlay.setText("▶ Lecture");
                    btnPlay.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;");
                } else {
                    mediaPlayer.play();
                    btnPlay.setText("⏸ Pause");
                    btnPlay.setStyle("-fx-background-color: #fdcb6e; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;");
                }
            });

            btnStop.setOnAction(e -> {
                mediaPlayer.stop();
                btnPlay.setText("▶ Lecture");
                btnPlay.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;");
            });

            mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty());

            mediaPlayer.currentTimeProperty().addListener((obs, ov, nv) -> {
                if (!timeSlider.isValueChanging() && !timeSlider.isPressed()) {
                    Duration totalDur = mediaPlayer.getTotalDuration();
                    if (totalDur != null && !totalDur.isUnknown()) {
                        timeSlider.setValue(nv.toSeconds());
                        lblTime.setText(formatTime(nv) + " / " + formatTime(totalDur));
                    }
                }
            });

            timeSlider.setOnMousePressed(e -> mediaPlayer.pause());
            timeSlider.setOnMouseReleased(e -> {
                mediaPlayer.seek(Duration.seconds(timeSlider.getValue()));
                mediaPlayer.play();
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.stop();
                btnPlay.setText("▶ Lecture");
                btnPlay.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 8; -fx-padding: 8 20;");
            });

            mediaPlayer.setOnError(() -> javafx.application.Platform.runLater(() -> {
                container.getChildren().clear();
                container.getChildren().add(createWebViewLocalPlayer(filePath));
                container.setUserData(null);
            }));

            HBox row1 = new HBox(10, btnPlay, btnStop, lblStatus);
            row1.setAlignment(Pos.CENTER);
            row1.setPadding(new Insets(8, 15, 4, 15));
            row1.setStyle("-fx-background-color: #2d3436;");

            Label lblVol = new Label("🔊");
            lblVol.setStyle("-fx-text-fill: white;");
            HBox row2 = new HBox(10, timeSlider, lblTime, lblVol, volumeSlider);
            row2.setAlignment(Pos.CENTER);
            row2.setPadding(new Insets(4, 15, 8, 15));
            row2.setStyle("-fx-background-color: #2d3436;");

            container.getChildren().addAll(videoPane, row1, row2);
            container.setUserData(mediaPlayer);

        } catch (Exception e) {
            container.getChildren().clear();
            container.getChildren().add(createWebViewLocalPlayer(filePath));
        }
        return container;
    }

    public static VBox createWebViewLocalPlayer(String filePath) {
        VBox container = new VBox();
        container.setStyle("-fx-background-color: black;");
        File file = new File(filePath);
        if (!file.exists()) return createErrorMessage("Fichier non trouvé", filePath);

        WebView webView = new WebView();
        webView.setPrefHeight(450);
        webView.getEngine().setJavaScriptEnabled(true);

        String html = "<!DOCTYPE html><html><head>"
                + "<style>*{margin:0;padding:0;}body{background:#000;display:flex;"
                + "justify-content:center;align-items:center;height:100vh;}"
                + "video{width:100%;height:100%;object-fit:contain;}</style></head><body>"
                + "<video controls><source src='" + file.toURI().toString() + "'/>"
                + "</video></body></html>";

        webView.getEngine().loadContent(html);
        VBox.setVgrow(webView, Priority.ALWAYS);
        container.getChildren().add(webView);
        container.setUserData(webView);
        return container;
    }

    public static VBox createNoVideoMessage() {
        VBox c = new VBox(10);
        c.setAlignment(Pos.CENTER);
        c.setPrefHeight(400);
        c.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 10;");
        Label i = new Label("🎬");
        i.setStyle("-fx-font-size: 60px;");
        Label m = new Label("Aucune vidéo");
        m.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label s = new Label("Ajoutez une URL YouTube ou un fichier MP4");
        s.setStyle("-fx-text-fill: #b2bec3;");
        c.getChildren().addAll(i, m, s);
        return c;
    }

    public static VBox createErrorMessage(String title, String detail) {
        VBox c = new VBox(10);
        c.setAlignment(Pos.CENTER);
        c.setPrefHeight(400);
        c.setStyle("-fx-background-color: #2d3436; -fx-background-radius: 10;");
        Label i = new Label("❌");
        i.setStyle("-fx-font-size: 50px;");
        Label m = new Label(title);
        m.setStyle("-fx-text-fill: #d63031; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label s = new Label(detail);
        s.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 12px;");
        s.setWrapText(true);
        c.getChildren().addAll(i, m, s);
        return c;
    }

    public static void stopMedia(VBox container) {
        if (container == null) return;
        Object data = container.getUserData();
        if (data instanceof MediaPlayer mp) {
            try { mp.stop(); mp.dispose(); } catch (Exception ignored) {}
        } else if (data instanceof WebView wv) {
            try { wv.getEngine().loadContent(""); } catch (Exception ignored) {}
        }
        container.getChildren().forEach(node -> {
            if (node instanceof VBox vbox) stopMedia(vbox);
        });
    }

    private static String formatTime(Duration d) {
        if (d == null || d.isUnknown()) return "00:00";
        int t = (int) d.toSeconds();
        return String.format("%02d:%02d", t / 60, t % 60);
    }
}
