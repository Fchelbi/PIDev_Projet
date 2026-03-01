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

    @FXML private Label lblWelcome, lblAvatarHeader;
    @FXML private Label lblNbRdv, lblNbSeances;
    @FXML private Label lblNom, lblEmail, lblTel;
    @FXML private Label lblAffirmation, lblAffirmStatus;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane accueilPane;
    @FXML private VBox navAccueil, navMessages;
    @FXML private HBox indicAccueil, indicMessages;
    @FXML private Label lblMessagesBadge;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private VBox currentActiveNav;

    private static final String NAV_NORMAL  =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:transparent;-fx-background-radius:0 10 10 0;";
    private static final String NAV_ACTIVE  =
            "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:0 10 10 0;";
    private static final String INDIC_HIDDEN  =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:transparent;-fx-background-radius:0 2 2 0;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:#E8956D;-fx-background-radius:0 2 2 0;";

    private static final String CARD_NORMAL =
            "-fx-background-color:white;-fx-padding:24;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,2);";
    private static final String CARD_HOVER  =
            "-fx-background-color:#FFF8F0;-fx-padding:24;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(232,149,109,0.18),14,0,0,4);";

    @FXML void initialize() { System.out.println("✅ PatientHome initialized"); }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        loadAffirmation();
        startNotifications();
        if (navAccueil   != null) navAccueil.setStyle(NAV_ACTIVE);
        if (indicAccueil != null) indicAccueil.setStyle(INDIC_VISIBLE);
        currentActiveNav = navAccueil;
    }

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) { System.err.println("Refresh: " + e.getMessage()); }

        lblWelcome.setText("Bonjour, " + currentUser.getPrenom() + " 👋");
        lblAvatarHeader.setText(currentUser.getPrenom().substring(0, 1).toUpperCase());
        if (lblNom   != null) lblNom.setText(currentUser.getPrenom() + " " + currentUser.getNom());
        if (lblEmail != null) lblEmail.setText(currentUser.getEmail());
        if (lblTel   != null) lblTel.setText(currentUser.getNum_tel() != null ? currentUser.getNum_tel() : "—");
        if (lblNbRdv     != null) lblNbRdv.setText("0");
        if (lblNbSeances != null) lblNbSeances.setText("0");
        updateHeaderPhoto();
    }

    private void updateHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(f.toURI() + "?t=" + System.currentTimeMillis(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblAvatarHeader.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblAvatarHeader.setVisible(true);
    }

    // ─── API 1: Affirmations.dev ──────────────────────────────
    private void loadAffirmation() {
        if (lblAffirmation == null) return;
        Platform.runLater(() -> {
            if (lblAffirmStatus != null) lblAffirmStatus.setText("⏳ Chargement...");
            lblAffirmation.setText("");
        });
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.affirmations.dev/"))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "EchoCare/1.0").GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String text = extractValue(resp.body(), "affirmation");
                Platform.runLater(() -> {
                    if (lblAffirmStatus != null) lblAffirmStatus.setText("✨ Affirmation du jour");
                    lblAffirmation.setText("❝  " + text + "  ❞");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblAffirmStatus != null) lblAffirmStatus.setText("✨ Affirmation");
                    lblAffirmation.setText("❝  Vous avez la force de surmonter tous les défis d'aujourd'hui.  ❞");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }



    private void startNotifications() {
        services.Notificationservice ns = services.Notificationservice.INSTANCE;
        ns.start(currentUser);

        ns.setOnUnreadCountChanged(count -> {
            if (lblMessagesBadge == null) return;
            if (count > 0) {
                lblMessagesBadge.setText(String.valueOf(count));
                lblMessagesBadge.setVisible(true);
                lblMessagesBadge.setManaged(true);
            } else {
                lblMessagesBadge.setVisible(false);
                lblMessagesBadge.setManaged(false);
            }
        });

        ns.setOnNewMessage(() ->
                utils.Notificationhelper.show("💬", "Nouveau message",
                        "Votre coach vous a envoyé un message", this::openMessagerie));

        ns.setOnIncomingCall(call -> {
            try {
                entities.User caller = us.getUserById(call.getId_caller());
                if (caller == null) return;
                utils.Notificationhelper.show("📞", "Appel entrant",
                        caller.getPrenom() + " " + caller.getNom() + " vous appelle",
                        () -> openCallScreen(caller, call));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void openMessagerie() {
        try {
            if (navMessages != null) navMessages.setStyle(NAV_ACTIVE);
            if (indicMessages != null) indicMessages.setStyle(INDIC_VISIBLE);
            currentActiveNav = navMessages;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Messagerie.fxml"));
            HBox page = loader.load();
            Messageriecontroller ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openCallScreen(entities.User caller, entities.Call call) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Messagerie.fxml"));
            HBox page = loader.load();
            Messageriecontroller ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            contentArea.getChildren().setAll(page);
            ctrl.openCallScreen(true, call, call.getId_call());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String extractValue(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s);
        if (i < 0) return "";
        i += s.length();
        int end = json.indexOf("\"", i);
        return end < 0 ? "" : json.substring(i, end).replace("\\n", " ").replace("\\'", "'");
    }

    @FXML void refreshAffirmation(MouseEvent e) { loadAffirmation(); }

    // ─── Navigation ───────────────────────────────────────────
    private void showAccueilFromProfil() {
        if (navAccueil   != null) navAccueil.setStyle(NAV_ACTIVE);
        if (indicAccueil != null) indicAccueil.setStyle(INDIC_VISIBLE);
        currentActiveNav = navAccueil;
        contentArea.getChildren().setAll(accueilPane);
    }

    @FXML void showAccueil(MouseEvent event) {
        if (navAccueil   != null) navAccueil.setStyle(NAV_ACTIVE);
        if (indicAccueil != null) indicAccueil.setStyle(INDIC_VISIBLE);
        currentActiveNav = navAccueil;
        contentArea.getChildren().setAll(accueilPane);
    }

    @FXML void onNavAccueilEnter(MouseEvent e) { if(navAccueil!=currentActiveNav) navAccueil.setStyle(NAV_ACTIVE); }
    @FXML void onNavAccueilExit(MouseEvent e)  { if(navAccueil!=currentActiveNav) navAccueil.setStyle(NAV_NORMAL); }

    @FXML void showProfil(MouseEvent event) {
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

    @FXML void onCardProfilEnter(MouseEvent e) { ((VBox) e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardProfilExit(MouseEvent e)  { ((VBox) e.getSource()).setStyle(CARD_NORMAL); }

    @FXML void showMessages(MouseEvent event) { openMessagerie(); }
    @FXML void onNavMessagesEnter(MouseEvent e) { if(navMessages!=currentActiveNav) navMessages.setStyle(NAV_ACTIVE); }
    @FXML void onNavMessagesExit(MouseEvent e)  { if(navMessages!=currentActiveNav) navMessages.setStyle(NAV_NORMAL); }

    @FXML void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Voulez-vous vraiment quitter ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}