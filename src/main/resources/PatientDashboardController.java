package tn.esprit.projet.gui.patient;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class PatientDashboardController {

    @FXML
    private StackPane centerPane;

    // ✅ afficher liste psy
    @FXML
    private void showListePsy() {
        loadPage("PatientConsultation.fxml");
    }

    // ✅ afficher mes consultations
    @FXML
    private void showMesConsultations() {
        loadPage("PatientMesConsultations.fxml");
    }

    // 🔥 navigation interne dashboard
    private void loadPage(String fileName) {
        try {
            // ✅ puisque ton FXML est dans resources root
            Parent page = FXMLLoader.load(
                    getClass().getResource("/" + fileName)
            );

            centerPane.getChildren().setAll(page);

        } catch (Exception e) {
            System.err.println("Erreur chargement: " + fileName);
            e.printStackTrace();
        }
    }

    // ✅ au démarrage afficher liste psy
    @FXML
    public void initialize() {
        showListePsy();
    }
}
