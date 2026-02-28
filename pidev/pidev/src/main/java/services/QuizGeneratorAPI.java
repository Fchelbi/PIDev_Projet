package services;

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
 * PROBLÈME ORIGINAL (cassé) :
 *   - API HuggingFace Inference avec Mistral-7B gratuit → fréquemment
 *     indisponible (modèle en veille, timeout 60s, cold start >120s).
 *   - Parsing JSON fait à la main avec split("---") → fragile, cassé
 *     dès que le modèle change son formatage.
 *
 * SOLUTION APPLIQUÉE :
 *   API 1 (principale) : OpenRouter.ai — agrégateur d'IA gratuit.
 *     Modèle: google/gemma-3-1b-it:free (toujours actif, rapide, gratuit)
 *     Endpoint: https://openrouter.ai/api/v1/chat/completions
 *     Format: OpenAI-compatible, JSON standard
 *
 *   API 2 (fallback) : Ollama local (si le prof a Ollama installé)
 *     Sinon : génération de questions par défaut intelligente (sans API)
 *
 *   Parsing : On demande à l'IA de répondre en JSON pur → parsing fiable
 *              avec extraction manuelle du JSON (sans dépendance externe).
 * ═══════════════════════════════════════════════════════════════════════
 *
 * CONTRÔLE DE SAISIE (validation des paramètres) :
 *   - title : ne doit pas être vide
 *   - count : entre 1 et 20
 *   - Si l'API échoue → fallback avec questions génériques de qualité
 */
public class QuizGeneratorAPI {

    // ── API 1 : OpenRouter (agrégateur IA gratuit) ─────────────────────────
    private static final String OPENROUTER_URL    = "https://openrouter.ai/api/v1/chat/completions";
    // Clé gratuite — remplacer par votre propre clé sur openrouter.ai (gratuit à créer)
    private static final String OPENROUTER_KEY    = "sk-or-v1-VOTRE_CLE_OPENROUTER_ICI";
    private static final String OPENROUTER_MODEL  = "google/gemma-3-1b-it:free";

    // ── API 2 : Ollama local (si installé sur la machine) ─────────────────
    private static final String OLLAMA_URL        = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL      = "mistral";

    // ══════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Génère des questions de quiz sur un sujet donné.
     *
     * @param title  Titre de la formation (ex: "Communication")
     * @param desc   Description de la formation
     * @param count  Nombre de questions (1-20)
     * @return Liste de GeneratedQuestion
     * @throws Exception si toutes les APIs échouent
     */
    public List<GeneratedQuestion> generateQuestions(String title, String desc, int count)
            throws Exception {

        // ── Contrôle de saisie ──────────────────────────────────────────────
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de la formation est requis.");
        }
        if (count < 1 || count > 20) {
            throw new IllegalArgumentException("Le nombre de questions doit être entre 1 et 20.");
        }

        String topic = title.trim();
        String context = (desc != null && !desc.trim().isEmpty()) ? desc.trim() : topic;

        // ── Tentative 1 : OpenRouter ────────────────────────────────────────
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

        // ── Tentative 2 : Ollama local ──────────────────────────────────────
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

