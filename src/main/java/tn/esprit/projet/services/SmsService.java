package tn.esprit.projet.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SmsService {

    // ⚠️ REMPLACE PAR TES VALEURS
    private static final String ACCOUNT_SID = "ACd6a636fad3cc49b59bcbbc225d9713a3";
    private static final String AUTH_TOKEN = "d25cc1c56875f4ae29740640081afb7a";
    private static final String FROM_NUMBER = "+17755005336"; // numéro Twilio


    static {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public static void envoyerSMS(String to, String message) {
        try {
            Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(FROM_NUMBER),
                    message
            ).create();

            System.out.println("SMS envoyé avec succès !");
        } catch (Exception e) {
            System.err.println("Erreur envoi SMS: " + e.getMessage());
        }
    }
}