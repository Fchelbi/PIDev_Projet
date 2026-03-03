package controllers;

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
import javafx.scene.web.WebView;
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
import java.sql.SQLException;
import java.util.List;

public class AdminHome {

    // Top stats
    @FXML private Label lblWelcome, lblHeaderAvatar;
    @FXML private Label lblNbPatients, lblNbCoaches, lblNbAdmins, lblNbTotal;
    @FXML private Label lblPctPatients, lblPctCoaches, lblPctAdmins;
    @FXML private ProgressBar pbPatients, pbCoaches, pbAdmins;


    @FXML private StackPane contentArea;
    @FXML private WebView wvChartAdmin;
    @FXML private ScrollPane dashboardPane;
    @FXML private ImageView imgHeaderPhoto;

    @FXML private VBox navDashboard, navUtilisateurs, navProfil;
    @FXML private HBox indicDashboard, indicUtilisateurs, indicProfil;

    private User currentUser;
    private final serviceUser us = new serviceUser();
    private VBox currentActiveNav;

    private static final String NAV_NORMAL =
            "-fx-padding:12 20 12 20;-fx-cursor:hand;-fx-background-color:transparent;-fx-background-radius:10;";
    private static final String NAV_ACTIVE =
            "-fx-padding:12 20 12 20;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.13);-fx-background-radius:10;";
    private static final String INDIC_HIDDEN =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:transparent;-fx-background-radius:2;";
    private static final String INDIC_VISIBLE =
            "-fx-min-width:4;-fx-max-width:4;-fx-min-height:30;-fx-background-color:#E8956D;-fx-background-radius:2;";

    @FXML void initialize() { System.out.println("✅ AdminHome initialized"); }

    public void setUser(User user) {
        this.currentUser = user;
        refreshUserData();
        refreshStats(null);
        setActiveNav(navDashboard, indicDashboard);
    }

