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
import entities.User;
import entities.Call;
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

public class PatientHome {

    // ── Labels ────────────────────────────────────────────────────────────
    @FXML private Label lblWelcome, lblAvatarHeader;
    @FXML private Label lblNbRdv, lblNbSeances;
    @FXML private Label lblNom, lblEmail, lblTel;
    @FXML private Label lblAffirmation, lblAffirmStatus;
    @FXML private Label lblAdvice, lblAdviceStatus;
    @FXML private ImageView imgHeaderPhoto;

    // ── Layout ────────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;
    @FXML private ScrollPane accueilPane;

    // ── Messages (your friend's module) ───────────────────────────────────
    @FXML private VBox navMessages;
    @FXML private HBox indicMessages;
    @FXML private Label lblMessagesBadge;

    // ── Formation nav (your module) ───────────────────────────────────────
    @FXML private VBox navAccueil;
    @FXML private VBox navFormations;
    @FXML private VBox navMesFormations;
    @FXML private VBox navResultats;
    @FXML private VBox navProfil;

    @FXML private HBox indicAccueil;
    @FXML private HBox indicFormations;
    @FXML private HBox indicMesFormations;
    @FXML private HBox indicResultats;
    @FXML private HBox indicProfil;

    // ── Services & State ──────────────────────────────────────────────────
    private User currentUser;
    private final serviceUser us = new serviceUser();
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
        System.out.println("PatientHome initialized");
    }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        loadAffirmation();
        loadAdvice();
        startNotifications();
        setActiveNav(navAccueil, indicAccueil);
    }

    // ════════════════════════════════════════════════════════════════════
    //  USER DATA
    // ════════════════════════════════════════════════════════════════════

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) {
            System.err.println("Refresh: " + e.getMessage());
        }
        lblWelcome.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
        lblAvatarHeader.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        if (lblNom != null) lblNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (lblEmail != null) lblEmail.setText(currentUser.getEmail());
        if (lblTel != null)
            lblTel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "—");
        if (lblNbRdv != null) lblNbRdv.setText("0");
        if (lblNbSeances != null) lblNbSeances.setText("0");
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
    //  AFFIRMATION API
    // ════════════════════════════════════════════════════════════════════

    private void loadAffirmation() {
        if (lblAffirmation == null) return;
        Platform.runLater(() -> {
            if (lblAffirmStatus != null) lblAffirmStatus.setText("⏳ Chargement...");
            lblAffirmation.setText("");
        });
        Thread t = new Thread(() -> {
            try {
                HttpClient c = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest r = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.affirmations.dev/"))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "EchoCare/1.0").GET().build();
                HttpResponse<String> resp = c.send(r, HttpResponse.BodyHandlers.ofString());
                String text = extractValue(resp.body(), "affirmation");
                Platform.runLater(() -> {
                    if (lblAffirmStatus != null) lblAffirmStatus.setText("✨ Affirmation du jour");
                    lblAffirmation.setText("❝  " + text + "  ❞");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblAffirmStatus != null) lblAffirmStatus.setText("✨ Affirmation");
                    lblAffirmation.setText(
                            "❝  Vous avez la force de surmonter tous les défis d'aujourd'hui.  ❞");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════════════════════
    //  ADVICE API
    // ════════════════════════════════════════════════════════════════════

    private void loadAdvice() {
        if (lblAdvice == null) return;
        Platform.runLater(() -> {
            if (lblAdviceStatus != null) lblAdviceStatus.setText("⏳ Chargement...");
            lblAdvice.setText("");
        });
        Thread t = new Thread(() -> {
            try {
                HttpClient c = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest r = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.adviceslip.com/advice"))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "EchoCare/1.0").GET().build();
                HttpResponse<String> resp = c.send(r, HttpResponse.BodyHandlers.ofString());
                String text = extractValue(resp.body(), "advice");
                Platform.runLater(() -> {
                    if (lblAdviceStatus != null) lblAdviceStatus.setText("🌿 Conseil bien-être");
                    lblAdvice.setText("💡  " + text);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblAdviceStatus != null) lblAdviceStatus.setText("🌿 Conseil");
                    lblAdvice.setText("💡  Prenez soin de votre santé mentale chaque jour.");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════════════════════
    //  NOTIFICATIONS (your friend's messaging module)
    // ════════════════════════════════════════════════════════════════════

    private void startNotifications() {
        try {
            services.Notificationservice ns = services.Notificationservice.INSTANCE;
            ns.start(currentUser);

            // Badge for unread messages
            ns.setOnUnreadCountChanged(count -> {
                if (lblMessagesBadge == null) return;
                if (count > 0) {
                    lblMessagesBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                    lblMessagesBadge.setVisible(true);
                    lblMessagesBadge.setManaged(true);
                } else {
                    lblMessagesBadge.setVisible(false);
                    lblMessagesBadge.setManaged(false);
                }
            });

            // New message notification
            ns.setOnNewMessage(this::openMessagerie);

            // Incoming call
            ns.setOnIncomingCall(call -> {
                try {
                    entities.User caller = us.getUserById(call.getId_caller());
                    if (caller == null) return;
                    utils.Notificationhelper.show("📞",
                            caller.getPrenom() + " " + caller.getNom() + " · " + caller.getRole(),
                            "Appel entrant — Appuyez pour répondre",
                            () -> openCallScreen(caller, call));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            // Notification service might not exist yet — that's OK
            System.err.println("Notifications not available: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  MESSAGING (your friend's module)
    // ════════════════════════════════════════════════════════════════════

    private void openMessagerie() {
        try {
            setActiveNav(navMessages, indicMessages);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Messagerie.fxml"));
            HBox page = loader.load();
            Messageriecontroller ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openCallScreen(entities.User caller, entities.Call call) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Messagerie.fxml"));
            HBox page = loader.load();
            Messageriecontroller ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setSelectedContact(caller);
            contentArea.getChildren().setAll(page);
            ctrl.openCallScreen(true, call, call.getId_call());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  JSON HELPER
    // ════════════════════════════════════════════════════════════════════

    private String extractValue(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return "";
        i += s.length();
        int end = json.indexOf("\"", i);
        return end < 0 ? "" : json.substring(i, end)
                .replace("\\n", " ").replace("\\'", "'");
    }

    @FXML
    void refreshAffirmation(MouseEvent e) {
        loadAffirmation();
    }

    @FXML
    void refreshAdvice(MouseEvent e) {
        loadAdvice();
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION — click handlers
    // ════════════════════════════════════════════════════════════════════

    private void showAccueilFromProfil() {
        setActiveNav(navAccueil, indicAccueil);
        contentArea.getChildren().setAll(accueilPane);
    }

    @FXML
    void showAccueil(MouseEvent event) {
        setActiveNav(navAccueil, indicAccueil);
        contentArea.getChildren().setAll(accueilPane);
    }

    @FXML
    void showAllFormations(MouseEvent event) {
        setActiveNav(navFormations, indicFormations);
        loadSubPage("PatientFormations.fxml", "Formations");
    }

    @FXML
    void showMyFormations(MouseEvent event) {
        setActiveNav(navMesFormations, indicMesFormations);
        loadSubPage("PatientMesFormations.fxml", "Mes Formations");
    }

    @FXML
    void showMyResults(MouseEvent event) {
        setActiveNav(navResultats, indicResultats);
        loadSubPage("PatientResultats.fxml", "Mes Résultats");
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
            ctrl.setOnBackToAccueil(this::showAccueilFromProfil);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le profil.");
        }
    }

    @FXML
    void showMessages(MouseEvent event) {
        openMessagerie();
    }

    // ════════════════════════════════════════════════════════════════════
    //  SUB-PAGE LOADER
    // ════════════════════════════════════════════════════════════════════

    private void loadSubPage(String fxmlName, String title) {
        try {
            java.net.URL res = getClass().getResource("/" + fxmlName);
            if (res != null) {
                FXMLLoader loader = new FXMLLoader(res);
                Parent page = loader.load();
                Object ctrl = loader.getController();
                if (ctrl != null) {
                    try {
                        ctrl.getClass().getMethod("setUser", User.class)
                                .invoke(ctrl, currentUser);
                    } catch (Exception ignored) {
                    }
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
        VBox outer = new VBox(0);
        HBox header = new HBox();
        header.setStyle("-fx-padding:22 30;-fx-background-color:white;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label h = new Label(title);
        h.setStyle("-fx-font-size:22px;-fx-font-weight:700;-fx-text-fill:#2D3748;");
        header.getChildren().add(h);
        VBox content = new VBox(16);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-padding:80;");
        Label icon = new Label("🚧");
        icon.setStyle("-fx-font-size:50px;");
        Label msg = new Label("Page en cours de développement");
        msg.setStyle("-fx-font-size:17px;-fx-font-weight:600;-fx-text-fill:#4A5568;");
        Label sub = new Label("Bientôt disponible.");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#A0AEC0;");
        content.getChildren().addAll(icon, msg, sub);
        outer.getChildren().addAll(header, content);
        ScrollPane sp = new ScrollPane(outer);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:#F2F0EC;");
        return sp;
    }

    // ════════════════════════════════════════════════════════════════════
    //  HOVER HANDLERS
    // ════════════════════════════════════════════════════════════════════

    @FXML void onNavAccueilEnter(MouseEvent e) {
        if (navAccueil != currentActiveNav) navAccueil.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavAccueilExit(MouseEvent e) {
        if (navAccueil != currentActiveNav) navAccueil.setStyle(NAV_NORMAL);
    }

    @FXML void onNavFormationsEnter(MouseEvent e) {
        if (navFormations != currentActiveNav) navFormations.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavFormationsExit(MouseEvent e) {
        if (navFormations != currentActiveNav) navFormations.setStyle(NAV_NORMAL);
    }

    @FXML void onNavMesFormationsEnter(MouseEvent e) {
        if (navMesFormations != currentActiveNav) navMesFormations.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavMesFormationsExit(MouseEvent e) {
        if (navMesFormations != currentActiveNav) navMesFormations.setStyle(NAV_NORMAL);
    }

    @FXML void onNavResultatsEnter(MouseEvent e) {
        if (navResultats != currentActiveNav) navResultats.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavResultatsExit(MouseEvent e) {
        if (navResultats != currentActiveNav) navResultats.setStyle(NAV_NORMAL);
    }

    @FXML void onNavProfilEnter(MouseEvent e) {
        if (navProfil != currentActiveNav) navProfil.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavProfilExit(MouseEvent e) {
        if (navProfil != currentActiveNav) navProfil.setStyle(NAV_NORMAL);
    }

    @FXML void onNavMessagesEnter(MouseEvent e) {
        if (navMessages != currentActiveNav) navMessages.setStyle(NAV_ACTIVE);
    }
    @FXML void onNavMessagesExit(MouseEvent e) {
        if (navMessages != currentActiveNav) navMessages.setStyle(NAV_NORMAL);
    }

    // ── Card hover ──────────────────────────────────────────────────────
    @FXML void onCardFormationsEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardFormationsExit(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardMesFormationsEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardMesFormationsExit(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardResultatsEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardResultatsExit(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardProfilEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardProfilExit(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }

    // ════════════════════════════════════════════════════════════════════
    //  ACTIVE NAV HELPER
    // ════════════════════════════════════════════════════════════════════

    private void setActiveNav(VBox nav, HBox indic) {
        VBox[] navs = {navAccueil, navFormations, navMesFormations,
                navResultats, navProfil, navMessages};
        HBox[] indics = {indicAccueil, indicFormations, indicMesFormations,
                indicResultats, indicProfil, indicMessages};
        for (VBox n : navs) if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i : indics) if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    // ════════════════════════════════════════════════════════════════════
    //  LOGOUT
    // ════════════════════════════════════════════════════════════════════

    @FXML
    void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion",
                "Voulez-vous vraiment quitter ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}