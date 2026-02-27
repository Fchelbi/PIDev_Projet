package services;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Random;

public class Emailservice {

    // ✅ IMPORTANT: Remplace par ton vrai email Gmail et App Password
    private static final String FROM_EMAIL = "emnaboughoufa123@gmail.com";
    private static final String APP_PASSWORD = "glrc qmde vgou roan"; // App Password Gmail (16 chars)

    public static String generateCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    public static boolean send2FACode(String toEmail, String code, String userName) {
        String subject = "🔐 Code de vérification";
        String body = buildHTML2FA(code, userName);
        return sendEmail(toEmail, subject, body);
    }

    public static boolean sendPasswordResetCode(String toEmail, String code, String userName) {
        String subject = "🔑 Réinitialisation mot de passe";
        String body = buildHTMLReset(code, userName);
        return sendEmail(toEmail, subject, body);
    }

    private static String buildHTML2FA(String code, String userName) {
        return "<!DOCTYPE html><html><head><style>"
                + "body{font-family:Arial;background:#f5f7fa;padding:40px;margin:0}"
                + ".container{max-width:600px;margin:0 auto;background:white;border-radius:20px;box-shadow:0 10px 40px rgba(0,0,0,0.1)}"
                + ".header{background:linear-gradient(135deg,#A7B5E0,#D4A5BD);padding:40px;text-align:center}"
                + ".header h1{color:white;margin:0;font-size:28px}"
                + ".content{padding:40px}"
                + ".code-box{background:linear-gradient(135deg,#A7B5E0,#D4A5BD);color:white;font-size:42px;font-weight:bold;text-align:center;padding:30px;border-radius:15px;letter-spacing:10px;margin:30px 0}"
                + ".message{color:#4A5568;font-size:16px;margin:20px 0}"
                + ".warning{background:#FFF5F5;border-left:4px solid #E57373;padding:20px;color:#E57373;margin:25px 0}"
                + ".footer{background:#F7FAFC;padding:30px;text-align:center;color:#A0AEC0;font-size:13px}"
                + "</style></head><body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h1>🔐 Code de Vérification</h1></div>"
                + "<div class=\"content\">"
                + "<p class=\"message\">Bonjour <strong>" + userName + "</strong>,</p>"
                + "<p class=\"message\">Votre code de vérification :</p>"
                + "<div class=\"code-box\">" + code + "</div>"
                + "<p class=\"message\">Valide pendant <strong>10 minutes</strong>.</p>"
                + "<div class=\"warning\">⚠️ Ne partagez jamais ce code.</div>"
                + "</div>"
                + "<div class=\"footer\"><p>© 2024 Application</p></div>"
                + "</div></body></html>";
    }

    private static String buildHTMLReset(String code, String userName) {
        return "<!DOCTYPE html><html><head><style>"
                + "body{font-family:Arial;background:#f5f7fa;padding:40px;margin:0}"
                + ".container{max-width:600px;margin:0 auto;background:white;border-radius:20px;box-shadow:0 10px 40px rgba(0,0,0,0.1)}"
                + ".header{background:linear-gradient(135deg,#81C995,#A7D8B0);padding:40px;text-align:center}"
                + ".header h1{color:white;margin:0;font-size:28px}"
                + ".content{padding:40px}"
                + ".code-box{background:linear-gradient(135deg,#81C995,#A7D8B0);color:white;font-size:42px;font-weight:bold;text-align:center;padding:30px;border-radius:15px;letter-spacing:10px;margin:30px 0}"
                + ".message{color:#4A5568;font-size:16px;margin:20px 0}"
                + ".warning{background:#FFF5F5;border-left:4px solid #E57373;padding:20px;color:#E57373;margin:25px 0}"
                + ".footer{background:#F7FAFC;padding:30px;text-align:center;color:#A0AEC0;font-size:13px}"
                + "</style></head><body>"
                + "<div class=\"container\">"
                + "<div class=\"header\"><h1>🔑 Réinitialisation</h1></div>"
                + "<div class=\"content\">"
                + "<p class=\"message\">Bonjour <strong>" + userName + "</strong>,</p>"
                + "<p class=\"message\">Code de réinitialisation :</p>"
                + "<div class=\"code-box\">" + code + "</div>"
                + "<p class=\"message\">Valide pendant <strong>15 minutes</strong>.</p>"
                + "<div class=\"warning\">⚠️ Contactez-nous si vous n'avez rien demandé.</div>"
                + "</div>"
                + "<div class=\"footer\"><p>© 2024 Application</p></div>"
                + "</div></body></html>";
    }

    private static boolean sendEmail(String toEmail, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé: " + toEmail);
            return true;
        } catch (MessagingException e) {
            System.err.println("❌ Erreur email: " + e.getMessage());
            return false;
        }
    }
}