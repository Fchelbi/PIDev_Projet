package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import entities.User;
import services.serviceUser;
import utils.LightDialog;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AdminHome {
    @FXML private Label lblWelcome, lblNbPatients, lblNbCoaches, lblNbAdmins, lblNbTotal;
    @FXML private Label lblHeaderAvatar;
    @FXML private PieChart pieChartRoles;
    @FXML private StackPane contentArea;
    @FXML private VBox dashboardPane;
    @FXML private HBox btnDashboard, btnUtilisateurs;
    @FXML private ImageView imgHeaderPhoto;
    @FXML private Circle headerAvatarCircle;

    private User currentUser;
    private final serviceUser us = new serviceUser();

    public void setUser(User user) {
        this.currentUser = user;
        lblWelcome.setText(user.getPrenom() + " " + user.getNom());
        lblHeaderAvatar.setText(user.getPrenom().substring(0, 1).toUpperCase());
        loadHeaderPhoto();
        refreshStats(null);
    }

    /**
     * Charger la photo dans le header
     */
    private void loadHeaderPhoto() {
        if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            File f = new File(currentUser.getPhoto());
            if (f.exists()) {
                imgHeaderPhoto.setImage(new Image(f.toURI().toString(), 40, 40, false, true));
                imgHeaderPhoto.setVisible(true);
                lblHeaderAvatar.setVisible(false);
                return;
            }
        }
        imgHeaderPhoto.setVisible(false);
        lblHeaderAvatar.setVisible(true);
    }

    @FXML void initialize() { System.out.println("✅ AdminHome initialized"); }

    @FXML void refreshStats(ActionEvent event) {
        try {
            List<User> users = us.selectALL();
            int p = 0, c = 0, a = 0;
            for (User u : users) {
                switch (u.getRole().toUpperCase()) {
                    case "PATIENT": p++; break;
                    case "COACH": c++; break;
                    case "ADMIN": a++; break;
                }
            }
            lblNbPatients.setText(String.valueOf(p));
            lblNbCoaches.setText(String.valueOf(c));
            lblNbAdmins.setText(String.valueOf(a));
            lblNbTotal.setText(String.valueOf(users.size()));

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Patients (" + p + ")", p),
                    new PieChart.Data("Coaches (" + c + ")", c),
                    new PieChart.Data("Admins (" + a + ")", a));
            pieChartRoles.setData(pieData);
            pieChartRoles.getData().get(0).getNode().setStyle("-fx-pie-color: #A7B5E0;");
            pieChartRoles.getData().get(1).getNode().setStyle("-fx-pie-color: #D4A5BD;");
            pieChartRoles.getData().get(2).getNode().setStyle("-fx-pie-color: #C3CEF0;");
        } catch (SQLException e) {
            LightDialog.showError("Erreur", "Impossible de charger les statistiques.");
        }
    }

    @FXML void showDashboard(MouseEvent event) {
        highlightButton(btnDashboard);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(dashboardPane);
        refreshStats(null);
    }

    @FXML void showUtilisateurs(MouseEvent event) {
        try {
            highlightButton(btnUtilisateurs);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionUtilisateurs.fxml"));
            VBox page = loader.load();
            ((GestionUtilisateurs) loader.getController()).setCurrentUser(currentUser);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger la page.");
        }
    }

    @FXML void showProfil(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Profil.fxml"));
            ScrollPane page = loader.load();
            ((Profil) loader.getController()).setCurrentUser(currentUser);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (IOException e) {
            e.printStackTrace();
            LightDialog.showError("Erreur", "Impossible de charger le profil.");
        }
    }

    private void highlightButton(HBox active) {
        String reset = "-fx-padding: 14 25; -fx-cursor: hand;";
        String activeStyle = "-fx-padding: 14 25; -fx-background-color: linear-gradient(to right, rgba(167,181,224,0.12), transparent); " +
                "-fx-border-color: transparent transparent transparent #A7B5E0; -fx-border-width: 0 0 0 3; -fx-cursor: hand;";
        btnDashboard.setStyle(reset);
        btnUtilisateurs.setStyle(reset);
        active.setStyle(activeStyle);
    }

    @FXML void handleLogout(ActionEvent event) {
        if (LightDialog.showConfirmation("Déconnexion", "Êtes-vous sûr ?", "👋")) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
                ((Stage) lblWelcome.getScene().getWindow()).setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}