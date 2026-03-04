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
 * YouTubeAPIService — Recherche de vidéos YouTube via YouTube Data API v3
 *
 * IMPORTANT: Replace API_KEY with your own key from Google Cloud Console:
 *   1. Go to https://console.cloud.google.com/
 *   2. Enable "YouTube Data API v3"
 *   3. Create an API Key under Credentials
 *   4. Paste it below
 *
 * The API key in the original code may be expired or quota-exceeded.
 */
public class YouTubeAPIService {

    // ⚠️ Replace this with your own valid YouTube Data API v3 key
    private static final String API_KEY    = "AIzaSyDf3mR2KnGmiyuf39uYW9PqOGRaaTflHAY";
    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN SEARCH METHOD
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Search YouTube videos.
     * Returns a list of YouTubeVideo results, or throws Exception on failure.
     */
    public List<YouTubeVideo> searchVideos(String query, int maxResults) throws Exception {

        // Validate inputs
        if (query == null || query.trim().isEmpty())
            throw new IllegalArgumentException("Le terme de recherche ne peut pas être vide.");
        if (query.trim().length() < 2)
            throw new IllegalArgumentException("Le terme de recherche doit contenir au moins 2 caractères.");

        maxResults = Math.max(1, Math.min(20, maxResults));

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String urlStr  = SEARCH_URL
                + "?part=snippet"
                + "&q="          + encoded
                + "&type=video"
                + "&maxResults=" + maxResults
                + "&key="        + API_KEY;

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("User-Agent", "EchoCare/1.0");

            int code = conn.getResponseCode();
            if (code != 200) {
                // Read error body
                String errBody = readStream(conn.getErrorStream());
                String reason  = parseError(errBody);
                throw new Exception("YouTube API erreur HTTP " + code + ": " + reason);
            }

            String responseBody = readStream(conn.getInputStream());
            return parseResults(responseBody);

        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STREAM READER
    // ══════════════════════════════════════════════════════════════════════════

    private String readStream(java.io.InputStream stream) throws Exception {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  JSON PARSING
    // ══════════════════════════════════════════════════════════════════════════

    private List<YouTubeVideo> parseResults(String json) {
        List<YouTubeVideo> results = new ArrayList<>();

        int itemsIdx = json.indexOf("\"items\"");
        if (itemsIdx == -1) return results;

        int arrStart = json.indexOf('[', itemsIdx);
        if (arrStart == -1) return results;

        // Walk through each { } block at the top level of the items array
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
            // videoId lives inside "id":{"kind":"...","videoId":"XXXX"}
            String videoId = extractNestedField(block, "id", "videoId");
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
            System.err.println("parseItem error: " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  JSON HELPERS (no external dependencies)
    // ══════════════════════════════════════════════════════════════════════════

    /** Extract value of a simple JSON string field: "key":"value" */
    private String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        idx += search.length();
        // Skip whitespace and colon
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':')) idx++;
        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length() && json.charAt(idx) != '"') {
            if (json.charAt(idx) == '\\' && idx + 1 < json.length()) {
                idx++;
                char next = json.charAt(idx);
                switch (next) {
                    case 'n'  -> sb.append(' ');
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'r'  -> { /* skip */ }
                    default   -> sb.append(next);
                }
            } else {
                sb.append(json.charAt(idx));
            }
            idx++;
        }
        return sb.toString();
    }

    /** Extract child field from a named sub-object: parentKey.childKey */
    private String extractNestedField(String json, String parentKey, String childKey) {
        String search = "\"" + parentKey + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
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
            if      (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\u0026", "&")
                .replace("\\u0027", "'")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .trim();
    }

    private String parseError(String errorJson) {
        if (errorJson == null || errorJson.isEmpty()) return "Erreur inconnue";
        String msg = extractField(errorJson, "message");
        return msg != null ? msg : errorJson.substring(0, Math.min(200, errorJson.length()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULT CLASS
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
