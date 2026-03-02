package utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hachage mot de passe avec SHA-256 + sel aléatoire
 * Format stocké: BASE64(salt):BASE64(hash)
 * Compatible sans dépendance externe
 */
public class Passwordutil {

    private static final int SALT_BYTES = 16;

    /** Hacher un mot de passe → format "salt:hash" */
    public static String hash(String password) {
        try {
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            rng.nextBytes(salt);
            byte[] hash = sha256(salt, password);
            return Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur hachage: " + e.getMessage(), e);
        }
    }

    /** Vérifier un mot de passe contre le hash stocké */
    public static boolean verify(String password, String stored) {
        try {
            // Ancien mot de passe en clair (migration) → accepter temporairement
            if (!stored.contains(":")) return stored.equals(password);
            String[] parts = stored.split(":", 2);
            byte[] salt    = Base64.getDecoder().decode(parts[0]);
            byte[] expected= Base64.getDecoder().decode(parts[1]);
            byte[] actual  = sha256(salt, password);
            // Comparaison constante (anti timing attack)
            if (expected.length != actual.length) return false;
            int diff = 0;
            for (int i = 0; i < expected.length; i++) diff |= (expected[i] ^ actual[i]);
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Détecter si un mot de passe est déjà haché (contient ":") */
    public static boolean isHashed(String stored) {
        return stored != null && stored.contains(":");
    }

    private static byte[] sha256(byte[] salt, String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return md.digest(password.getBytes("UTF-8"));
    }
}