package services;

import utils.VideoPlayerUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * QuizGeneratorAPI — Génère des questions de quiz via OpenRouter AI.
 *
 * generateFromYouTube(url, count) :
 *   1. Extrait l'ID YouTube
 *   2. Récupère le titre via oEmbed (sans clé API)
 *   3. Récupère la transcription réelle (sous-titres automatiques)
 *   4. Envoie la TRANSCRIPTION à l'IA → questions sur le CONTENU de la vidéo
 */
public class QuizGeneratorAPI {

    private static final String OPENROUTER_URL   = "https://openrouter.ai/api/v1/chat/completions";
    private static final String OPENROUTER_KEY   = "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";
    private static final String OPENROUTER_MODEL = "google/gemma-3-1b-it:free";
    private static final String OLLAMA_URL       = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL     = "mistral";

    // ════════════════════════════════════════════════════════════════════
    //  PUBLIC — Generate from title/description
    // ════════════════════════════════════════════════════════════════════

    public List<GeneratedQuestion> generateQuestions(String title, String description, int count) throws Exception {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Titre requis.");
        if (count < 1 || count > 20)         throw new IllegalArgumentException("Entre 1 et 20 questions.");

        String context = (description != null && !description.isBlank()) ? description : title;

        try {
            List<GeneratedQuestion> result = callOpenRouter(title, context, count);
            if (!result.isEmpty()) return result;
        } catch (Exception e) {
            System.err.println("[QuizGen] OpenRouter: " + e.getMessage());
        }

        try {
            List<GeneratedQuestion> result = callOllama(title, context, count);
            if (!result.isEmpty()) return result;
        } catch (Exception e) {
            System.err.println("[QuizGen] Ollama: " + e.getMessage());
        }

        return generateFallbackQuestions(title, count);
    }

    // ════════════════════════════════════════════════════════════════════
    //  PUBLIC — Generate FROM YouTube video content (transcript)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Fetches the real transcript of a YouTube video and generates questions
     * about what is ACTUALLY SAID in the video — not just its title or URL.
     */
    public List<GeneratedQuestion> generateFromYouTube(String youtubeUrl, int count) throws Exception {
        if (count < 1 || count > 20) throw new IllegalArgumentException("Entre 1 et 20 questions.");

        String videoId = VideoPlayerUtil.extractYouTubeId(youtubeUrl);
        if (videoId == null || videoId.isBlank())
            throw new IllegalArgumentException("URL YouTube invalide : " + youtubeUrl);

        // Step 1: get video title via oEmbed (no API key needed)
        String videoTitle = fetchVideoTitle(videoId);
        System.out.println("[QuizGen] Titre vidéo : " + videoTitle);

        // Step 2: get the real transcript (auto-generated subtitles)
        String transcript = fetchTranscript(videoId);

        // Step 3: build context — use transcript if available, fall back to title only
        String context;
        if (transcript != null && transcript.length() > 150) {
            // Limit to 3000 chars to fit in the prompt window
            String t = transcript.length() > 3000 ? transcript.substring(0, 3000) + "..." : transcript;
            context = "Transcription de la vidéo :\n" + t;
            System.out.println("[QuizGen] Transcription récupérée : " + transcript.length() + " chars");
        } else {
            // No transcript found — questions will be about the topic of the title
            context = "Contenu de la vidéo sur le sujet : " + videoTitle;
            System.out.println("[QuizGen] Pas de transcription — utilisation du titre");
        }

        // Step 4: send transcript to AI to generate content-based questions
        return generateQuestions(videoTitle, context, count);
    }

    // ════════════════════════════════════════════════════════════════════
    //  YOUTUBE — Title via oEmbed (no API key)
    // ════════════════════════════════════════════════════════════════════

    private String fetchVideoTitle(String videoId) {
        try {
            String response = httpGet("https://www.youtube.com/oembed?url="
                    + "https://www.youtube.com/watch?v=" + videoId + "&format=json");
            String title = extractStringField(response, "title");
            if (title != null && !title.isBlank()) return title;
        } catch (Exception e) {
            System.err.println("[QuizGen] oEmbed: " + e.getMessage());
        }
        return videoId;
    }

