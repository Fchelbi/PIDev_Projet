package services;

import utils.VideoPlayerUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizGeneratorAPI — Génération automatique de questions de quiz par IA
 *
 * ═══════════════════════════════════════════════════════════════════════
 * YOUTUBE QUIZ GENERATION (sans clé API, sans ouvrir le navigateur) :
 *
 *   generateFromYouTube(url, count) :
 *     1. Extrait l'ID de la vidéo
 *     2. Récupère le titre via YouTube oEmbed (gratuit, sans clé)
 *     3. Récupère la transcription (sous-titres auto) via l'API publique
 *     4. Envoie le VRAI contenu de la vidéo à l'IA OpenRouter
 *     → Les questions sont basées sur ce que dit réellement la vidéo
 *
 *   Signature : generateFromYouTube(String youtubeUrl, int count)
 *   — 2 paramètres seulement, pas de clé API requise
 * ═══════════════════════════════════════════════════════════════════════
 */
public class QuizGeneratorAPI {

    // ── API 1 : OpenRouter ─────────────────────────────────────────────────
    private static final String OPENROUTER_URL   = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_KEY   = "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";
    private static final String OPENROUTER_MODEL = "google/gemma-3-1b-it:free";

    // ── API 2 : Ollama local ───────────────────────────────────────────────
    private static final String OLLAMA_URL   = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "mistral";

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE — génère à partir d'un titre/description
    // ══════════════════════════════════════════════════════════════════════════

