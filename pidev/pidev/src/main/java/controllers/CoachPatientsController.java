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
import entities.Formation;
import services.FormationService;
import services.serviceUser;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
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
            // ── Only formations belonging to this coach ──
            FormationService formationService = new FormationService();
            List<Formation> coachFormations = (currentUser != null)
                    ? formationService.selectByCoach(currentUser.getId_user())
                    : formationService.selectALL();

            List<Integer> coachFormationIds = coachFormations.stream()
                    .map(Formation::getId).collect(java.util.stream.Collectors.toList());

            List<Participant> allPartic = participantService.selectALL().stream()
                    .filter(p -> coachFormationIds.contains(p.getFormationId()))
                    .collect(java.util.stream.Collectors.toList());

            // Only patients enrolled in coach's formations
            List<Integer> coachPatientIds = allPartic.stream()
                    .map(Participant::getUserId).distinct()
                    .collect(java.util.stream.Collectors.toList());

            List<QuizResult> allResults = quizResultService.selectALL().stream()
                    .filter(r -> coachPatientIds.contains(r.getUserId()))
                    .collect(java.util.stream.Collectors.toList());

            Map<Integer, List<QuizResult>> byPatient = new LinkedHashMap<>();
            for (QuizResult r : allResults)
                byPatient.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);

            if (lblCount != null) lblCount.setText(byPatient.size() + " patient(s)");

            if (byPatient.isEmpty()) {
                Label empty = new Label("Aucun patient inscrit à vos formations.");
                empty.setStyle("-fx-font-size:15px;-fx-text-fill:#A0AEC0;-fx-padding:30;");
                patientsList.getChildren().add(empty);
                return;
            }

            // ── Try to load real patient names ──
            serviceUser us = new serviceUser();
            Map<Integer, User> usersMap = new HashMap<>();
            try {
                for (User u : us.selectALL())
                    usersMap.put(u.getId_user(), u);
            } catch (Exception ignored) {}

            for (Map.Entry<Integer, List<QuizResult>> entry : byPatient.entrySet()) {
                int pid = entry.getKey();
                List<QuizResult> results = entry.getValue();
                long passed = results.stream().filter(QuizResult::isPassed).count();
                double avg = results.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);
                long inscrits = allPartic.stream().filter(p -> p.getUserId() == pid).count();

                // Real name if available
                User patient = usersMap.get(pid);
                String displayName = (patient != null)
                        ? patient.getPrenom() + " " + patient.getNom()
                        : "Patient #" + pid;

                HBox card = new HBox(15);
                card.setPadding(new Insets(14));
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

                Circle avatar = new Circle(22);
                avatar.setFill(Color.web(avg >= 70 ? "#00b894" : "#E8956D"));
                String initials = (patient != null)
                        ? patient.getPrenom().substring(0, 1).toUpperCase()
                        : "P";
                Label avLbl = new Label(initials);
                avLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;");
                StackPane avPane = new StackPane(avatar, avLbl);

                VBox info = new VBox(3);
                Label pLbl = new Label(displayName);
                pLbl.setStyle("-fx-font-weight:bold;-fx-font-size:14px;-fx-text-fill:#2d3436;");
                Label sLbl = new Label(inscrits + " formation(s) | "
                        + results.size() + " quiz | " + passed + " réussi(s)");
                sLbl.setStyle("-fx-text-fill:#636e72;-fx-font-size:12px;");
                info.getChildren().addAll(pLbl, sLbl);

                HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);

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