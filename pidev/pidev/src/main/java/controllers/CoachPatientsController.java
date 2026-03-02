package controllers;

import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import services.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class CoachPatientsController implements Initializable {

    @FXML private VBox patientsList;
    @FXML private Label lblCount;

    private QuizResultService quizResultService;
    private ParticipantService participantService;
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        loadPatients();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        quizResultService = new QuizResultService();
        participantService = new ParticipantService();
    }

    private void loadPatients() {
        if (patientsList == null) return;
        patientsList.getChildren().clear();
        try {
            List<QuizResult> allResults = quizResultService.selectALL();
            List<Participant> allPartic = participantService.selectALL();

            Map<Integer, List<QuizResult>> byPatient = new LinkedHashMap<>();
            for (QuizResult r : allResults)
                byPatient.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);

            if (lblCount != null) lblCount.setText(byPatient.size() + " patient(s)");

            if (byPatient.isEmpty()) {
                Label empty = new Label("Aucun patient n'a encore passé de quiz.");
                empty.setStyle("-fx-font-size:15px;-fx-text-fill:#A0AEC0;-fx-padding:30;");
                patientsList.getChildren().add(empty);
                return;
            }

            for (Map.Entry<Integer, List<QuizResult>> entry : byPatient.entrySet()) {
                int pid = entry.getKey();
                List<QuizResult> results = entry.getValue();
                long passed = results.stream().filter(QuizResult::isPassed).count();
                double avg = results.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
                long inscrits = allPartic.stream().filter(p -> p.getUserId() == pid).count();

                HBox card = new HBox(15);
                card.setPadding(new Insets(14));
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

                Circle avatar = new Circle(22);
                avatar.setFill(Color.web(avg >= 70 ? "#00b894" : "#E8956D"));
                Label avLbl = new Label("P" + pid);
                avLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11px;");
                StackPane avPane = new StackPane(avatar, avLbl);

                VBox info = new VBox(3);
                Label pLbl = new Label("Patient #" + pid);
                pLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#2d3436;");
                Label sLbl = new Label(inscrits + " formation(s) | "
                        + results.size() + " quiz | " + passed + " réussi(s)");
                sLbl.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
                info.getChildren().addAll(pLbl, sLbl);

                HBox spacer = new HBox();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                ProgressBar pb = new ProgressBar(avg / 100.0);
                pb.setPrefWidth(100);
                pb.setStyle("-fx-accent:" + (avg >= 70 ? "#00b894" : "#d63031") + ";");

                Label avgLbl = new Label(String.format("%.0f%%", avg));
                avgLbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:"
                        + (avg >= 70 ? "#00b894" : "#d63031") + ";");

                card.getChildren().addAll(avPane, info, spacer, pb, avgLbl);
                patientsList.getChildren().add(card);
            }
        } catch (SQLException e) {
            patientsList.getChildren().add(new Label("Erreur: " + e.getMessage()));
        }
    }
}