        // ── Fallback : questions génériques de qualité ───────────────────────
        System.out.println("[QuizGen] ⚠️ Fallback questions génériques pour: " + topic);
        return generateFallbackQuestions(topic, count);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  API 1 : OpenRouter
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> callOpenRouter(String title, String context, int count)
            throws Exception {

        String prompt = buildPrompt(title, context, count);

        // Corps de la requête au format OpenAI-compatible
        String body = "{"
                + "\"model\":\"" + OPENROUTER_MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":"
                + jsonStr(prompt) + "}],"
                + "\"max_tokens\":2500,"
                + "\"temperature\":0.7"
                + "}";

        String response = httpPost(OPENROUTER_URL, body,
                "Authorization", "Bearer " + OPENROUTER_KEY,
                "HTTP-Referer", "https://echocare.app",
                "X-Title", "EchoCare Quiz Generator");

        // Extraire le texte du champ "content" de la réponse OpenAI
        String content = extractOpenAIContent(response);
        return parseJsonQuestions(content);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  API 2 : Ollama local
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> callOllama(String title, String context, int count)
            throws Exception {

        String prompt = buildPrompt(title, context, count);

        String body = "{"
                + "\"model\":\"" + OLLAMA_MODEL + "\","
                + "\"prompt\":" + jsonStr(prompt) + ","
                + "\"stream\":false"
                + "}";

        String response = httpPost(OLLAMA_URL, body);
        // Ollama répond avec {"response":"...",...}
        String content = extractField(response, "response");
        return parseJsonQuestions(content);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROMPT CONSTRUCTION
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * On demande à l'IA de répondre UNIQUEMENT en JSON.
     * Format strict → parsing fiable sans librairie externe.
     */
    private String buildPrompt(String title, String context, int count) {
        return "Tu es un expert en création de quiz pédagogiques. "
                + "Génère exactement " + count + " questions QCM sur le sujet : \"" + title + "\". "
                + "Contexte : " + context + ". "
                + "Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ni après, "
                + "sans balises markdown. Format exact :\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"Texte de la question ?\",\n"
                + "    \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n"
                + "    \"correct\": 0,\n"
                + "    \"points\": 10\n"
                + "  }\n"
                + "]\n"
                + "Règles : "
                + "- correct est l'index (0-3) de la bonne réponse. "
                + "- points est 5 ou 10. "
                + "- Questions en français. "
                + "- Ne génère que du JSON, RIEN d'autre.";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PARSING JSON  (sans librairie externe)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Parse le JSON retourné par l'IA.
     * On nettoie d'abord les balises markdown éventuelles, puis on extrait
     * chaque bloc { ... } et on lit les champs question/options/correct/points.
     */
    private List<GeneratedQuestion> parseJsonQuestions(String raw) {
        List<GeneratedQuestion> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;

        // Nettoyer les balises markdown ```json ... ```
        String clean = raw.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        // Trouver le tableau JSON
        int arrStart = clean.indexOf('[');
        int arrEnd   = clean.lastIndexOf(']');
        if (arrStart == -1 || arrEnd == -1 || arrEnd <= arrStart) {
            System.err.println("[QuizGen] Pas de tableau JSON trouvé dans : " + clean.substring(0, Math.min(200, clean.length())));
            return result;
        }
        clean = clean.substring(arrStart, arrEnd + 1);

        // Découper les objets { ... }
        int depth = 0;
        int start = -1;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    String block = clean.substring(start, i + 1);
                    GeneratedQuestion gq = parseOneQuestion(block);
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

            gq.questionText = extractField(block, "question");
            if (gq.questionText == null || gq.questionText.isBlank()) return null;

            // Extraire le tableau options
            gq.options = new ArrayList<>();
            int optStart = block.indexOf("\"options\"");
            if (optStart != -1) {
                int arrS = block.indexOf('[', optStart);
                int arrE = block.indexOf(']', arrS);
                if (arrS != -1 && arrE != -1) {
                    String opts = block.substring(arrS + 1, arrE);
                    for (String part : opts.split(",")) {
                        String opt = part.replace("\"", "").trim();
                        if (!opt.isEmpty()) gq.options.add(opt);
                    }
                }
            }
            if (gq.options.size() < 2) return null;

            // correct index
            String correctStr = extractField(block, "correct");
            gq.correctIndex = 0;
            if (correctStr != null) {
                try { gq.correctIndex = Integer.parseInt(correctStr.trim()); } catch (NumberFormatException ignored) {}
            }
            if (gq.correctIndex < 0 || gq.correctIndex >= gq.options.size()) gq.correctIndex = 0;

            // points
            String pointsStr = extractField(block, "points");
            gq.points = 5;
            if (pointsStr != null) {
                try { gq.points = Integer.parseInt(pointsStr.trim()); } catch (NumberFormatException ignored) {}
            }

            return gq;
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FALLBACK — questions génériques intelligentes (sans API)
    // ══════════════════════════════════════════════════════════════════════════

    private List<GeneratedQuestion> generateFallbackQuestions(String topic, int count) {
        List<GeneratedQuestion[]> bank = new ArrayList<>();

        // Questions génériques adaptées aux soft skills / formations
        bank.add(q("Quelle est la définition de \"" + topic + "\" dans un contexte professionnel ?",
                new String[]{"Une compétence comportementale clé", "Un outil informatique", "Un diplôme universitaire", "Un processus administratif"}, 0, 10));
        bank.add(q("Lequel de ces éléments est fondamental pour maîtriser \"" + topic + "\" ?",
                new String[]{"La pratique régulière", "Un équipement coûteux", "Des années d'expérience uniquement", "Un titre hiérarchique"}, 0, 10));
        bank.add(q("Comment évaluer l'efficacité d'un apprentissage en \"" + topic + "\" ?",
                new String[]{"Par des retours d'expérience concrets", "Par la théorie seule", "Par le nombre d'heures", "Par l'ancienneté"}, 0, 5));
        bank.add(q("Quel obstacle est le plus fréquent lors du développement de \"" + topic + "\" ?",
                new String[]{"La résistance au changement", "Le manque de temps", "L'absence de ressources", "La complexité technique"}, 0, 5));
        bank.add(q("Quelle approche favorise le mieux le développement de \"" + topic + "\" ?",
                new String[]{"L'apprentissage par la pratique et la réflexion", "La lecture seule", "Les cours magistraux", "La mémorisation"}, 0, 10));
        bank.add(q("Dans une équipe, comment \"" + topic + "\" améliore-t-il la collaboration ?",
                new String[]{"En créant un environnement de confiance", "En imposant des règles strictes", "En réduisant les interactions", "En formalisant tout par écrit"}, 0, 5));
        bank.add(q("Quel est l'impact de \"" + topic + "\" sur la performance professionnelle ?",
                new String[]{"Il augmente la productivité et la satisfaction", "Il n'a pas d'impact mesurable", "Il complique les processus", "Il est uniquement personnel"}, 0, 10));
        bank.add(q("Comment intégrer \"" + topic + "\" dans sa routine quotidienne ?",
                new String[]{"Par de petites habitudes régulières", "Par une formation unique intensive", "Par des lectures hebdomadaires", "Par des formations annuelles uniquement"}, 0, 5));
        bank.add(q("Quel signe montre une bonne maîtrise de \"" + topic + "\" ?",
                new String[]{"Adapter son comportement selon le contexte", "Toujours agir de la même manière", "Éviter les situations difficiles", "Déléguer systématiquement"}, 0, 10));
        bank.add(q("Pourquoi \"" + topic + "\" est-il important dans le monde du travail actuel ?",
                new String[]{"Il complète les compétences techniques", "Il remplace les compétences techniques", "Il est uniquement utile aux managers", "Il est facultatif dans la plupart des métiers"}, 0, 5));

        List<GeneratedQuestion> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, bank.size()); i++) {
            result.add(bank.get(i)[0]);
        }
        return result;
    }

    private GeneratedQuestion[] q(String text, String[] opts, int correct, int points) {
        GeneratedQuestion gq = new GeneratedQuestion();
        gq.questionText = text;
        gq.options      = List.of(opts);
        gq.correctIndex = correct;
        gq.points       = points;
        return new GeneratedQuestion[]{gq};
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HTTP HELPER
    // ══════════════════════════════════════════════════════════════════════════

    private String httpPost(String urlStr, String body, String... headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        // Headers optionnels
        for (int i = 0; i + 1 < headers.length; i += 2) {
            conn.setRequestProperty(headers[i], headers[i + 1]);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream() : conn.getErrorStream();

        if (is == null) throw new Exception("HTTP " + code + " — pas de réponse");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }

        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code + ": " + sb.toString().substring(0, Math.min(200, sb.length())));
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PARSEURS DE CHAMPS JSON SIMPLES
    // ══════════════════════════════════════════════════════════════════════════

    /** Extrait la valeur d'un champ JSON simple : "key":"value" ou "key":123 */
    private String extractField(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        idx += search.length();
        // Sauter les espaces et le ':'
        while (idx < json.length() && (json.charAt(idx) == ' ' || json.charAt(idx) == ':')) idx++;
        if (idx >= json.length()) return null;

        if (json.charAt(idx) == '"') {
            // Valeur string
            idx++;
            StringBuilder sb = new StringBuilder();
            while (idx < json.length() && json.charAt(idx) != '"') {
                if (json.charAt(idx) == '\\') idx++; // échappement
                if (idx < json.length()) sb.append(json.charAt(idx));
                idx++;
            }
            return sb.toString();
        } else {
            // Valeur numérique ou booléenne
            int end = idx;
            while (end < json.length() && ",}\n\r ".indexOf(json.charAt(end)) == -1) end++;
            return json.substring(idx, end).trim();
        }
    }

    /** Extrait le texte du champ content dans une réponse OpenAI */
    private String extractOpenAIContent(String json) {
        // Structure: {"choices":[{"message":{"content":"..."}}]}
        String content = extractField(json, "content");
        if (content != null && !content.isBlank()) return content;
        // Fallback: chercher "text"
        return extractField(json, "text");
    }

    /** Échappe une chaîne pour l'insérer dans du JSON */
    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", " ") + "\"";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CLASSE RÉSULTAT
    // ══════════════════════════════════════════════════════════════════════════

    public static class GeneratedQuestion {
        public String       questionText;
        public List<String> options;
        public int          correctIndex;
        public int          points;
    }
}