    // ════════════════════════════════════════════════════════════════════
    //  YOUTUBE — Transcript via public subtitle API (no API key)
    // ════════════════════════════════════════════════════════════════════

    private String fetchTranscript(String videoId) {
        try {
            // Fetch the YouTube watch page to find the subtitle URL embedded in its JSON
            String html = httpGetWithAgent(
                    "https://www.youtube.com/watch?v=" + videoId,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120");

            String subtitleUrl = extractSubtitleUrl(html);
            if (subtitleUrl == null) {
                System.out.println("[QuizGen] Aucun sous-titre pour : " + videoId);
                return null;
            }

            String xml = httpGet(subtitleUrl);
            return parseSubtitleXml(xml);

        } catch (Exception e) {
            System.err.println("[QuizGen] Transcript: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the subtitle URL from YouTube's embedded JSON (captionTracks.baseUrl).
     * YouTube includes this in every watch page for videos that have auto-captions.
     */
    private String extractSubtitleUrl(String html) {
        try {
            int captionIdx = html.indexOf("\"captionTracks\":");
            if (captionIdx == -1) return null;

            int urlIdx = html.indexOf("\"baseUrl\":\"", captionIdx);
            if (urlIdx == -1) return null;
            urlIdx += 11; // skip past "baseUrl":"

            StringBuilder sb = new StringBuilder();
            while (urlIdx < html.length()) {
                char c = html.charAt(urlIdx);
                if (c == '"') break;
                // Handle unicode escapes like \u0026 → &
                if (c == '\\' && urlIdx + 5 < html.length() && html.charAt(urlIdx + 1) == 'u') {
                    try {
                        sb.append((char) Integer.parseInt(html.substring(urlIdx + 2, urlIdx + 6), 16));
                        urlIdx += 6;
                        continue;
                    } catch (NumberFormatException ignored) {}
                }
                sb.append(c);
                urlIdx++;
            }
            String url = sb.toString();
            return url.startsWith("http") ? url : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses YouTube subtitle XML into plain text.
     * Format: <text start="..." dur="...">content here</text>
     */
    private String parseSubtitleXml(String xml) {
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

            String word = xml.substring(contentStart, contentEnd)
                    .replace("&amp;", "&").replace("&lt;", "<")
                    .replace("&gt;", ">").replace("&quot;", "\"")
                    .replace("&#39;", "'").replace("\n", " ")
                    .replaceAll("<[^>]+>", "").trim();

            if (!word.isEmpty()) sb.append(word).append(" ");
            i = contentEnd + 7;
        }
        return sb.toString().trim();
    }

    // ════════════════════════════════════════════════════════════════════
    //  AI — OpenRouter
    // ════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> callOpenRouter(String title, String context, int count) throws Exception {
        String body = "{"
                + "\"model\":" + jsonStr(OPENROUTER_MODEL) + ","
                + "\"messages\":[{\"role\":\"user\",\"content\":"
                + jsonStr(buildPrompt(title, context, count)) + "}],"
                + "\"max_tokens\":2500,\"temperature\":0.95}";

        String raw = httpPost(OPENROUTER_URL, body,
                "Authorization", "Bearer " + OPENROUTER_KEY,
                "HTTP-Referer", "https://echocare.app",
                "X-Title", "EchoCare Quiz Generator");

        String content = extractJsonArray(raw);
        if (content == null) {
            String field = extractStringField(raw, "content");
            if (field != null) content = extractJsonArray(field);
        }
        return parseJsonQuestions(content);
    }

    // ════════════════════════════════════════════════════════════════════
    //  AI — Ollama (local fallback)
    // ════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> callOllama(String title, String context, int count) throws Exception {
        String body = "{\"model\":" + jsonStr(OLLAMA_MODEL)
                + ",\"prompt\":" + jsonStr(buildPrompt(title, context, count))
                + ",\"stream\":false}";
        String response = httpPost(OLLAMA_URL, body);
        String content = extractJsonArray(response);
        if (content == null) {
            String field = extractStringField(response, "response");
            if (field != null) content = extractJsonArray(field);
        }
        return parseJsonQuestions(content);
    }

    // ════════════════════════════════════════════════════════════════════
    //  PROMPT — tells AI to use transcript content for questions
    // ════════════════════════════════════════════════════════════════════

    private String buildPrompt(String title, String context, int count) {
        // Add timestamp seed so AI generates different questions each call
        long seed = System.currentTimeMillis() % 9999;
        String[] angles = {
                "Concentre-toi sur les concepts fondamentaux.",
                "Axe les questions sur les applications pratiques.",
                "Mets l'accent sur les définitions et terminologie.",
                "Crée des questions sur les causes et conséquences.",
                "Focus sur les exemples concrets et cas réels."
        };
        String angle = angles[(int)(seed % angles.length)];
        return "Tu es un expert en création de quiz pédagogiques. Seed:" + seed + ". "
                + "Génère exactement " + count + " questions QCM DIFFÉRENTES ET VARIÉES sur : \"" + title + "\". "
                + angle + " "
                + "Contenu de référence : " + context + ". "
                + "IMPORTANT: Ne répète pas les mêmes questions. Varie les formulations et angles d'approche. "
                + "Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ni après, sans balises markdown. "
                + "Format exact : [{\"question\":\"Texte ?\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correct\":0,\"points\":10}] "
                + "Règles : correct = index(0-3) de la bonne réponse. points = 5 ou 10. Questions en français. JSON UNIQUEMENT.";
    }

    // ════════════════════════════════════════════════════════════════════
    //  FALLBACK — generic questions when AI is unavailable
    // ════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> generateFallbackQuestions(String topic, int count) {
        List<GeneratedQuestion[]> bank = new ArrayList<>();
        bank.add(q("Quelle est la définition de \"" + topic + "\" dans un contexte professionnel ?",
                new String[]{"Une compétence comportementale clé", "Un outil informatique", "Un diplôme universitaire", "Un processus administratif"}, 0, 10));
        bank.add(q("Lequel de ces éléments est fondamental pour maîtriser \"" + topic + "\" ?",
                new String[]{"La pratique régulière", "Un équipement coûteux", "Des années d'expérience uniquement", "Un titre hiérarchique"}, 0, 10));
        bank.add(q("Comment évaluer l'efficacité d'un apprentissage en \"" + topic + "\" ?",
                new String[]{"Par des retours d'expérience concrets", "Par la théorie seule", "Par le nombre d'heures", "Par l'ancienneté"}, 0, 5));
        bank.add(q("Quel obstacle est le plus fréquent lors du développement de \"" + topic + "\" ?",
                new String[]{"La résistance au changement", "Le manque de temps", "L'absence de ressources", "La complexité technique"}, 0, 5));
        bank.add(q("Quelle approche favorise le mieux le développement de \"" + topic + "\" ?",
                new String[]{"L'apprentissage par la pratique", "La lecture seule", "Les cours magistraux", "La mémorisation"}, 0, 10));
        bank.add(q("Dans une équipe, comment \"" + topic + "\" améliore-t-il la collaboration ?",
                new String[]{"En créant un environnement de confiance", "En imposant des règles strictes", "En réduisant les interactions", "En formalisant tout par écrit"}, 0, 5));
        bank.add(q("Quel est l'impact de \"" + topic + "\" sur la performance professionnelle ?",
                new String[]{"Il augmente la productivité et la satisfaction", "Il n'a pas d'impact mesurable", "Il complique les processus", "Il est uniquement personnel"}, 0, 10));
        bank.add(q("Comment intégrer \"" + topic + "\" dans sa routine quotidienne ?",
                new String[]{"Par de petites habitudes régulières", "Par une formation unique intensive", "Par des lectures hebdomadaires", "Par des formations annuelles"}, 0, 5));
        bank.add(q("Quel signe montre une bonne maîtrise de \"" + topic + "\" ?",
                new String[]{"Adapter son comportement selon le contexte", "Toujours agir de la même manière", "Éviter les situations difficiles", "Déléguer systématiquement"}, 0, 10));
        bank.add(q("Pourquoi \"" + topic + "\" est-il important dans le monde du travail actuel ?",
                new String[]{"Il complète les compétences techniques", "Il remplace les compétences techniques", "Il est utile aux managers uniquement", "Il est facultatif"}, 0, 5));

        List<GeneratedQuestion> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, bank.size()); i++) result.add(bank.get(i)[0]);
        return result;
    }

