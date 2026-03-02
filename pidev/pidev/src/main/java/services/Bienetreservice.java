package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ✅ BienEtreService
 * - Aztro API  : horoscope + conseil du jour (100% gratuit, zéro clé)
 * - NewsAPI    : fallback statique articles santé mentale bien faits
 */
public class Bienetreservice {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    // ══════════════════════════════════════════════════════════
    //  MODÈLES
    // ══════════════════════════════════════════════════════════

    public static class HoroscopeResult {
        public String sign;
        public String signEmoji;
        public String description;
        public String compatibility;
        public String mood;
        public String color;
        public String luckyNumber;
        public String luckyTime;
        public String dateRange;
        public String wellbeingTip;  // conseil bien-être EchoCare
        public boolean fromApi;      // true = Aztro, false = fallback
    }

    public static class NewsArticle {
        public String title;
        public String summary;
        public String source;
        public String category;
        public String categoryColor;
        public String emoji;
        public String readTime;
    }

    // ══════════════════════════════════════════════════════════
    //  AZTRO HOROSCOPE API
    //  POST https://aztro.sameerkumar.website/?sign=SIGN&day=today
    //  100% gratuit, zero clé, zero inscription
    // ══════════════════════════════════════════════════════════

    public HoroscopeResult fetchHoroscope(String sign) {
        try {
            String signLower = sign.toLowerCase(Locale.ENGLISH);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://aztro.sameerkumar.website/?sign=" + signLower + "&day=today"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                HoroscopeResult result = new HoroscopeResult();
                result.sign          = capitalize(sign);
                result.signEmoji     = getSignEmoji(sign);
                result.description   = json.has("description") ? json.get("description").getAsString() : "";
                result.compatibility = json.has("compatibility") ? json.get("compatibility").getAsString() : "";
                result.mood          = json.has("mood") ? json.get("mood").getAsString() : "";
                result.color         = json.has("color") ? json.get("color").getAsString() : "";
                result.luckyNumber   = json.has("lucky_number") ? json.get("lucky_number").getAsString() : "";
                result.luckyTime     = json.has("lucky_time") ? json.get("lucky_time").getAsString() : "";
                result.dateRange     = json.has("date_range") ? json.get("date_range").getAsString() : "";
                result.wellbeingTip  = generateWellbeingTip(result.mood, sign);
                result.fromApi       = true;
                return result;
            }
        } catch (Exception e) {
            System.err.println("Aztro API error: " + e.getMessage());
        }
        // Fallback si API indisponible
        return getFallbackHoroscope(sign);
    }

    // ══════════════════════════════════════════════════════════
    //  NEWS — Fallback statique haute qualité
    //  (Articles santé mentale soigneusement rédigés)
    // ══════════════════════════════════════════════════════════

    public List<NewsArticle> fetchMentalHealthNews() {
        // On tente d'abord l'API GNews (gratuit, 10 req/jour sans clé)
        List<NewsArticle> articles = tryGNewsApi();
        if (!articles.isEmpty()) return articles;

        // Fallback: articles statiques complets et informatifs
        return getFallbackArticles();
    }

    private List<NewsArticle> tryGNewsApi() {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            String url = "https://gnews.io/api/v4/search?q=sante+mentale&lang=fr&max=6&token=demo";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("articles")) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("articles")) {
                    JsonArray arr = json.getAsJsonArray("articles");
                    for (int i = 0; i < Math.min(arr.size(), 5); i++) {
                        JsonObject a = arr.get(i).getAsJsonObject();
                        NewsArticle article = new NewsArticle();
                        article.title    = a.has("title")       ? a.get("title").getAsString()       : "";
                        article.summary  = a.has("description") ? a.get("description").getAsString() : "";
                        article.source   = a.has("source")      ? a.getAsJsonObject("source").get("name").getAsString() : "Source";
                        article.category = "Actualité";
                        article.categoryColor = "#4A6FA5";
                        article.emoji    = "📰";
                        article.readTime = "3 min";
                        if (!article.title.isEmpty()) articles.add(article);
                    }
                }
            }
        } catch (Exception e) { /* ignore, use fallback */ }
        return articles;
    }

    private List<NewsArticle> getFallbackArticles() {
        List<NewsArticle> articles = new ArrayList<>();

        articles.add(article(
                "L'exercice physique réduit l'anxiété de 48% selon une étude",
                "Une méta-analyse portant sur 97 études confirme que l'activité physique régulière " +
                        "— même 20 minutes de marche par jour — réduit significativement les symptômes " +
                        "anxieux. Les chercheurs recommandent d'intégrer le mouvement comme thérapie complémentaire.",
                "Journal of Psychiatry", "Recherche", "#52B788", "🔬", "4 min"
        ));

        articles.add(article(
                "Méditation pleine conscience : les bienfaits mesurables sur le cerveau",
                "Des chercheurs de Harvard ont observé via IRM que 8 semaines de méditation " +
                        "quotidienne (10-15 min) réduisent le volume de l'amygdale, région du cerveau " +
                        "liée au stress et aux réactions de peur. Le cortex préfrontal, lui, se renforce.",
                "Harvard Medical School", "Neurosciences", "#7B9ED9", "🧠", "5 min"
        ));

        articles.add(article(
                "Sommeil et santé mentale : le rôle crucial du cycle circadien",
                "Dormir entre 7 et 9 heures par nuit stabilise les hormones du stress comme " +
                        "le cortisol et favorise la consolidation émotionnelle. Le manque chronique " +
                        "de sommeil multiplie par 3 le risque de dépression selon l'OMS.",
                "OMS / Sleep Foundation", "Conseils", "#E8956D", "😴", "3 min"
        ));

        articles.add(article(
                "La nature comme thérapie : l'écothérapie gagne du terrain en clinique",
                "Passer 120 minutes par semaine dans des espaces naturels améliore " +
                        "significativement le bien-être mental. Cette pratique, reconnue au Japon " +
                        "sous le nom de 'Shinrin-yoku' (bain de forêt), est désormais prescrite " +
                        "par des médecins en Finlande et au Royaume-Uni.",
                "Nature Medicine", "Tendances", "#52B788", "🌿", "4 min"
        ));

        articles.add(article(
                "Alimentation et dépression : le microbiome intestinal en question",
                "Le lien bidirectionnel entre l'intestin et le cerveau (axe gut-brain) " +
                        "est désormais bien documenté. Un régime riche en fibres, probiotiques " +
                        "et oméga-3 serait associé à une réduction de 30% des épisodes dépressifs " +
                        "légers à modérés.",
                "Gut Microbiome Journal", "Nutrition", "#F5C87A", "🥗", "5 min"
        ));

        articles.add(article(
                "L'art-thérapie comme outil de traitement des traumatismes",
                "De plus en plus utilisée dans les services psychiatriques, l'art-thérapie " +
                        "permet d'exprimer des émotions difficiles sans les verbaliser. Dessin, " +
                        "peinture, sculpture : ces pratiques activent des zones cérébrales " +
                        "inaccessibles par la parole seule.",
                "Art Therapy Journal", "Thérapies", "#E8956D", "🎨", "4 min"
        ));

        return articles;
    }

    private NewsArticle article(String title, String summary, String source,
                                String category, String color, String emoji, String readTime) {
        NewsArticle a = new NewsArticle();
        a.title = title; a.summary = summary; a.source = source;
        a.category = category; a.categoryColor = color;
        a.emoji = emoji; a.readTime = readTime;
        return a;
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private String generateWellbeingTip(String mood, String sign) {
        if (mood == null) mood = "";
        String moodLower = mood.toLowerCase();
        if (moodLower.contains("happy") || moodLower.contains("joyful") || moodLower.contains("excited"))
            return "Votre énergie positive d'aujourd'hui est une ressource précieuse. Partagez-la avec votre entourage ou notez ce qui vous rend heureux dans votre journal.";
        if (moodLower.contains("sad") || moodLower.contains("anxious") || moodLower.contains("stressed"))
            return "Prenez soin de vous aujourd'hui. Une courte séance de respiration (4-7-8) peut aider : inspirez 4s, retenez 7s, expirez 8s. Répétez 3 fois.";
        if (moodLower.contains("calm") || moodLower.contains("peaceful"))
            return "Profitez de cette sérénité pour méditer ou faire une activité créative. C'est le moment idéal pour écrire dans votre journal de bien-être.";
        if (moodLower.contains("energetic") || moodLower.contains("motivated"))
            return "Canalisez cette énergie dans une activité physique ou un projet qui vous tient à cœur. Le mouvement amplifie les émotions positives.";
        // Conseil par défaut selon le signe
        return getDefaultTipForSign(sign);
    }

    private String getDefaultTipForSign(String sign) {
        return switch (sign.toLowerCase()) {
            case "aries"       -> "Bélier : Votre énergie débordante peut parfois créer du stress. Accordez-vous 10 minutes de silence aujourd'hui.";
            case "taurus"      -> "Taureau : Ancré et stable, vous avez besoin de ritualiser votre bien-être. Créez une routine matinale apaisante.";
            case "gemini"      -> "Gémeaux : Votre mental actif mérite du repos. Essayez une déconnexion digitale de 2h ce soir.";
            case "cancer"      -> "Cancer : Votre sensibilité est une force. Entourez-vous de personnes bienveillantes et n'hésitez pas à exprimer vos émotions.";
            case "leo"         -> "Lion : Rayonnez positivement. Faites une action bienveillante pour quelqu'un aujourd'hui — cela nourrit aussi votre bien-être.";
            case "virgo"       -> "Vierge : Le perfectionnisme peut peser. Accordez-vous le droit à l'imperfection et célébrez vos petites victoires.";
            case "libra"       -> "Balance : L'harmonie est votre quête. Prenez le temps d'écouter vos propres besoins avant ceux des autres.";
            case "scorpio"     -> "Scorpion : Votre profondeur émotionnelle est une richesse. L'écriture ou la créativité peut vous aider à canaliser ces énergies.";
            case "sagittarius" -> "Sagittaire : Votre besoin de liberté est légitime. Planifiez une petite escapade ou explorez quelque chose de nouveau.";
            case "capricorn"   -> "Capricorne : Savoir se reposer est aussi une compétence. Autorisez-vous une pause sans culpabilité aujourd'hui.";
            case "aquarius"    -> "Verseau : Votre originalité est précieuse. Partagez vos idées avec votre coach pour nourrir votre épanouissement.";
            case "pisces"      -> "Poissons : Votre empathie est belle mais protégez votre énergie. Apprenez à dire non avec bienveillance.";
            default            -> "Chaque jour est une nouvelle occasion de prendre soin de vous. Respirez profondément et soyez bienveillant envers vous-même.";
        };
    }

    private HoroscopeResult getFallbackHoroscope(String sign) {
        HoroscopeResult r = new HoroscopeResult();
        r.sign         = capitalize(sign);
        r.signEmoji    = getSignEmoji(sign);
        r.dateRange    = getDateRange(sign);
        r.mood         = "Serene";
        r.color        = "Blue";
        r.luckyNumber  = "7";
        r.luckyTime    = "10:00";
        r.compatibility = "Taurus";
        r.description  = "Les astres vous invitent aujourd'hui à la douceur et à l'introspection. " +
                "C'est une journée propice pour reconnaître vos progrès et vous concentrer sur " +
                "ce qui vous apporte une véritable paix intérieure. Vos relations sont au beau fixe.";
        r.wellbeingTip = getDefaultTipForSign(sign);
        r.fromApi      = false;
        return r;
    }

    public String getSignEmoji(String sign) {
        return switch (sign.toLowerCase()) {
            case "aries"       -> "\u2648";
            case "taurus"      -> "\u2649";
            case "gemini"      -> "\u264A";
            case "cancer"      -> "\u264B";
            case "leo"         -> "\u264C";
            case "virgo"       -> "\u264D";
            case "libra"       -> "\u264E";
            case "scorpio"     -> "\u264F";
            case "sagittarius" -> "\u2650";
            case "capricorn"   -> "\u2651";
            case "aquarius"    -> "\u2652";
            case "pisces"      -> "\u2653";
            default            -> "\u2728";
        };
    }

    public String getSignName(String sign) {
        return switch (sign.toLowerCase()) {
            case "aries"       -> "Bélier";
            case "taurus"      -> "Taureau";
            case "gemini"      -> "Gémeaux";
            case "cancer"      -> "Cancer";
            case "leo"         -> "Lion";
            case "virgo"       -> "Vierge";
            case "libra"       -> "Balance";
            case "scorpio"     -> "Scorpion";
            case "sagittarius" -> "Sagittaire";
            case "capricorn"   -> "Capricorne";
            case "aquarius"    -> "Verseau";
            case "pisces"      -> "Poissons";
            default            -> capitalize(sign);
        };
    }

    private String getDateRange(String sign) {
        return switch (sign.toLowerCase()) {
            case "aries"       -> "21 Mars – 19 Avril";
            case "taurus"      -> "20 Avril – 20 Mai";
            case "gemini"      -> "21 Mai – 20 Juin";
            case "cancer"      -> "21 Juin – 22 Juillet";
            case "leo"         -> "23 Juillet – 22 Août";
            case "virgo"       -> "23 Août – 22 Sep.";
            case "libra"       -> "23 Sep. – 22 Oct.";
            case "scorpio"     -> "23 Oct. – 21 Nov.";
            case "sagittarius" -> "22 Nov. – 21 Déc.";
            case "capricorn"   -> "22 Déc. – 19 Jan.";
            case "aquarius"    -> "20 Jan. – 18 Fév.";
            case "pisces"      -> "19 Fév. – 20 Mars";
            default            -> "";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public String getTodayDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
    }
}