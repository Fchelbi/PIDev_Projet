package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Mes Résultats de Quiz.
 * implements PatientPageController — NOT PatientController (that class is untouched).
 */
public class PatientResultatsController implements Initializable, PatientController {

    @FXML private VBox resultsList;
    @FXML private Label lblCount;

    private QuizResultService  quizResultService;
    private QuizService        quizService;
    private CertificateService certificateService;

    private User currentUser;
    private int  currentUserId = 1;

    @Override
    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) this.currentUserId = user.getId_user();
        loadResults();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        quizResultService  = new QuizResultService();
        quizService        = new QuizService();
        certificateService = new CertificateService();
    }

    // ════════════════════════════════════════════════════════════════════
    //  LOAD
    // ════════════════════════════════════════════════════════════════════

    private void loadResults() {
        if (resultsList == null) return;
        resultsList.getChildren().clear();
        try {
            List<QuizResult> results = quizResultService.selectByUser(currentUserId);
            if (lblCount != null) lblCount.setText(results.size() + " résultat(s)");

            if (results.isEmpty()) {
                VBox empty = new VBox(10);
                empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(50));
                Label icon = new Label("📭"); icon.setStyle("-fx-font-size:40px;");
                Label txt  = new Label("Aucun résultat de quiz"); txt.setStyle("-fx-font-size:16px;-fx-text-fill:#A0AEC0;-fx-font-weight:600;");
                Label sub  = new Label("Inscrivez-vous à une formation et passez un quiz"); sub.setStyle("-fx-font-size:12px;-fx-text-fill:#CBD5E0;");
                empty.getChildren().addAll(icon, txt, sub);
                resultsList.getChildren().add(empty);
                return;
            }
            for (QuizResult r : results) resultsList.getChildren().add(createResultCard(r));
        } catch (SQLException e) {
            resultsList.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  CARD
    // ════════════════════════════════════════════════════════════════════

    private HBox createResultCard(QuizResult r) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        Label icon = new Label(r.isPassed() ? "✅" : "❌");
        icon.setStyle("-fx-font-size:24px;");

        String quizName = "Quiz #" + r.getQuizId();
        String formationTitle = "";
        try {
            for (Quiz q : quizService.selectALL()) {
                if (q.getId() == r.getQuizId()) {
                    quizName = q.getTitle();
                    try {
                        for (Formation f : new FormationService().selectALL()) {
                            if (f.getId() == q.getFormationId()) {
                                formationTitle = f.getTitle();
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                    break;
                }
            }
        } catch (SQLException ignored) {}

        VBox info = new VBox(3);
        Label lblQuiz = new Label(quizName);
        lblQuiz.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#2d3436;");
        info.getChildren().add(lblQuiz);
        if (!formationTitle.isEmpty()) {
            Label lblF = new Label("📚 " + formationTitle);
            lblF.setStyle("-fx-text-fill:#4A6FA5;-fx-font-size:12px;");
            info.getChildren().add(lblF);
        }
        Label lblDate = new Label(r.getCompletedAt() != null ? r.getCompletedAt().toString().substring(0, 16) : "—");
        lblDate.setStyle("-fx-text-fill:#b2bec3;-fx-font-size:11px;");
        Label lblDetail = new Label(r.getScore() + "/" + r.getTotalPoints() + " pts");
        lblDetail.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
        info.getChildren().addAll(lblDate, lblDetail);

        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(5); right.setAlignment(Pos.CENTER);
        Label lblPct = new Label(String.format("%.0f%%", r.getPercentage()));
        lblPct.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:" + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        Label lblStatus = new Label(r.isPassed() ? "RÉUSSI" : "ÉCHOUÉ");
        lblStatus.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:" + (r.isPassed() ? "#00b894" : "#d63031") + ";");
        right.getChildren().addAll(lblPct, lblStatus);

        if (r.isPassed() && !formationTitle.isEmpty()) {
            Button btnCert = new Button("📜 Certificat");
            btnCert.setStyle("-fx-background-color:#fdcb6e;-fx-text-fill:#2d3436;"
                    + "-fx-cursor:hand;-fx-background-radius:6;-fx-padding:4 10;-fx-font-size:11px;");
            final String fTitle = formationTitle;
            btnCert.setOnAction(e -> downloadCertificate(r, fTitle));
            right.getChildren().add(btnCert);
        }

        card.getChildren().addAll(icon, info, spacer, right);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    //  CERTIFICATE
    // ════════════════════════════════════════════════════════════════════

    private void downloadCertificate(QuizResult r, String formationTitle) {
        String name = currentUser != null ? currentUser.getPrenom() + " " + currentUser.getNom() : "Patient #" + currentUserId;
        String cert = certificateService.generateCertificate(name, formationTitle, r.getScore(), r.getTotalPoints(), r.getPercentage());
        if (cert != null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("📜 Certificat"); a.setContentText("🎉 Félicitations ! Votre certificat a été généré avec succès.");
            ButtonType openBtn = new ButtonType("📂 Ouvrir");
            ButtonType later   = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(openBtn, later);
            a.showAndWait().ifPresent(b -> {
                if (b == openBtn) { try { java.awt.Desktop.getDesktop().open(new java.io.File(cert)); } catch (Exception ex) {} }
            });
        } else showInfo("Erreur lors de la génération du certificat.");
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}