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
import services.serviceUser;
import utils.LightDialog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.sql.SQLException;
import java.time.Duration;

/**
 * PatientHome — contrôleur principal patient, wired to PatientHome.fxml.
 *
 * FICHIERS À SUPPRIMER :
 *   DELETE AdminController.java  (wired to old AdminDashboard.fxml — disparu)
 *   DELETE CoachController.java  (wired to old CoachDashboard.fxml — disparu)
 *   DELETE PatientController.java (la grosse classe avec takeQuiz, showAccueil...)
 *          wired to PatientDashboard.fxml — disparu
 *
 *   GARDER PatientPageController.java (l'interface légère, juste setUser())
 *          utilisée par les 3 sub-pages formations du patient.
 *
 * loadSubPage() utilise PatientPageController (interface) — pas PatientController (classe).
 */
public class PatientHome {

    @FXML private Label lblWelcome, lblAvatarHeader;
    @FXML private Label lblNbRdv, lblNbSeances;
    @FXML private Label lblNom, lblEmail, lblTel;
    @FXML private Label lblAffirmation, lblAffirmStatus;
    @FXML private Label lblAdvice, lblAdviceStatus;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane accueilPane;
    @FXML private VBox navMessages;
    @FXML private HBox indicMessages;
    @FXML private Label lblMessagesBadge;
    @FXML private VBox navAccueil, navFormations, navMesFormations, navResultats, navProfil;
    @FXML private HBox indicAccueil, indicFormations, indicMesFormations, indicResultats, indicProfil;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private VBox currentActiveNav;

    private static final String NAV_NORMAL    = "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:transparent;-fx-background-radius:0 10 10 0;";
    private static final String NAV_ACTIVE    = "-fx-padding:12 20 12 16;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:0 10 10 0;";
    private static final String INDIC_HIDDEN  = "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:transparent;-fx-background-radius:0 2 2 0;";
    private static final String INDIC_VISIBLE = "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:#E8956D;-fx-background-radius:0 2 2 0;";
    private static final String CARD_NORMAL   = "-fx-background-color:white;-fx-padding:20;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,2);";
    private static final String CARD_HOVER    = "-fx-background-color:#FFF8F0;-fx-padding:20;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(232,149,109,0.22),14,0,0,5);";

