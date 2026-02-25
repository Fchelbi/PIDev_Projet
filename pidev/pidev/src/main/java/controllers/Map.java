package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.List;

public class Map {

    @FXML private TextField tfSearch;
    @FXML private ListView<Place> listPlaces;
    @FXML private Label lblResultCount;
    @FXML private Label lblSelectedName, lblSelectedAddress, lblSelectedRating, lblSelectedStatus;
    @FXML private VBox detailCard;

    private List<Place> currentPlaces;

    private double currentLat = 36.8065;
    private double currentLng = 10.1815;

    @FXML
    void initialize() {
        loadPlaces();

        listPlaces.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                showPlaceDetail(selected);
            }
        });

        listPlaces.setCellFactory(lv -> new ListCell<Place>() {
            @Override
            protected void updateItem(Place place, boolean empty) {
                super.updateItem(place, empty);
                if (empty || place == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(4);
                    box.setStyle("-fx-padding: 12; -fx-background-color: white; " +
                            "-fx-background-radius: 10; -fx-border-color: #E2E8F0; " +
                            "-fx-border-radius: 10; -fx-border-width: 1;");

                    Label name = new Label(place.getName());
                    name.setStyle("-fx-font-weight: 700; -fx-text-fill: #4A5568; -fx-font-size: 13px;");

                    Label address = new Label("📍 " + place.getAddress());
                    address.setStyle("-fx-text-fill: #718096; -fx-font-size: 11px;");

                    HBox ratingBox = new HBox(10);
                    Label rating = new Label("⭐ " + String.format("%.1f", place.getRating()));
                    rating.setStyle("-fx-text-fill: #D4889A; -fx-font-size: 12px; -fx-font-weight: 600;");

                    Label status = new Label(place.isOpen() ? "✅ Ouvert" : "❌ Fermé");
                    status.setStyle("-fx-text-fill: " + (place.isOpen() ? "#5FAD6F" : "#E8B4B8") +
                            "; -fx-font-size: 11px; -fx-font-weight: 600;");

                    ratingBox.getChildren().addAll(rating, status);
                    box.getChildren().addAll(name, address, ratingBox);
                    setGraphic(box);
                    setStyle("-fx-padding: 3; -fx-background-color: transparent;");
                }
            }
        });
    }

    private void loadPlaces() {
        currentPlaces = MapService.getDemoPlaces(currentLat, currentLng);
        updatePlacesList();
    }

    @FXML
    void handleSearch() {
        String query = tfSearch.getText().trim();
        if (query.isEmpty()) {
            LightDialog.showError("Recherche", "Entrez une ville ou adresse!");
            return;
        }

        try {
            currentPlaces = MapService.searchByCity(query);
            if (currentPlaces.isEmpty()) {
                currentPlaces = MapService.getDemoPlaces(currentLat, currentLng);
            }
            updatePlacesList();
        } catch (Exception e) {
            currentPlaces = MapService.getDemoPlaces(currentLat, currentLng);
            updatePlacesList();
        }
    }

    @FXML
    void openMapInBrowser() {
        try {
            String html = MapService.generateDemoMapHTML(currentLat, currentLng, currentPlaces);

            File tempFile = new File("echocare_map.html");
            FileWriter writer = new FileWriter(tempFile);
            writer.write(html);
            writer.close();

            Desktop.getDesktop().browse(tempFile.toURI());
            System.out.println("✅ Carte ouverte dans le navigateur");
        } catch (IOException e) {
            LightDialog.showError("Erreur", "Impossible d'ouvrir la carte.");
            e.printStackTrace();
        }
    }

    @FXML
    void openInGoogleMaps() {
        Place selected = listPlaces.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LightDialog.showError("Erreur", "Sélectionnez un lieu d'abord!");
            return;
        }

        try {
            String url = "https://www.google.com/maps/search/?api=1&query=" +
                    selected.getLat() + "," + selected.getLng();
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            LightDialog.showError("Erreur", "Impossible d'ouvrir Google Maps.");
        }
    }

    @FXML
    void filterOpen() {
        if (currentPlaces == null) return;
        List<Place> filtered = currentPlaces.stream()
                .filter(Place::isOpen)
                .toList();
        ObservableList<Place> items = FXCollections.observableArrayList(filtered);
        listPlaces.setItems(items);
        lblResultCount.setText(filtered.size() + " ouvert(s)");
    }

    @FXML
    void filterTopRated() {
        if (currentPlaces == null) return;
        List<Place> sorted = currentPlaces.stream()
                .sorted((a, b) -> Double.compare(b.getRating(), a.getRating()))
                .toList();
        ObservableList<Place> items = FXCollections.observableArrayList(sorted);
        listPlaces.setItems(items);
        lblResultCount.setText(sorted.size() + " résultat(s)");
    }

    @FXML
    void resetFilters() {
        tfSearch.clear();
        loadPlaces();
    }

    private void updatePlacesList() {
        ObservableList<Place> items = FXCollections.observableArrayList(currentPlaces);
        listPlaces.setItems(items);
        lblResultCount.setText(currentPlaces.size() + " psychologue(s) trouvé(s)");
    }

    private void showPlaceDetail(Place place) {
        detailCard.setVisible(true);
        detailCard.setManaged(true);
        lblSelectedName.setText(place.getName());
        lblSelectedAddress.setText("📍 " + place.getAddress());
        lblSelectedRating.setText("⭐ " + String.format("%.1f", place.getRating()) + " / 5");
        lblSelectedStatus.setText(place.isOpen() ? "✅ Ouvert maintenant" : "❌ Fermé");
        lblSelectedStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " +
                (place.isOpen() ? "#5FAD6F" : "#E8B4B8") + ";");
    }
}