    public List<GeneratedQuestion> generateQuestions(String title, String desc, int count)
            throws Exception {

        if (title == null || title.trim().isEmpty())
            throw new IllegalArgumentException("Le titre de la formation est requis.");
        if (count < 1 || count > 20)
            throw new IllegalArgumentException("Le nombre de questions doit être entre 1 et 20.");

        String topic   = title.trim();
        String context = (desc != null && !desc.trim().isEmpty()) ? desc.trim() : topic;

        try {
            System.out.println("[QuizGen] Tentative OpenRouter...");
            List<GeneratedQuestion> result = callOpenRouter(topic, context, count);
            if (!result.isEmpty()) {
                System.out.println("[QuizGen] ✅ OpenRouter : " + result.size() + " questions");
                return result;
            }
        } catch (Exception e) {
            System.err.println("[QuizGen] OpenRouter échoué: " + e.getMessage());
        }

        try {
            System.out.println("[QuizGen] Tentative Ollama local...");
            List<GeneratedQuestion> result = callOllama(topic, context, count);
            if (!result.isEmpty()) {
                System.out.println("[QuizGen] ✅ Ollama : " + result.size() + " questions");
                return result;
            }
        } catch (Exception e) {
            System.err.println("[QuizGen] Ollama non disponible: " + e.getMessage());
        }

        System.out.println("[QuizGen] ⚠️ Fallback questions génériques pour: " + topic);
        return generateFallbackQuestions(topic, count);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GÉNÉRATION DEPUIS UNE VIDÉO YOUTUBE — sans clé API, sans navigateur
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Génère des questions basées sur le CONTENU RÉEL d'une vidéo YouTube.
     * Aucune clé API requise. Aucune ouverture de navigateur.
     *
     * @param youtubeUrl URL de la vidéo YouTube
     * @param count      Nombre de questions (1-20)
     */
    public List<GeneratedQuestion> generateFromYouTube(String youtubeUrl, int count)
            throws Exception {

        if (count < 1 || count > 20)
            throw new IllegalArgumentException("Le nombre de questions doit être entre 1 et 20.");

        String videoId = VideoPlayerUtil.extractYouTubeId(youtubeUrl);
        if (videoId == null || videoId.isEmpty())
            throw new IllegalArgumentException("URL YouTube invalide : " + youtubeUrl);

        System.out.println("[QuizGen] YouTube ID: " + videoId);

        // Titre via oEmbed (gratuit, sans clé)
        String videoTitle = fetchYouTubeTitle(videoId);
        System.out.println("[QuizGen] Titre: " + videoTitle);

        // Transcription via sous-titres publics (gratuit, sans clé)
        String transcript = fetchYouTubeTranscript(videoId);

        String context;
        if (transcript != null && transcript.length() > 100) {
            // Tronquer à 3000 chars pour ne pas dépasser la fenêtre du prompt
            String t = transcript.length() > 3000 ? transcript.substring(0, 3000) + "..." : transcript;
            context = "Transcription de la vidéo \"" + videoTitle + "\" :\n" + t;
            System.out.println("[QuizGen] ✅ Transcription: " + transcript.length() + " chars");
        } else {
            context = "Vidéo YouTube sur le sujet : " + videoTitle;
            System.out.println("[QuizGen] ⚠️ Pas de transcription, titre seul utilisé");
        }

        return generateQuestions(videoTitle, context, count);
    }

    // ── Titre via YouTube oEmbed (sans clé API) ────────────────────────────
    private String fetchYouTubeTitle(String videoId) {
        try {
            String resp = httpGet("https://www.youtube.com/oembed?url="
                    + "https://www.youtube.com/watch?v=" + videoId + "&format=json");
            String title = extractStringField(resp, "title");
            if (title != null && !title.isBlank()) return title;
        } catch (Exception e) {
            System.err.println("[QuizGen] oEmbed failed: " + e.getMessage());
        }
        return videoId;
    }

    // ── Transcription via sous-titres publics YouTube (sans clé API) ───────
    private String fetchYouTubeTranscript(String videoId) {
        try {
            // Récupérer la page YouTube pour trouver l'URL des sous-titres
            String html = httpGetWithAgent(
                    "https://www.youtube.com/watch?v=" + videoId,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120");

            String captionUrl = extractCaptionUrl(html);
            if (captionUrl == null) {
                System.out.println("[QuizGen] Aucun sous-titre disponible pour: " + videoId);
                return null;
            }

            String xml = httpGet(captionUrl);
            return parseTranscriptXml(xml);

        } catch (Exception e) {
            System.err.println("[QuizGen] Transcript failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrait l'URL des sous-titres depuis le HTML de la page YouTube.
     * YouTube inclut les URLs dans le JSON embarqué sous "captionTracks".
     */
    private String extractCaptionUrl(String html) {
        try {
            int idx = html.indexOf("\"captionTracks\":");
            if (idx == -1) return null;

            int urlIdx = html.indexOf("\"baseUrl\":\"", idx);
            if (urlIdx == -1) return null;
            urlIdx += "\"baseUrl\":\"".length();

            StringBuilder sb = new StringBuilder();
            while (urlIdx < html.length()) {
                char c = html.charAt(urlIdx);
                if (c == '"') break;
                if (c == '\\' && urlIdx + 1 < html.length()) {
                    char next = html.charAt(urlIdx + 1);
                    if (next == 'u' && urlIdx + 5 < html.length()) {
                        try {
                            sb.append((char) Integer.parseInt(html.substring(urlIdx + 2, urlIdx + 6), 16));
                            urlIdx += 6;
                            continue;
                        } catch (NumberFormatException ignored) {}
                    }
                    urlIdx++; // skip backslash
                    if (urlIdx < html.length()) sb.append(html.charAt(urlIdx));
                } else {
                    sb.append(c);
                }
                urlIdx++;
            }
            String raw = sb.toString();
            return raw.startsWith("http") ? raw : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse le XML des sous-titres YouTube.
     * Format : <text start="..." dur="...">contenu</text>
     */
    private String parseTranscriptXml(String xml) {
        if (xml == null || xml.isBlank()) return null;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < xml.length()) {
            int tagStart = xml.indexOf("<text ", i);
            if (tagStart == -1) break;
            int contentStart = xml.indexOf('>', tagStart);
            if (contentStart == -1) break;
            int contentEnd = xml.indexOf("</text>", ++contentStart);
            if (contentEnd == -1) break;

            String content = xml.substring(contentStart, contentEnd)
                    .replace("&amp;", "&").replace("&lt;", "<")
                    .replace("&gt;", ">").replace("&quot;", "\"")
                    .replace("&#39;", "'").replace("\n", " ")
                    .replaceAll("<[^>]+>", "").trim();

            if (!content.isEmpty()) sb.append(content).append(" ");
            i = contentEnd + 7;
        }
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  API 1 : OpenRouter
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> callOpenRouter(String title, String context, int count)
            throws Exception {
        String body = "{"
                + "\"model\":" + jsonStr(OPENROUTER_MODEL) + ","
                + "\"messages\":[{\"role\":\"user\",\"content\":"
                + jsonStr(buildPrompt(title, context, count)) + "}],"
                + "\"max_tokens\":2500,\"temperature\":0.7}";

        String raw = httpPost(OPENROUTER_URL, body,
                "Authorization", "Bearer " + OPENROUTER_KEY,
                "HTTP-Referer", "https://echocare.app",
                "X-Title", "EchoCare Quiz Generator");

        System.out.println("[QuizGen] OpenRouter raw (200): " + raw.substring(0, Math.min(200, raw.length())));

        String content = extractJsonArray(raw);
        if (content == null || content.isBlank()) {
            String field = extractStringField(raw, "content");
            if (field != null) content = extractJsonArray(field);
        }
        return parseJsonQuestions(content);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  API 2 : Ollama
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> callOllama(String title, String context, int count)
            throws Exception {
        String body = "{\"model\":" + jsonStr(OLLAMA_MODEL)
                + ",\"prompt\":" + jsonStr(buildPrompt(title, context, count))
                + ",\"stream\":false}";
        String response = httpPost(OLLAMA_URL, body);
        String content = extractJsonArray(response);
        if (content == null || content.isBlank()) {
            String field = extractStringField(response, "response");
            if (field != null) content = extractJsonArray(field);
        }
        return parseJsonQuestions(content);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROMPT
    // ══════════════════════════════════════════════════════════════════════════

    private String buildPrompt(String title, String context, int count) {
        return "Tu es un expert en création de quiz pédagogiques. "
                + "Génère exactement " + count + " questions QCM basées sur ce contenu : \"" + title + "\". "
                + "Contexte (utilise ce texte pour créer des questions précises et pertinentes sur le vrai contenu) : "
                + context + ". "
                + "Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ni après, "
                + "sans balises markdown. Format exact :\n"
                + "[{\"question\":\"Texte ?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correct\":0,\"points\":10}]\n"
                + "Règles : correct=index(0-3) bonne réponse. points=5 ou 10. Questions en français. JSON UNIQUEMENT.";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  JSON PARSING
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> parseJsonQuestions(String raw) {
        List<GeneratedQuestion> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        String clean = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        String arr = extractJsonArray(clean);
        if (arr == null) {
            System.err.println("[QuizGen] No JSON array found in: " + clean.substring(0, Math.min(200, clean.length())));
            return result;
        }
        int depth = 0, start = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '"') { i++; while (i < arr.length()) { char sc = arr.charAt(i); if (sc == '\\') i++; else if (sc == '"') break; i++; } }
            else if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start != -1) { GeneratedQuestion gq = parseOneQuestion(arr.substring(start, i + 1)); if (gq != null) result.add(gq); start = -1; } }
        }
        return result;
    }

    private GeneratedQuestion parseOneQuestion(String block) {
        try {
            GeneratedQuestion gq = new GeneratedQuestion();
            gq.questionText = extractStringField(block, "question");
            if (gq.questionText == null || gq.questionText.isBlank()) return null;
            gq.options = new ArrayList<>();
            int optStart = block.indexOf("\"options\"");
            if (optStart != -1) {
                int arrS = block.indexOf('[', optStart);
                int arrE = findMatchingBracket(block, arrS);
                if (arrS != -1 && arrE != -1) gq.options = parseStringArray(block.substring(arrS + 1, arrE));
            }
            if (gq.options.size() < 2) return null;
            String cs = extractNumberField(block, "correct");
            gq.correctIndex = 0;
            if (cs != null) try { gq.correctIndex = Integer.parseInt(cs.trim()); } catch (NumberFormatException ignored) {}
            if (gq.correctIndex < 0 || gq.correctIndex >= gq.options.size()) gq.correctIndex = 0;
            String ps = extractNumberField(block, "points");
            gq.points = 5;
            if (ps != null) try { gq.points = Integer.parseInt(ps.trim()); } catch (NumberFormatException ignored) {}
            return gq;
        } catch (Exception e) { return null; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FALLBACK
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> generateFallbackQuestions(String topic, int count) {
        List<GeneratedQuestion[]> bank = new ArrayList<>();
        bank.add(q("Quelle est la définition de \""+topic+"\" dans un contexte professionnel ?", new String[]{"Une compétence comportementale clé","Un outil informatique","Un diplôme universitaire","Un processus administratif"}, 0, 10));
        bank.add(q("Lequel de ces éléments est fondamental pour maîtriser \""+topic+"\" ?", new String[]{"La pratique régulière","Un équipement coûteux","Des années d'expérience uniquement","Un titre hiérarchique"}, 0, 10));
        bank.add(q("Comment évaluer l'efficacité d'un apprentissage en \""+topic+"\" ?", new String[]{"Par des retours d'expérience concrets","Par la théorie seule","Par le nombre d'heures","Par l'ancienneté"}, 0, 5));
        bank.add(q("Quel obstacle est le plus fréquent lors du développement de \""+topic+"\" ?", new String[]{"La résistance au changement","Le manque de temps","L'absence de ressources","La complexité technique"}, 0, 5));
        bank.add(q("Quelle approche favorise le mieux le développement de \""+topic+"\" ?", new String[]{"L'apprentissage par la pratique et la réflexion","La lecture seule","Les cours magistraux","La mémorisation"}, 0, 10));
        bank.add(q("Dans une équipe, comment \""+topic+"\" améliore-t-il la collaboration ?", new String[]{"En créant un environnement de confiance","En imposant des règles strictes","En réduisant les interactions","En formalisant tout par écrit"}, 0, 5));
        bank.add(q("Quel est l'impact de \""+topic+"\" sur la performance professionnelle ?", new String[]{"Il augmente la productivité et la satisfaction","Il n'a pas d'impact mesurable","Il complique les processus","Il est uniquement personnel"}, 0, 10));
        bank.add(q("Comment intégrer \""+topic+"\" dans sa routine quotidienne ?", new String[]{"Par de petites habitudes régulières","Par une formation unique intensive","Par des lectures hebdomadaires","Par des formations annuelles uniquement"}, 0, 5));
        bank.add(q("Quel signe montre une bonne maîtrise de \""+topic+"\" ?", new String[]{"Adapter son comportement selon le contexte","Toujours agir de la même manière","Éviter les situations difficiles","Déléguer systématiquement"}, 0, 10));
        bank.add(q("Pourquoi \""+topic+"\" est-il important dans le monde du travail actuel ?", new String[]{"Il complète les compétences techniques","Il remplace les compétences techniques","Il est uniquement utile aux managers","Il est facultatif dans la plupart des métiers"}, 0, 5));
        List<GeneratedQuestion> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, bank.size()); i++) result.add(bank.get(i)[0]);
        return result;
    }

    private GeneratedQuestion[] q(String text, String[] opts, int correct, int points) {
        GeneratedQuestion gq = new GeneratedQuestion();
        gq.questionText = text; gq.options = List.of(opts); gq.correctIndex = correct; gq.points = points;
        return new GeneratedQuestion[]{gq};
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HTTP
    // ══════════════════════════════════════════════════════════════════════════

    private String httpPost(String urlStr, String body, String... headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        for (int i = 0; i + 1 < headers.length; i += 2) conn.setRequestProperty(headers[i], headers[i + 1]);
        try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new Exception("HTTP " + code + " — no response");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + sb.toString().substring(0, Math.min(300, sb.length())));
        return sb.toString();
    }

    private String httpGet(String urlStr) throws Exception { return httpGetWithAgent(urlStr, "EchoCare/1.0"); }

    private String httpGetWithAgent(String urlStr, String userAgent) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(20_000);
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new Exception("HTTP " + code);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  JSON HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String extractJsonArray(String text) {
        if (text == null) return null;
        int depth = 0, start = -1; boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) { if (c == '\\') i++; else if (c == '"') inString = false; }
            else { if (c == '"') inString = true; else if (c == '[') { if (depth == 0) start = i; depth++; } else if (c == ']') { depth--; if (depth == 0 && start != -1) return text.substring(start, i + 1); } }
        }
        return null;
    }

    private String extractStringField(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        idx += key.length() + 2;
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':')) idx++;
        if (idx >= json.length() || json.charAt(idx) != '"') return null;
        idx++;
        StringBuilder sb = new StringBuilder();
        while (idx < json.length()) {
            char c = json.charAt(idx);
            if (c == '\\') { idx++; if (idx >= json.length()) break; char e = json.charAt(idx); switch(e){case '"': sb.append('"'); break; case '\\': sb.append('\\'); break; case 'n': sb.append('\n'); break; case 'r': sb.append('\r'); break; case 't': sb.append('\t'); break; default: sb.append(e);} }
            else if (c == '"') break;
            else sb.append(c);
            idx++;
        }
        return sb.toString();
    }

    private String extractNumberField(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        idx += key.length() + 2;
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':')) idx++;
        if (idx >= json.length()) return null;
        int end = idx;
        while (end < json.length() && ",}\n\r ".indexOf(json.charAt(end)) == -1) end++;
        return json.substring(idx, end).trim();
    }

    private List<String> parseStringArray(String s) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && s.charAt(i) != '"') i++;
            if (i >= s.length()) break;
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '\\') { i++; if (i < s.length()) { char e = s.charAt(i); switch(e){case '"': sb.append('"'); break; case '\\': sb.append('\\'); break; case 'n': sb.append('\n'); break; default: sb.append(e);} } }
                else if (c == '"') break;
                else sb.append(c);
                i++;
            }
            String val = sb.toString().trim();
            if (!val.isEmpty()) result.add(val);
            i++;
        }
        return result;
    }

    private int findMatchingBracket(String text, int start) {
        if (start < 0 || start >= text.length()) return -1;
        int depth = 0; boolean inStr = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inStr) { if (c == '\\') i++; else if (c == '"') inStr = false; }
            else { if (c == '"') inStr = true; else if (c == '[') depth++; else if (c == ']') { depth--; if (depth == 0) return i; } }
        }
        return -1;
    }

    private String jsonStr(String s) {
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","").replace("\t"," ") + "\"";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESULT CLASS
    // ══════════════════════════════════════════════════════════════════════════

    public static class GeneratedQuestion {
        public String       questionText;
        public List<String> options;
        public int          correctIndex;
        public int          points;
    }
}
