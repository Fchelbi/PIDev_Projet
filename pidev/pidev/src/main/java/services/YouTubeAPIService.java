package services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * YouTubeAPIService — Recherche de vidéos YouTube
 *
 * API utilisée : YouTube Data API v3
 * Endpoint : https://www.googleapis.com/youtube/v3/search
 *
 * CONTRÔLE DE SAISIE :
 *   - query ne doit pas être vide ni trop courte (min 2 caractères)
 *   - maxResults entre 1 et 20
 *
 * FIX PARSING :
 *   Avant : parsing fragile avec split("\"videoId\"") → cassé si YouTube
 *           change l'ordre des champs JSON.
 *   Après : extraction par champ nommé, robuste au reformatage.
 */
public class YouTubeAPIService {

    private static final String API_KEY    = "AIzaSyDf3mR2KnGmiyuf39uYW9PqOGRaaTflHAY";
    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Recherche des vidéos YouTube.
     *
     * Contrôle de saisie :
     *  - query null ou vide → IllegalArgumentException
     *  - query < 2 caractères → IllegalArgumentException
     *  - maxResults hors [1, 20] → corrigé silencieusement
     */
    public List<YouTubeVideo> searchVideos(String query, int maxResults) throws Exception {

        // ── Validation des entrées ──────────────────────────────────────────
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Le terme de recherche ne peut pas être vide.");
        }
        if (query.trim().length() < 2) {
            throw new IllegalArgumentException("Le terme de recherche doit contenir au moins 2 caractères.");
        }
        maxResults = Math.max(1, Math.min(20, maxResults));

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String urlStr  = SEARCH_URL
                + "?part=snippet"
                + "&q=" + encoded
                + "&type=video"
                + "&maxResults=" + maxResults
                + "&relevanceLanguage=fr"
                + "&key=" + API_KEY;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int code = conn.getResponseCode();
        if (code != 200) {
            // Lire le message d'erreur
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) sb.append(line);
            throw new Exception("YouTube API erreur HTTP " + code + " — " + parseError(sb.toString()));
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line).append('\n');
        reader.close();
        conn.disconnect();

        return parseResults(response.toString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PARSING (robuste)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * FIX : Au lieu de split("\"videoId\"") qui casse si YouTube réorganise
     * les champs, on découpe par "items" puis lit chaque item séparément.
     */
    private List<YouTubeVideo> parseResults(String json) {
        List<YouTubeVideo> results = new ArrayList<>();

        // Trouver le tableau "items"
        int itemsIdx = json.indexOf("\"items\"");
        if (itemsIdx == -1) return results;

        int arrStart = json.indexOf('[', itemsIdx);
        if (arrStart == -1) return results;

        // Découper chaque objet item { ... }
        int depth = 0, start = -1;
        for (int i = arrStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    String block = json.substring(start, i + 1);
                    YouTubeVideo v = parseItem(block);
                    if (v != null) results.add(v);
                    start = -1;
                }
            }
        }
        return results;
    }

    private YouTubeVideo parseItem(String block) {
        try {
            // videoId dans "videoId":{"kind":"...","videoId":"XXXX"}
            String videoId = extractNestedField(block, "videoId", "videoId");
            if (videoId == null || videoId.length() < 5) return null;

            String title   = extractField(block, "title");
            String channel = extractField(block, "channelTitle");
            String desc    = extractField(block, "description");

            YouTubeVideo v = new YouTubeVideo();
            v.videoId      = videoId;
            v.title        = title   != null ? unescapeJson(title)   : "(Sans titre)";
            v.channelTitle = channel != null ? unescapeJson(channel) : "";
            v.description  = desc    != null ? unescapeJson(desc)    : "";
            v.youtubeUrl   = "https://www.youtube.com/watch?v=" + videoId;
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS JSON (sans dépendance externe)
    // ══════════════════════════════════════════════════════════════════════════

    /** Extrait la valeur d'un champ JSON simple "key":"value" */
    private String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        idx += search.length();
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':')) idx++;
        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == '\\' && idx + 1 < json.length()) {
                idx++;
                char next = json.charAt(idx);
                if (next == 'n') sb.append(' ');
                else if (next == '"') sb.append('"');
                else if (next == '\\') sb.append('\\');
                else sb.append(next);
            } else {
                sb.append(json.charAt(idx));
            }
            idx++;
        }
        return sb.toString();
    }

    /**
     * Extrait un champ dans un sous-objet : ex chercher "videoId" dans "videoId":{"videoId":"..."}
     * On cherche d'abord le bloc de l'objet parent, puis le champ enfant.
     */
    private String extractNestedField(String json, String parentKey, String childKey) {
        String search = "\"" + parentKey + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        // Trouver le sous-objet { ... }
        int objStart = json.indexOf('{', idx + search.length());
        if (objStart == -1) return null;
        int objEnd = findClosingBrace(json, objStart);
        if (objEnd == -1) return null;
        String sub = json.substring(objStart, objEnd + 1);
        return extractField(sub, childKey);
    }

    private int findClosingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private String unescapeJson(String s) {
        return s.replace("\\u0026", "&")
                .replace("\\u0027", "'")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .trim();
    }

    private String parseError(String errorJson) {
        String msg = extractField(errorJson, "message");
        return msg != null ? msg : errorJson.substring(0, Math.min(100, errorJson.length()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CLASSE RÉSULTAT
    // ══════════════════════════════════════════════════════════════════════════

    public static class YouTubeVideo {
        public String videoId;
        public String title;
        public String description;
        public String channelTitle;
        public String youtubeUrl;

        @Override
        public String toString() { return title + " — " + channelTitle; }
    }
}