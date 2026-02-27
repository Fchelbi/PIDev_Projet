package mains;

import entities.User;
import services.serviceUser;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        MainFX.main(args);
        serviceUser su = new serviceUser();

        // 1. Test Ajouter (INSERT)
        User u1 = new User(0, "TestNom", "TestPrenom", "test2@email.com", "pass123", "PATIENT", "22222222");
        try {
            su.insertOne(u1);
            // Kenouslek houni ma3neha l'ajout mcha mriguel
            System.out.println("✅ User Ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur Ajout : " + e.getMessage());
        }

        // 2. Test Afficher (SELECT)
        try {
            List<User> users = su.selectALL();
            System.out.println("\n📋 Liste des utilisateurs :");
            // Boucle for bch nwarriw l users lkol
            for (User u : users) {
                System.out.println(u.toString());
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur Affichage : " + e.getMessage());
        }
    }
}