package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.*;
import utils.VideoPlayerUtil;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class PatientFormationsController implements Initializable {

    @FXML private VBox rootContainer;
    @FXML private TextField tfSearch;
    @FXML private FlowPane cardsPane;
    @FXML private Label lblCount;

    private FormationService formationService;
    private ParticipantService participantService;
    private QuizService quizService;
    private QuizResultService quizResultService;

    private User currentUser;
    private int currentUserId = 1;

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            this.currentUserId = user.getId_user();
        }
        loadFormations();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        formationService = new FormationService();
        participantService = new ParticipantService();
        quizService = new QuizService();
        quizResultService = new QuizResultService();
    }

    private void loadFormations() {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();
        try {
            List<Formation> formations = formationService.selectALL();
            if (tfSearch != null) {
                tfSearch.textProperty().addListener((obs, o, n) -> filterFormations(formations, n));
            }
            for (Formation f : formations) {
                cardsPane.getChildren().add(createCard(f));
            }
            if (lblCount != null) {
                lblCount.setText(formations.size() + " formations disponibles");
            }
            if (formations.isEmpty()) {
                Label empty = new Label("Aucune formation disponible pour le moment.");
                empty.setStyle("-fx-font-size:15px;-fx-text-fill:#A0AEC0;-fx-padding:40;");
                cardsPane.getChildren().add(empty);
            }
        } catch (SQLException e) {
            cardsPane.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }

    private void filterFormations(List<Formation> all, String keyword) {
        cardsPane.getChildren().clear();
        String kw = keyword == null ? "" : keyword.toLowerCase().trim();
        int count = 0;
        for (Formation f : all) {
            if (kw.isEmpty()
                    || f.getTitle().toLowerCase().contains(kw)
                    || f.getCategory().toLowerCase().contains(kw)
                    || f.getDescription().toLowerCase().contains(kw)) {
                cardsPane.getChildren().add(createCard(f));
                count++;
            }
        }
        if (lblCount != null) lblCount.setText(count + " formations trouvées");
        if (count == 0) {
            Label empty = new Label("Aucune formation trouvée pour: " + keyword);
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#A0AEC0;-fx-padding:30;");
            cardsPane.getChildren().add(empty);
        }
    }

    private VBox createCard(Formation f) {
        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");
        card.setPadding(new Insets(20));

        Label lblCat = new Label(f.getCategory() != null ? f.getCategory() : "—");
        lblCat.setStyle("-fx-background-color:#E8956D;-fx-text-fill:white;"
                + "-fx-padding:3 10;-fx-background-radius:10;-fx-font-size:11px;");

        Label lblTitle = new Label(f.getTitle());
        lblTitle.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2d3436;");
        lblTitle.setWrapText(true);

        Label lblDesc = new Label(f.getDescription());
        lblDesc.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(50);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);

        boolean enrolled = isEnrolled(f.getId());

        if (!enrolled) {
            Button btnReg = new Button("S'inscrire");
            btnReg.setStyle("-fx-background-color:#00b894;-fx-text-fill:white;"
                    + "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 14;");
            btnReg.setOnAction(e -> {
                registerToFormation(f);
                loadFormations();
            });
            buttons.getChildren().add(btnReg);
        } else {
            Label enrolled_lbl = new Label("✅ Inscrit");
            enrolled_lbl.setStyle("-fx-text-fill:#00b894;-fx-font-weight:bold;-fx-font-size:12px;");
            buttons.getChildren().add(enrolled_lbl);
        }

        // Video button
        if (f.getVideoUrl() != null && !f.getVideoUrl().trim().isEmpty()) {
            Button btnVideo = new Button("▶ Vidéo");
            btnVideo.setStyle("-fx-background-color:#6c5ce7;-fx-text-fill:white;"
                    + "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:6 12;");
            btnVideo.setOnAction(e -> watchVideo(f));
            buttons.getChildren().add(btnVideo);
        }

        card.getChildren().addAll(lblCat, lblTitle, lblDesc, buttons);
        return card;
    }

    private boolean isEnrolled(int formationId) {
        try {
            return participantService.isAlreadyRegistered(currentUserId, formationId);
        } catch (SQLException e) {
            return false;
        }
    }

    private void registerToFormation(Formation f) {
        try {
            if (participantService.isAlreadyRegistered(currentUserId, f.getId())) {
                showInfo("Vous êtes déjà inscrit à cette formation!");
                return;
            }
            Participant p = new Participant();
            p.setUserId(currentUserId);
            p.setFormationId(f.getId());
            participantService.insertOne(p);
            showInfo("Inscription réussie à: " + f.getTitle() + " ✅");
        } catch (SQLException e) {
            showInfo("Erreur: " + e.getMessage());
        }
    }

    private void watchVideo(Formation f) {
        String videoUrl = f.getVideoUrl();
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            showInfo("Aucune vidéo disponible.");
            return;
        }
        // Open in a simple dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🎬 " + f.getTitle());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(700, 500);

        VBox container;
        if (VideoPlayerUtil.isYouTubeUrl(videoUrl)) {
            container = VideoPlayerUtil.createYouTubePlayer(videoUrl);
        } else {
            File file = new File(videoUrl);
            if (file.exists() && videoUrl.toLowerCase().endsWith(".mp4")) {
                container = VideoPlayerUtil.createLocalPlayer(videoUrl);
            } else {
                container = VideoPlayerUtil.createErrorMessage("Fichier non trouvé", videoUrl);
            }
        }
        dialog.getDialogPane().setContent(container);
        dialog.setOnCloseRequest(e -> VideoPlayerUtil.stopMedia(container));
        dialog.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
