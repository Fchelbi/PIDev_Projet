package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapService {

    // ✅ Remplace par ta clé Google Maps API
    private static final String API_KEY = "YOUR_GOOGLE_MAPS_API_KEY";

    /**
     * Classe pour représenter un lieu (psychologue/cabinet)
     */
    public static class Place {
        private String name;
        private String address;
        private double lat;
        private double lng;
        private double rating;
        private String phone;
        private boolean isOpen;
        private String placeId;

        public Place(String name, String address, double lat, double lng,
                     double rating, boolean isOpen, String placeId) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.rating = rating;
            this.isOpen = isOpen;
            this.placeId = placeId;
        }

        // Getters
        public String getName() { return name; }
        public String getAddress() { return address; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public double getRating() { return rating; }
        public String getPhone() { return phone; }
        public boolean isOpen() { return isOpen; }
        public String getPlaceId() { return placeId; }
        public void setPhone(String p) { this.phone = p; }

        @Override
        public String toString() {
            return name + " (" + rating + "⭐) - " + address;
        }
    }

    /**
     * Chercher psychologues près d'une position
     */
    public static List<Place> searchNearbyPsychologists(double lat, double lng, int radiusMeters) {
        List<Place> places = new ArrayList<>();

        try {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location=" + lat + "," + lng +
                    "&radius=" + radiusMeters +
                    "&keyword=" + URLEncoder.encode("psychologue psychiatre thérapeute", StandardCharsets.UTF_8) +
                    "&type=health" +
                    "&language=fr" +
                    "&key=" + API_KEY;

            String response = makeGetRequest(url);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray results = json.getAsJsonArray("results");

            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    JsonObject place = results.get(i).getAsJsonObject();

                    String name = place.get("name").getAsString();
                    String address = place.has("vicinity") ?
                            place.get("vicinity").getAsString() : "Adresse non disponible";

                    JsonObject location = place.getAsJsonObject("geometry")
                            .getAsJsonObject("location");
                    double pLat = location.get("lat").getAsDouble();
                    double pLng = location.get("lng").getAsDouble();

                    double rating = place.has("rating") ?
                            place.get("rating").getAsDouble() : 0;

                    boolean isOpen = false;
                    if (place.has("opening_hours")) {
                        isOpen = place.getAsJsonObject("opening_hours")
                                .get("open_now").getAsBoolean();
                    }

                    String placeId = place.has("place_id") ?
                            place.get("place_id").getAsString() : "";

                    places.add(new Place(name, address, pLat, pLng, rating, isOpen, placeId));
                }
            }

            System.out.println("✅ Trouvé " + places.size() + " psychologues");

        } catch (Exception e) {
            System.err.println("❌ Erreur recherche: " + e.getMessage());
            // Ajouter données de démonstration si API échoue
            places = getDemoPlaces(lat, lng);
        }

        return places;
    }

    /**
     * Chercher par ville/adresse
     */
    public static List<Place> searchByCity(String city) {
        List<Place> places = new ArrayList<>();

        try {
            // D'abord geocoder la ville
            String geocodeUrl = "https://maps.googleapis.com/maps/api/geocode/json" +
                    "?address=" + URLEncoder.encode(city, StandardCharsets.UTF_8) +
                    "&key=" + API_KEY;

            String response = makeGetRequest(geocodeUrl);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray results = json.getAsJsonArray("results");

            if (results != null && results.size() > 0) {
                JsonObject location = results.get(0).getAsJsonObject()
                        .getAsJsonObject("geometry")
                        .getAsJsonObject("location");

                double lat = location.get("lat").getAsDouble();
                double lng = location.get("lng").getAsDouble();

                places = searchNearbyPsychologists(lat, lng, 10000);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur geocoding: " + e.getMessage());
            places = getDemoPlaces(36.8065, 10.1815); // Tunis par défaut
        }

        return places;
    }

    /**
     * Générer HTML pour la carte Google Maps
     */
    public static String generateMapHTML(double centerLat, double centerLng,
                                         List<Place> places) {
        StringBuilder markers = new StringBuilder();

        for (int i = 0; i < places.size(); i++) {
            Place p = places.get(i);
            markers.append(String.format(
                    "var marker%d = new google.maps.Marker({" +
                            "position: {lat: %f, lng: %f}," +
                            "map: map," +
                            "title: '%s'," +
                            "icon: {" +
                            "  path: google.maps.SymbolPath.CIRCLE," +
                            "  scale: 12," +
                            "  fillColor: '%s'," +
                            "  fillOpacity: 0.9," +
                            "  strokeColor: '#ffffff'," +
                            "  strokeWeight: 3" +
                            "}" +
                            "});" +
                            "var info%d = new google.maps.InfoWindow({" +
                            "content: '<div style=\"font-family:Arial;padding:8px;min-width:200px\">" +
                            "<h3 style=\"color:#7C6BC4;margin:0 0 8px 0;font-size:15px\">%s</h3>" +
                            "<p style=\"color:#666;margin:4px 0;font-size:12px\">📍 %s</p>" +
                            "<p style=\"color:#E8B4C8;margin:4px 0;font-size:13px\">⭐ %s/5</p>" +
                            "<p style=\"color:%s;margin:4px 0;font-size:12px;font-weight:bold\">%s</p>" +
                            "</div>'" +
                            "});" +
                            "marker%d.addListener('click', function() { info%d.open(map, marker%d); });",
                    i, p.getLat(), p.getLng(),
                    p.getName().replace("'", "\\'"),
                    p.isOpen() ? "#A8D5BA" : "#E8B4B8",
                    i,
                    p.getName().replace("'", "\\'"),
                    p.getAddress().replace("'", "\\'"),
                    String.format("%.1f", p.getRating()),
                    p.isOpen() ? "#5FAD6F" : "#E8B4B8",
                    p.isOpen() ? "✅ Ouvert" : "❌ Fermé",
                    i, i, i
            ));
        }

        return "<!DOCTYPE html>" +
                "<html><head>" +
                "<meta charset='utf-8'>" +
                "<style>" +
                "body{margin:0;font-family:Arial}" +
                "#map{width:100%;height:100vh}" +
                "</style>" +
                "</head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "function initMap(){" +
                "var map=new google.maps.Map(document.getElementById('map'),{" +
                "center:{lat:" + centerLat + ",lng:" + centerLng + "}," +
                "zoom:13," +
                "styles:[" +
                "{featureType:'all',elementType:'geometry',stylers:[{color:'#F5F0FA'}]}," +
                "{featureType:'water',elementType:'geometry',stylers:[{color:'#C4A8E0'}]}," +
                "{featureType:'road',elementType:'geometry',stylers:[{color:'#FFFFFF'}]}," +
                "{featureType:'poi',elementType:'labels',stylers:[{visibility:'off'}]}," +
                "{featureType:'road',elementType:'labels.text.fill',stylers:[{color:'#8B7FA3'}]}" +
                "]" +
                "});" +
                // Marqueur position user
                "new google.maps.Marker({" +
                "position:{lat:" + centerLat + ",lng:" + centerLng + "}," +
                "map:map," +
                "title:'Votre position'," +
                "icon:{path:google.maps.SymbolPath.CIRCLE,scale:10," +
                "fillColor:'#7C6BC4',fillOpacity:1,strokeColor:'#fff',strokeWeight:3}" +
                "});" +
                markers.toString() +
                "}" +
                "</script>" +
                "<script src='https://maps.googleapis.com/maps/api/js?key=" + API_KEY +
                "&callback=initMap' async defer></script>" +
                "</body></html>";
    }

    /**
     * Données de démonstration (si pas d'API key)
     */
    public static List<Place> getDemoPlaces(double baseLat, double baseLng) {
        List<Place> demos = new ArrayList<>();
        // ✅ Adresses réelles à Tunis avec coordonnées GPS précises
        demos.add(new Place("Dr. Sarah Ben Ali - Psychologue Clinicienne",
                "45 Avenue Habib Bourguiba, Tunis 1001",
                36.8188, 10.1657, 4.8, true, "demo1"));
        demos.add(new Place("Cabinet Sérénité - Psychothérapie",
                "12 Rue de Marseille, Lafayette, Tunis 1002",
                36.8131, 10.1775, 4.6, true, "demo2"));
        demos.add(new Place("Dr. Mohamed Trabelsi - Psychiatre",
                "8 Rue du Lac Biwa, Les Berges du Lac, Tunis 1053",
                36.8378, 10.2316, 4.9, false, "demo3"));
        demos.add(new Place("Centre Bien-Être Mental",
                "23 Avenue de Paris, El Menzah, Tunis 1004",
                36.8447, 10.1762, 4.3, true, "demo4"));
        demos.add(new Place("Dr. Amira Gharbi - Psychothérapeute",
                "15 Rue Alain Savary, Cité El Khadra, Tunis 1003",
                36.8264, 10.1931, 4.7, true, "demo5"));
        demos.add(new Place("Cabinet Harmony - Psychologie",
                "Centre Urbain Nord Bloc B, Tunis 1082",
                36.8601, 10.1948, 4.5, false, "demo6"));
        demos.add(new Place("Dr. Karim Mansour - Psychologue",
                "34 Avenue Kheireddine Pacha, Tunis 1002",
                36.8168, 10.1821, 4.4, true, "demo7"));
        demos.add(new Place("Cabinet Équilibre - Thérapie Cognitive",
                "10 Rue de Hollande, Montplaisir, Tunis 1073",
                36.8089, 10.1856, 4.8, true, "demo8"));
        System.out.println("📍 Mode démonstration: " + demos.size() + " lieux chargés (Tunis)");
        return demos;
    }

    /**
     * Générer carte HTML mode DEMO (sans API key)
     */
    public static String generateDemoMapHTML(double centerLat, double centerLng,
                                             List<Place> places) {
        StringBuilder placesJS = new StringBuilder("[");
        for (int i = 0; i < places.size(); i++) {
            Place p = places.get(i);
            if (i > 0) placesJS.append(",");
            placesJS.append(String.format(
                    "{name:'%s',address:'%s',lat:%f,lng:%f,rating:%.1f,open:%b}",
                    p.getName().replace("'", "\\'"),
                    p.getAddress().replace("'", "\\'"),
                    p.getLat(), p.getLng(), p.getRating(), p.isOpen()));
        }
        placesJS.append("]");

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='utf-8'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "body{margin:0;font-family:'Segoe UI',Arial,sans-serif}" +
                "#map{width:100%;height:100vh}" +
                ".custom-popup h3{color:#7C6BC4;margin:0 0 8px;font-size:14px}" +
                ".custom-popup p{margin:3px 0;font-size:12px;color:#666}" +
                ".status-open{color:#5FAD6F;font-weight:bold}" +
                ".status-closed{color:#E8B4B8;font-weight:bold}" +
                "</style></head><body>" +
                "<div id='map'></div>" +
                "<script>" +
                "var map=L.map('map').setView([" + centerLat + "," + centerLng + "],14);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{" +
                "attribution:'EchoCare Map'}).addTo(map);" +

                // Marqueur user
                "var userIcon=L.divIcon({html:'<div style=\"background:#7C6BC4;width:20px;height:20px;" +
                "border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(124,107,196,0.5)\"></div>'," +
                "className:'',iconSize:[20,20]});" +
                "L.marker([" + centerLat + "," + centerLng + "],{icon:userIcon})" +
                ".addTo(map).bindPopup('<b style=\"color:#7C6BC4\">📍 Votre position</b>');" +

                // Marqueurs places
                "var places=" + placesJS.toString() + ";" +
                "places.forEach(function(p){" +
                "var color=p.open?'#A8D5BA':'#E8B4B8';" +
                "var icon=L.divIcon({html:'<div style=\"background:'+color+';width:16px;height:16px;" +
                "border-radius:50%;border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.2)\"></div>'," +
                "className:'',iconSize:[16,16]});" +
                "L.marker([p.lat,p.lng],{icon:icon}).addTo(map)" +
                ".bindPopup('<div class=\"custom-popup\">" +
                "<h3>'+p.name+'</h3>" +
                "<p>📍 '+p.address+'</p>" +
                "<p>⭐ '+p.rating+'/5</p>" +
                "<p class=\"'+(p.open?'status-open':'status-closed')+'\">" +
                "'+(p.open?'✅ Ouvert maintenant':'❌ Fermé')+'</p></div>');" +
                "});" +
                "</script></body></html>";
    }

    /**
     * HTTP GET Request
     */
    private static String makeGetRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
}