    public void refreshUserData() {
        try {
            User updated = us.getUserById(currentUser.getId_user());
            if (updated != null) this.currentUser = updated;
        } catch (SQLException e) { System.err.println("Refresh: " + e.getMessage()); }
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

    // ─── Stats avec progress bars ──────────────────────────────
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
            int total = users.size();
            lblNbPatients.setText(String.valueOf(p));
            lblNbCoaches.setText(String.valueOf(c));
            lblNbAdmins.setText(String.valueOf(a));
            lblNbTotal.setText(String.valueOf(total));

            if (total > 0) {
                double pp = p / (double) total;
                double pc = c / (double) total;
                double pa = a / (double) total;
                if (pbPatients != null) pbPatients.setProgress(pp);
                if (pbCoaches  != null) pbCoaches.setProgress(pc);
                if (pbAdmins   != null) pbAdmins.setProgress(pa);
                if (lblPctPatients != null) lblPctPatients.setText(String.format("%.0f%%", pp * 100));
                if (lblPctCoaches  != null) lblPctCoaches.setText(String.format("%.0f%%", pc * 100));
                if (lblPctAdmins   != null) lblPctAdmins.setText(String.format("%.0f%%", pa * 100));
            }
            loadAdminCharts(p, c, a, total);
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les stats.");
        }
    }


    // ─── Navigation ───────────────────────────────────────────
    @FXML void showDashboard(MouseEvent event) {
        setActiveNav(navDashboard, indicDashboard);
        contentArea.getChildren().setAll(dashboardPane);
        refreshStats(null);
    }

    @FXML void showUtilisateurs(MouseEvent event) {
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

    @FXML void showProfil(MouseEvent event) {
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
            LightDialog.showError("Erreur", "Chargement impossible.");
        }
    }

    private void showDashboardFromProfil() {
        setActiveNav(navDashboard, indicDashboard);
        contentArea.getChildren().setAll(dashboardPane);
        refreshStats(null);
    }

    private void setActiveNav(VBox nav, HBox indic) {
        for (VBox n : new VBox[]{navDashboard, navUtilisateurs, navProfil})
            if (n != null) n.setStyle(NAV_NORMAL);
        for (HBox i : new HBox[]{indicDashboard, indicUtilisateurs, indicProfil})
            if (i != null) i.setStyle(INDIC_HIDDEN);
        if (nav   != null) nav.setStyle(NAV_ACTIVE);
        if (indic != null) indic.setStyle(INDIC_VISIBLE);
        currentActiveNav = nav;
    }

    @FXML void onNavDashboardEnter(MouseEvent e) { if(navDashboard   !=currentActiveNav) navDashboard.setStyle(NAV_ACTIVE); }
    @FXML void onNavDashboardExit(MouseEvent e)  { if(navDashboard   !=currentActiveNav) navDashboard.setStyle(NAV_NORMAL); }
    @FXML void onNavUtilsEnter(MouseEvent e)     { if(navUtilisateurs!=currentActiveNav) navUtilisateurs.setStyle(NAV_ACTIVE); }
    @FXML void onNavUtilsExit(MouseEvent e)      { if(navUtilisateurs!=currentActiveNav) navUtilisateurs.setStyle(NAV_NORMAL); }
    @FXML void onNavProfilEnter(MouseEvent e)    { if(navProfil      !=currentActiveNav) navProfil.setStyle(NAV_ACTIVE); }
    @FXML void onNavProfilExit(MouseEvent e)     { if(navProfil      !=currentActiveNav) navProfil.setStyle(NAV_NORMAL); }

    @FXML void handleLogout(MouseEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Voulez-vous vraiment quitter ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void loadAdminCharts(int patients, int coaches, int admins, int total) {
        if (wvChartAdmin == null) return;
        if (total == 0) {
            wvChartAdmin.getEngine().loadContent(
                    "<html><body style='display:flex;align-items:center;justify-content:center;" +
                            "height:260px;font-family:sans-serif;color:#A0AEC0;'>" +
                            "<p>Aucune donnée disponible</p></body></html>");
            return;
        }
        String html = """
            <!DOCTYPE html><html>
            <head>
            <script src="https://www.gstatic.com/charts/loader.js"></script>
            <script>
              google.charts.load('current',{packages:['corechart']});
              google.charts.setOnLoadCallback(draw);
              function draw() {
                drawPie(); drawBar();
              }
              function drawPie() {
                var d = google.visualization.arrayToDataTable([
                  ['Role','Nombre'],
                  ['Patients',%d],
                  ['Coaches',%d],
                  ['Admins',%d]
                ]);
                new google.visualization.PieChart(document.getElementById('pie')).draw(d,{
                  pieHole:0.42,
                  colors:['#E8956D','#F5C87A','#4A6FA5'],
                  backgroundColor:'transparent',
                  legend:{position:'bottom',textStyle:{color:'#718096',fontSize:10}},
                  chartArea:{width:'90%%',height:'75%%'},
                  title:'Repartition',
                  titleTextStyle:{color:'#2D3748',fontSize:12,bold:true}
                });
              }
              function drawBar() {
                var d = google.visualization.arrayToDataTable([
                  ['Role','Nombre',{role:'style'},{role:'annotation'}],
                  ['Patients',%d,'color:#E8956D','%d'],
                  ['Coaches', %d,'color:#F5C87A','%d'],
                  ['Admins',  %d,'color:#4A6FA5','%d']
                ]);
                new google.visualization.BarChart(document.getElementById('bar')).draw(d,{
                  legend:{position:'none'},
                  backgroundColor:'transparent',
                  hAxis:{minValue:0,textStyle:{color:'#718096',fontSize:10}},
                  vAxis:{textStyle:{color:'#718096',fontSize:11,bold:true}},
                  chartArea:{width:'70%%',height:'78%%'},
                  bar:{groupWidth:'55%%'},
                  annotations:{alwaysOutside:true,textStyle:{fontSize:11,bold:true}},
                  title:'Nombre par role',
                  titleTextStyle:{color:'#2D3748',fontSize:12,bold:true}
                });
              }
            </script>
            <style>
              body{margin:0;padding:4px;background:transparent;}
              .row{display:flex;gap:12px;height:270px;}
              .box{flex:1;background:white;border-radius:10px;overflow:hidden;
                   box-shadow:0 1px 6px rgba(0,0,0,0.06);}
            </style>
            </head>
            <body><div class="row">
              <div class="box" id="pie"></div>
              <div class="box" id="bar"></div>
            </div></body></html>
            """.formatted(
                patients, coaches, admins,
                patients, patients, coaches, coaches, admins, admins
        );
        wvChartAdmin.getEngine().loadContent(html);
    }

}