package utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des codes de réinitialisation de mot de passe
 */
public class PasswordResetManager {

    private static Map<String, ResetData> resetCodes = new HashMap<>();

    /**
     * Classe interne pour stocker code + expiry
     */
    public static class ResetData {
        private String code;
        private LocalDateTime expiry;

        public ResetData(String code, LocalDateTime expiry) {
            this.code = code;
            this.expiry = expiry;
        }

        public String getCode() { return code; }
        public LocalDateTime getExpiry() { return expiry; }
    }

    /**
     * Stocker code reset pour un email
     */
    public static void storeCode(String email, String code) {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        resetCodes.put(email.toLowerCase(), new ResetData(code, expiry));
        System.out.println("✅ Code reset stocké pour: " + email);
    }

    /**
     * Vérifier code reset
     */
    public static boolean verifyCode(String email, String inputCode) {
        ResetData data = resetCodes.get(email.toLowerCase());

        if (data == null) {
            System.out.println("❌ Aucun code reset pour: " + email);
            return false;
        }

        if (LocalDateTime.now().isAfter(data.getExpiry())) {
            resetCodes.remove(email.toLowerCase());
            System.out.println("❌ Code reset expiré");
            return false;
        }

        return data.getCode().equals(inputCode);
    }

    /**
     * Supprimer code après utilisation
     */
    public static void removeCode(String email) {
        resetCodes.remove(email.toLowerCase());
        System.out.println("🗑️ Code reset supprimé");
    }

    /**
     * Nettoyer codes expirés
     */
    public static void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        resetCodes.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiry()));
    }
}