    @FXML void initialize() { System.out.println("PatientHome initialized"); }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        loadAffirmation();
        loadAdvice();
        startNotifications();
        setActiveNav(navAccueil, indicAccueil);
    }

    public void refreshUserData() {
        try { User u = us.getUserById(currentUser.getId_user()); if (u != null) currentUser = u; }
        catch (SQLException e) { System.err.println("Refresh: " + e.getMessage()); }
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
                imgHeaderPhoto.setVisible(true); lblAvatarHeader.setVisible(false); return;
            }
        }
        imgHeaderPhoto.setVisible(false); lblAvatarHeader.setVisible(true);
    }

    private void loadAffirmation() {
        if (lblAffirmation == null) return;
        Platform.runLater(() -> { if (lblAffirmStatus != null) lblAffirmStatus.setText("⏳ Chargement..."); lblAffirmation.setText(""); });
        new Thread(() -> {
            try {
                HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpResponse<String> resp = c.send(HttpRequest.newBuilder().uri(URI.create("https://www.affirmations.dev/")).timeout(Duration.ofSeconds(8)).header("User-Agent","EchoCare/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
                String text = extractValue(resp.body(), "affirmation");
                Platform.runLater(() -> { if (lblAffirmStatus!=null) lblAffirmStatus.setText("✨ Affirmation du jour"); lblAffirmation.setText("❝  " + text + "  ❞"); });
            } catch (Exception e) {
                Platform.runLater(() -> { if (lblAffirmStatus!=null) lblAffirmStatus.setText("✨ Affirmation"); lblAffirmation.setText("❝  Vous avez la force de surmonter tous les défis d'aujourd'hui.  ❞"); });
            }
        }, "affirmation-thread").start();
    }

    private void loadAdvice() {
        if (lblAdvice == null) return;
        Platform.runLater(() -> { if (lblAdviceStatus != null) lblAdviceStatus.setText("⏳ Chargement..."); lblAdvice.setText(""); });
        new Thread(() -> {
            try {
                HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
                HttpResponse<String> resp = c.send(HttpRequest.newBuilder().uri(URI.create("https://api.adviceslip.com/advice")).timeout(Duration.ofSeconds(8)).header("User-Agent","EchoCare/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
                String text = extractValue(resp.body(), "advice");
                Platform.runLater(() -> { if (lblAdviceStatus!=null) lblAdviceStatus.setText("🌿 Conseil bien-être"); lblAdvice.setText("💡  " + text); });
            } catch (Exception e) {
                Platform.runLater(() -> { if (lblAdviceStatus!=null) lblAdviceStatus.setText("🌿 Conseil"); lblAdvice.setText("💡  Prenez soin de votre santé mentale chaque jour."); });
            }
        }, "advice-thread").start();
    }

    @FXML void refreshAffirmation(MouseEvent e) { loadAffirmation(); }
    @FXML void refreshAdvice(MouseEvent e) { loadAdvice(); }

    private void startNotifications() {
        try {
            services.Notificationservice ns = services.Notificationservice.INSTANCE;
            ns.start(currentUser);
            ns.setOnUnreadCountChanged(count -> {
                if (lblMessagesBadge == null) return;
                if (count > 0) { lblMessagesBadge.setText(count > 9 ? "9+" : String.valueOf(count)); lblMessagesBadge.setVisible(true); lblMessagesBadge.setManaged(true); }
                else { lblMessagesBadge.setVisible(false); lblMessagesBadge.setManaged(false); }
            });
            ns.setOnNewMessage(this::openMessagerie);
            ns.setOnIncomingCall(call -> {
                try {
                    entities.User caller = us.getUserById(call.getId_caller());
                    if (caller == null) return;
                    utils.Notificationhelper.show("📞", caller.getPrenom() + " " + caller.getNom() + " · " + caller.getRole(), "Appel entrant — Appuyez pour répondre", () -> openCallScreen(caller, call));
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } catch (Exception e) { System.err.println("Notifications not available: " + e.getMessage()); }
    }

    private void openMessagerie() {
        try {
            setActiveNav(navMessages, indicMessages);
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
            ctrl.setCurrentUser(currentUser); ctrl.setSelectedContact(caller);
            contentArea.getChildren().setAll(page);
            ctrl.openCallScreen(true, call, call.getId_call());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAccueilFromProfil() { setActiveNav(navAccueil, indicAccueil); contentArea.getChildren().setAll(accueilPane); }
    @FXML void showAccueil(MouseEvent event)       { setActiveNav(navAccueil, indicAccueil); contentArea.getChildren().setAll(accueilPane); }
    @FXML void showAllFormations(MouseEvent event) { setActiveNav(navFormations, indicFormations); loadSubPage("PatientFormations.fxml", "Formations"); }
    @FXML void showMyFormations(MouseEvent event)  { setActiveNav(navMesFormations, indicMesFormations); loadSubPage("PatientMesFormations.fxml", "Mes Formations"); }
    @FXML void showMyResults(MouseEvent event)     { setActiveNav(navResultats, indicResultats); loadSubPage("PatientResultats.fxml", "Mes Résultats"); }
    @FXML void showMessages(MouseEvent event)      { openMessagerie(); }

    @FXML void showProfil(MouseEvent event) {
        setActiveNav(navProfil, indicProfil);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            Profil ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser); ctrl.setOnPhotoChanged(this::refreshUserData); ctrl.setOnBackToAccueil(this::showAccueilFromProfil);
            contentArea.getChildren().setAll(page);
        } catch (IOException e) { e.printStackTrace(); LightDialog.showError("Erreur", "Impossible de charger le profil."); }
    }

    // FIXED: uses PatientPageController interface — NOT PatientController class
    private void loadSubPage(String fxmlName, String title) {
        try {
            java.net.URL res = getClass().getResource("/" + fxmlName);
            if (res == null) { contentArea.getChildren().setAll(buildPlaceholder(title)); return; }
            FXMLLoader loader = new FXMLLoader(res);
            Parent page = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof PatientPageController ppc) {
                ppc.setUser(currentUser);
            } else if (ctrl != null) {
                try { ctrl.getClass().getMethod("setUser", entities.User.class).invoke(ctrl, currentUser); }
                catch (NoSuchMethodException ignored) {}
                catch (Exception e) { System.err.println("[PatientHome] setUser() failed: " + e.getMessage()); }
            }
            contentArea.getChildren().setAll(page);
        } catch (IOException e) { e.printStackTrace(); contentArea.getChildren().setAll(buildPlaceholder(title)); }
    }

    private ScrollPane buildPlaceholder(String title) {
        VBox outer = new VBox(0);
        VBox content = new VBox(16); content.setAlignment(javafx.geometry.Pos.CENTER); content.setStyle("-fx-padding:80;");
        Label icon = new Label("🚧"); icon.setStyle("-fx-font-size:50px;");
        Label msg  = new Label("Page en cours de développement"); msg.setStyle("-fx-font-size:17px;-fx-font-weight:600;-fx-text-fill:#4A5568;");
        content.getChildren().addAll(icon, msg);
        outer.getChildren().add(content);
        ScrollPane sp = new ScrollPane(outer); sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:#F2F0EC;");
        return sp;
    }

    @FXML void onNavAccueilEnter(MouseEvent e)        { if (navAccueil       != currentActiveNav) navAccueil.setStyle(NAV_ACTIVE); }
    @FXML void onNavAccueilExit(MouseEvent e)         { if (navAccueil       != currentActiveNav) navAccueil.setStyle(NAV_NORMAL); }
    @FXML void onNavFormationsEnter(MouseEvent e)     { if (navFormations    != currentActiveNav) navFormations.setStyle(NAV_ACTIVE); }
    @FXML void onNavFormationsExit(MouseEvent e)      { if (navFormations    != currentActiveNav) navFormations.setStyle(NAV_NORMAL); }
    @FXML void onNavMesFormationsEnter(MouseEvent e)  { if (navMesFormations != currentActiveNav) navMesFormations.setStyle(NAV_ACTIVE); }
    @FXML void onNavMesFormationsExit(MouseEvent e)   { if (navMesFormations != currentActiveNav) navMesFormations.setStyle(NAV_NORMAL); }
    @FXML void onNavResultatsEnter(MouseEvent e)      { if (navResultats     != currentActiveNav) navResultats.setStyle(NAV_ACTIVE); }
    @FXML void onNavResultatsExit(MouseEvent e)       { if (navResultats     != currentActiveNav) navResultats.setStyle(NAV_NORMAL); }
    @FXML void onNavProfilEnter(MouseEvent e)         { if (navProfil        != currentActiveNav) navProfil.setStyle(NAV_ACTIVE); }
    @FXML void onNavProfilExit(MouseEvent e)          { if (navProfil        != currentActiveNav) navProfil.setStyle(NAV_NORMAL); }
    @FXML void onNavMessagesEnter(MouseEvent e)       { if (navMessages      != currentActiveNav) navMessages.setStyle(NAV_ACTIVE); }
    @FXML void onNavMessagesExit(MouseEvent e)        { if (navMessages      != currentActiveNav) navMessages.setStyle(NAV_NORMAL); }
    @FXML void onCardFormationsEnter(MouseEvent e)    { ((VBox)e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardFormationsExit(MouseEvent e)     { ((VBox)e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardMesFormationsEnter(MouseEvent e) { ((VBox)e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardMesFormationsExit(MouseEvent e)  { ((VBox)e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardResultatsEnter(MouseEvent e)     { ((VBox)e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardResultatsExit(MouseEvent e)      { ((VBox)e.getSource()).setStyle(CARD_NORMAL); }
    @FXML void onCardProfilEnter(MouseEvent e)        { ((VBox)e.getSource()).setStyle(CARD_HOVER); }
    @FXML void onCardProfilExit(MouseEvent e)         { ((VBox)e.getSource()).setStyle(CARD_NORMAL); }

    private void setActiveNav(VBox nav, HBox indic) {
        VBox[] navs   = {navAccueil, navFormations, navMesFormations, navResultats, navProfil, navMessages};
        HBox[] indics = {indicAccueil, indicFormations, indicMesFormations, indicResultats, indicProfil, indicMessages};
        for (VBox n : navs)   if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i : indics) if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    private String extractValue(String json, String key) {
        String s = "\"" + key + "\":\"";
        int i = json.indexOf(s); if (i < 0) return "";
        i += s.length();
        int end = json.indexOf("\"", i);
        return end < 0 ? "" : json.substring(i, end).replace("\\n"," ").replace("\\'","'");
    }

    @FXML void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Voulez-vous vraiment quitter ?", "👋")) {
            try { Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml")); ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root)); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }
}