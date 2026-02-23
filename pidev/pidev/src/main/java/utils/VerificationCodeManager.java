package utils;

import entities.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des codes de vérification 2FA
 */
public class VerificationCodeManager {

    private static Map<String, CodeData> activeCodes = new HashMap<>();

    /**
     * Classe interne pour stocker code + expiry + user
     */
    public static class CodeData {
        private String code;
        private LocalDateTime expiry;
        private User user;

        public CodeData(String code, LocalDateTime expiry, User user) {
            this.code = code;
            this.expiry = expiry;
            this.user = user;
        }

        public String getCode() { return code; }
        public LocalDateTime getExpiry() { return expiry; }
        public User getUser() { return user; }
    }

    /**
     * Stocker code pour un email
     */
    public static void storeCode(String email, String code, User user) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        activeCodes.put(email.toLowerCase(), new CodeData(code, expiry, user));
        System.out.println("✅ Code stocké pour: " + email + " (expire: " + expiry + ")");
    }

    /**
     * Vérifier si code est valide
     */
    public static boolean verifyCode(String email, String inputCode) {
        CodeData data = activeCodes.get(email.toLowerCase());

        if (data == null) {
            System.out.println("❌ Aucun code trouvé pour: " + email);
            return false;
        }

        // Vérifier expiration
        if (LocalDateTime.now().isAfter(data.getExpiry())) {
            activeCodes.remove(email.toLowerCase());
            System.out.println("❌ Code expiré pour: " + email);
            return false;
        }

        // Vérifier code
        boolean isValid = data.getCode().equals(inputCode);
        System.out.println(isValid ? "✅ Code valide!" : "❌ Code incorrect");

        return isValid;
    }

    /**
     * Récupérer user associé au code
     */
    public static User getUser(String email) {
        CodeData data = activeCodes.get(email.toLowerCase());
        return data != null ? data.getUser() : null;
    }

    /**
     * Supprimer code après utilisation
     */
    public static void removeCode(String email) {
        activeCodes.remove(email.toLowerCase());
        System.out.println("🗑️ Code supprimé pour: " + email);
    }

    /**
     * Nettoyer codes expirés (à appeler périodiquement)
     */
    public static void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        activeCodes.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiry()));
        System.out.println("🧹 Nettoyage codes expirés effectué");
    }

    /**
     * Obtenir nombre de codes actifs
     */
    public static int getActiveCodesCount() {
        return activeCodes.size();
    }
}