    private GeneratedQuestion[] q(String text, String[] opts, int correct, int points) {
        GeneratedQuestion gq = new GeneratedQuestion();
        gq.questionText = text;
        gq.options = List.of(opts);
        gq.correctIndex = correct;
        gq.points = points;
        return new GeneratedQuestion[]{gq};
    }

    // ════════════════════════════════════════════════════════════════════
    //  HTTP HELPERS
    // ════════════════════════════════════════════════════════════════════

    private String httpPost(String urlStr, String body, String... headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        for (int i = 0; i + 1 < headers.length; i += 2)
            conn.setRequestProperty(headers[i], headers[i + 1]);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new Exception("HTTP " + code + " — no response");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        if (code < 200 || code >= 300)
            throw new Exception("HTTP " + code + ": " + sb.toString().substring(0, Math.min(300, sb.length())));
        return sb.toString();
    }

    private String httpGet(String urlStr) throws Exception {
        return httpGetWithAgent(urlStr, "EchoCare/1.0");
    }

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
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════
    //  JSON PARSING HELPERS
    // ════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> parseJsonQuestions(String raw) {
        List<GeneratedQuestion> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        String clean = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
        String arr = extractJsonArray(clean);
        if (arr == null) return result;

        int depth = 0, start = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '"') { i++; while (i < arr.length()) { char s = arr.charAt(i); if (s == '\\') i++; else if (s == '"') break; i++; } }
            else if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    GeneratedQuestion gq = parseOneQuestion(arr.substring(start, i + 1));
                    if (gq != null) result.add(gq);
                    start = -1;
                }
            }
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
                if (arrS != -1 && arrE != -1)
                    gq.options = parseStringArray(block.substring(arrS + 1, arrE));
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

    private String extractJsonArray(String text) {
        if (text == null) return null;
        int depth = 0, start = -1;
        boolean inStr = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inStr) { if (c == '\\') i++; else if (c == '"') inStr = false; }
            else {
                if (c == '"') inStr = true;
                else if (c == '[') { if (depth == 0) start = i; depth++; }
                else if (c == ']') { depth--; if (depth == 0 && start != -1) return text.substring(start, i + 1); }
            }
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
            if (c == '\\') {
                idx++;
                if (idx >= json.length()) break;
                char e = json.charAt(idx);
                switch (e) { case '"': sb.append('"'); break; case '\\': sb.append('\\'); break; case 'n': sb.append('\n'); break; default: sb.append(e); }
            } else if (c == '"') break;
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
                if (c == '\\') { i++; if (i < s.length()) { char e = s.charAt(i); switch (e) { case '"': sb.append('"'); break; case '\\': sb.append('\\'); break; case 'n': sb.append('\n'); break; default: sb.append(e); } } }
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
        int depth = 0;
        boolean inStr = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inStr) { if (c == '\\') i++; else if (c == '"') inStr = false; }
            else { if (c == '"') inStr = true; else if (c == '[') depth++; else if (c == ']') { depth--; if (depth == 0) return i; } }
        }
        return -1;
    }

    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", " ") + "\"";
    }

    // ════════════════════════════════════════════════════════════════════
    //  RESULT CLASS
    // ════════════════════════════════════════════════════════════════════

    public static class GeneratedQuestion {
        public String       questionText;
        public List<String> options;
        public int          correctIndex;
        public int          points;
    }
}
