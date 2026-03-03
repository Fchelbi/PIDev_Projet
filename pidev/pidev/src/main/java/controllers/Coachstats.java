package controllers;

import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import services.Servicerapport;
import services.serviceUser;
import utils.LightDialog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Coachstats {

    @FXML private Label       lblTotalPatients, lblMoyHumeur, lblMoyEmoji;
    @FXML private Label       lblPatientsBien, lblPatientsMoyen, lblPatientsBas;
    @FXML private ProgressBar pbBien, pbMoyen, pbBas;
    @FXML private Label       lblPctBien, lblPctMoyen, lblPctBas;
    @FXML private Label       lblInsight;
    @FXML private WebView     wvChart;
    @FXML private ListView<String> lvPatients;
    @FXML private ComboBox<String> cbFilter;
    @FXML private TextField        tfSearch;

    private User        currentCoach;
    private List<User>   allPatients = new ArrayList<>();
    private List<Double> allScores   = new ArrayList<>();

    private final serviceUser    us = new serviceUser();
    private final Servicerapport rs = new Servicerapport();

    // initialize() — NE PAS accéder aux champs ici, ils peuvent être null
    // Tout est initialisé dans setCoach() après injection FXML complète
    @FXML
    void initialize() {
        // volontairement vide — setup dans setCoach()
    }

    public void setCoach(User coach) {
        this.currentCoach = coach;

        // Setup cbFilter ici — FXML déjà injecté à ce stade
        if (cbFilter != null) {
            cbFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "Bien (≥7)", "Moyen (4-6)", "Bas (<4)"));
            cbFilter.getSelectionModel().selectFirst();
            cbFilter.setOnAction(e -> applyFilter());
        }
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((o, old, v) -> applyFilter());
        }

        loadData();
    }

    @FXML
    void handleRefresh() {
        loadData();
    }

    // ─── Charger données réelles ──────────────────────────────
    private void loadData() {
        Thread t = new Thread(() -> {
            try {
                List<User> patients = us.selectALL().stream()
                        .filter(u -> u.getRole().equalsIgnoreCase("PATIENT"))
                        .toList();

                List<User>   loaded = new ArrayList<>();
                List<Double> scores = new ArrayList<>();

                for (User p : patients) {
                    double score = rs.getLatestScoreForPatient(p.getId_user());
                    loaded.add(p);
                    scores.add(score); // -1 = pas de rapport
                }

                Platform.runLater(() -> {
                    allPatients = loaded;
                    allScores   = scores;
                    updateStats();
                    applyFilter();
                    loadGoogleCharts();
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        LightDialog.showError("Erreur", "Impossible de charger les données : " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─── Mise à jour stats ────────────────────────────────────
    private void updateStats() {
        int total = allPatients.size();
        set(lblTotalPatients, String.valueOf(total));

        List<Double> valid = allScores.stream().filter(s -> s >= 0).toList();
        int bien = 0, moyen = 0, bas = 0;
        double sum = 0;
        for (double s : valid) {
            sum += s;
            if (s >= 7) bien++;
            else if (s >= 4) moyen++;
            else bas++;
        }
        int withReport = valid.size();
        double moy = withReport > 0 ? sum / withReport : 0;

        set(lblMoyHumeur, withReport > 0 ? String.format("%.1f", moy) : "—");
        set(lblMoyEmoji,  moy >= 7 ? "😄" : moy >= 4 ? "😐" : "😢");
        set(lblPatientsBien,  String.valueOf(bien));
        set(lblPatientsMoyen, String.valueOf(moyen));
        set(lblPatientsBas,   String.valueOf(bas));

        double denom = withReport > 0 ? withReport : 1;
        setProgress(pbBien,  bien  / denom);
        setProgress(pbMoyen, moyen / denom);
        setProgress(pbBas,   bas   / denom);
        set(lblPctBien,  String.format("%.0f%%", bien  / denom * 100));
        set(lblPctMoyen, String.format("%.0f%%", moyen / denom * 100));
        set(lblPctBas,   String.format("%.0f%%", bas   / denom * 100));

        String insight;
        if (withReport == 0)      insight = "ℹ️  Aucun rapport généré pour l'instant.";
        else if (bas > 0)         insight = "⚠️  " + bas + " patient(s) en bas moral — consultation prioritaire.";
        else if (bien > total / 2) insight = "✅  La majorité de vos patients est en bonne forme !";
        else                      insight = "📊  Suivi régulier conseillé pour optimiser les progrès.";
        set(lblInsight, insight);
    }

    // ─── Filtrer liste patients ───────────────────────────────
    private void applyFilter() {
        String filter = (cbFilter != null && cbFilter.getValue() != null)
                ? cbFilter.getValue() : "Tous";
        String search = (tfSearch != null) ? tfSearch.getText().toLowerCase() : "";

        ObservableList<String> items = FXCollections.observableArrayList();
        for (int i = 0; i < allPatients.size(); i++) {
            User p = allPatients.get(i);
            double score = allScores.get(i);
            String name = p.getPrenom() + " " + p.getNom();

            boolean passFilter = switch (filter) {
                case "Bien (≥7)"   -> score >= 7;
                case "Moyen (4-6)" -> score >= 4 && score < 7;
                case "Bas (<4)"    -> score >= 0 && score < 4;
                default            -> true;
            };
            boolean passSearch = search.isEmpty() || name.toLowerCase().contains(search);

            if (passFilter && passSearch) {
                String emoji = score < 0 ? "❔" : score >= 7 ? "😄" : score >= 4 ? "😐" : "😢";
                String label = score < 0 ? "(Aucun rapport)" : String.format("%.1f/10", score);
                String cat   = score < 0 ? "—" : score >= 7 ? "Bien" : score >= 4 ? "Moyen" : "Bas";
                items.add(emoji + "  " + name + "  —  humeur: " + label + "  [" + cat + "]");
            }
        }
        if (lvPatients != null) lvPatients.setItems(items);
    }

    // ─── Google Charts ────────────────────────────────────────
    private void loadGoogleCharts() {
        if (wvChart == null) return;

        List<Double> valid = allScores.stream().filter(s -> s >= 0).toList();
        long bien  = valid.stream().filter(s -> s >= 7).count();
        long moyen = valid.stream().filter(s -> s >= 4 && s < 7).count();
        long bas   = valid.stream().filter(s -> s < 4).count();

        StringBuilder barRows = new StringBuilder();
        for (int i = 0; i < allPatients.size(); i++) {
            double score = allScores.get(i);
            if (score < 0) continue;
            String name  = allPatients.get(i).getPrenom();
            String color = score >= 7 ? "#52B788" : score >= 4 ? "#F5C87A" : "#E8956D";
            barRows.append(String.format("['%s', %.1f, '%s'],\n", name, score, color));
        }

        // Si aucune donnée: afficher message
        if (valid.isEmpty()) {
            wvChart.getEngine().loadContent("""
                <html><body style="display:flex;align-items:center;justify-content:center;
                height:240px;font-family:sans-serif;color:#A0AEC0;font-size:13px;">
                <p>📊 Aucune donnée — générez des rapports d'abord</p></body></html>
            """);
            return;
        }

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
            <script src="https://www.gstatic.com/charts/loader.js"></script>
            <script>
              google.charts.load('current', {packages:['corechart']});
              google.charts.setOnLoadCallback(drawAll);
              function drawAll() { drawPie(); drawBar(); }

              function drawPie() {
                var data = google.visualization.arrayToDataTable([
                  ['État','Patients'],
                  ['En forme (≥7)',%d],
                  ['Moyen (4-6)',%d],
                  ['Bas (<4)',%d]
                ]);
                new google.visualization.PieChart(
                  document.getElementById('pie')
                ).draw(data, {
                  pieHole:0.4,
                  colors:['#52B788','#F5C87A','#E8956D'],
                  backgroundColor:'transparent',
                  titleTextStyle:{color:'#2D3748',fontSize:12,bold:true},
                  legend:{position:'bottom',textStyle:{color:'#718096',fontSize:10}},
                  chartArea:{width:'88%%',height:'75%%'},
                  title:'Répartition'
                });
              }

              function drawBar() {
                var data = new google.visualization.DataTable();
                data.addColumn('string','Patient');
                data.addColumn('number','Score');
                data.addColumn({type:'string',role:'style'});
                data.addRows([%s]);
                new google.visualization.BarChart(
                  document.getElementById('bar')
                ).draw(data, {
                  legend:{position:'none'},
                  backgroundColor:'transparent',
                  hAxis:{minValue:0,maxValue:10,textStyle:{color:'#718096',fontSize:10}},
                  vAxis:{textStyle:{color:'#718096',fontSize:10}},
                  chartArea:{width:'72%%',height:'75%%'},
                  bar:{groupWidth:'60%%'},
                  title:'Score par patient',
                  titleTextStyle:{color:'#2D3748',fontSize:12,bold:true}
                });
              }
            </script>
            <style>
              body{margin:0;padding:4px;background:transparent;}
              .row{display:flex;gap:10px;height:240px;}
              .box{flex:1;background:white;border-radius:10px;overflow:hidden;
                   box-shadow:0 1px 4px rgba(0,0,0,0.06);}
            </style>
            </head>
            <body>
              <div class="row">
                <div class="box" id="pie"></div>
                <div class="box" id="bar"></div>
              </div>
            </body>
            </html>
            """.formatted(bien, moyen, bas, barRows);

        wvChart.getEngine().loadContent(html);
    }

    // ─── Helpers null-safe ────────────────────────────────────
    private void set(Label lbl, String text)  { if (lbl  != null) lbl.setText(text); }
    private void setProgress(ProgressBar pb, double v) { if (pb != null) pb.setProgress(v); }
}