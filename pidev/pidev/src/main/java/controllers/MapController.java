package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import services.MapService;
import services.MapService.Place;
import utils.LightDialog;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MapController {

    @FXML private TextField tfSearch;
    @FXML private ListView<Place> listPlaces;
    @FXML private Label lblResultCount;
    @FXML private Label lblSelectedName, lblSelectedAddress, lblSelectedRating, lblSelectedStatus;
    @FXML private VBox detailCard;

    private List<Place> currentPlaces;
    private double currentLat = 36.8065;
    private double currentLng = 10.1815;

    private static final String CARD_NORMAL =
            "-fx-padding: 12; -fx-background-color: #FDFAF6; " +
                    "-fx-background-radius: 10; -fx-border-color: #D5E3F5; " +
                    "-fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String CARD_HOVER =
            "-fx-padding: 12; -fx-background-color: #FFF3EC; " +
                    "-fx-background-radius: 10; -fx-border-color: #E8956D; " +
                    "-fx-border-radius: 10; -fx-border-width: 2; -fx-cursor: hand;";

    @FXML
    void initialize() {
        loadPlaces();
        listPlaces.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                showPlaceDetail(selected);
                // ✅ Click → ouvre Google Maps directement
                openGoogleMapsForPlace(selected);
            }
        });

        listPlaces.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Place place, boolean empty) {
                super.updateItem(place, empty);
                if (empty || place == null) {
                    setText(null); setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }
                VBox box = new VBox(5);
                box.setStyle(CARD_NORMAL);

                Label name = new Label("📍 " + place.getName());
                name.setStyle("-fx-font-weight: 700; -fx-text-fill: #3D5A8A; " +
                        "-fx-font-size: 13px; -fx-underline: true;");
                name.setTooltip(new Tooltip("Cliquer → ouvre Google Maps 🗺️"));

                Label address = new Label(place.getAddress());
                address.setStyle("-fx-text-fill: #5A6A8A; -fx-font-size: 11px;");
                address.setWrapText(true);

                HBox bottom = new HBox(10);
                Label rating = new Label("⭐ " + String.format("%.1f", place.getRating()) + "/5");
                rating.setStyle("-fx-text-fill: #E8956D; -fx-font-size: 12px; -fx-font-weight: 600;");

                Label status = new Label(place.isOpen() ? "✅ Ouvert" : "❌ Fermé");
                status.setStyle("-fx-text-fill: " + (place.isOpen() ? "#4A8A5A" : "#C07050") +
                        "; -fx-font-size: 11px; -fx-font-weight: 600;");

                Label mapsTag = new Label("🗺️ Voir sur Maps");
                mapsTag.setStyle("-fx-text-fill: #7B9ED9; -fx-font-size: 10px; -fx-font-weight: 600;" +
                        "-fx-background-color: #EEF4FF; -fx-background-radius: 6; -fx-padding: 2 8;");

                bottom.getChildren().addAll(rating, status, mapsTag);
                box.getChildren().addAll(name, address, bottom);

                box.setOnMouseEntered(e -> box.setStyle(CARD_HOVER));
                box.setOnMouseExited(e -> box.setStyle(CARD_NORMAL));

                setGraphic(box);
                setStyle("-fx-padding: 4; -fx-background-color: transparent;");
            }
        });
    }

    /**
     * ✅ Ouvre Google Maps avec adresse EXACTE — localisation précise garantie
     */
    private void openGoogleMapsForPlace(Place place) {
        try {
            // Nom + adresse complète = Google Maps trouve le lieu précisément
            String query = place.getName() + " " + place.getAddress();
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // URL avec coordonnées GPS pour centrer + zoom direct sur le lieu
            String url = "https://www.google.com/maps/search/?api=1"
                    + "&query=" + encoded
                    + "&center=" + place.getLat() + "," + place.getLng()
                    + "&zoom=17";

            Desktop.getDesktop().browse(new URI(url));
            System.out.println("🗺️ Maps → " + place.getName() + " [" + place.getLat() + "," + place.getLng() + "]");

        } catch (Exception e) {
            // Fallback GPS pur si erreur encoding
            try {
                String url = "https://www.google.com/maps/@" + place.getLat() + "," + place.getLng() + ",17z";
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                LightDialog.showError("Erreur", "Impossible d'ouvrir Google Maps.");
            }
        }
    }

    private void loadPlaces() {
        currentPlaces = MapService.getDemoPlaces(currentLat, currentLng);
        updatePlacesList();
    }

    @FXML void handleSearch() {
        String query = tfSearch.getText().trim();
        if (query.isEmpty()) { LightDialog.showError("Recherche", "Entrez une ville!"); return; }
        try {
            currentPlaces = MapService.searchByCity(query);
            if (currentPlaces.isEmpty()) currentPlaces = MapService.getDemoPlaces(currentLat, currentLng);
            updatePlacesList();
        } catch (Exception e) {
            currentPlaces = MapService.getDemoPlaces(currentLat, currentLng);
            updatePlacesList();
        }
    }

    @FXML void openMapInBrowser() {
        try {
            String html = MapService.generateDemoMapHTML(currentLat, currentLng, currentPlaces);
            File f = new File("echocare_map.html");
            new FileWriter(f) {{ write(html); close(); }};
            Desktop.getDesktop().browse(f.toURI());
        } catch (IOException e) { LightDialog.showError("Erreur", "Impossible d'ouvrir la carte."); }
    }

    @FXML void openInGoogleMaps() {
        Place s = listPlaces.getSelectionModel().getSelectedItem();
        if (s == null) { LightDialog.showError("Erreur", "Sélectionnez un lieu!"); return; }
        openGoogleMapsForPlace(s);
    }
