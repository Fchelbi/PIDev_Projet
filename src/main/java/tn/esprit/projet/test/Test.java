package tn.esprit.projet.test;

import tn.esprit.projet.entities.User;
import tn.esprit.projet.services.UserService; // Import du bon service
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.SQLException;

public class Test {

    public static void main(String[] args) {
        // Vérification de la connexion
        MyDBConnexion c1 = MyDBConnexion.getInstance();

        // Création d'un utilisateur compatible avec ta table 'utilisateur'
        // Paramètres : nom, prenom, email, motDePasse
        User u1 = new User("Wassim", "Test", "wassim@esprit.tn", "password123");

        // Utilisation du UserService que nous avons créé
        UserService us = new UserService();

        try {
            // Insertion dans la base pi_dev
            us.ajouterUtilisateur(u1);
            System.out.println("Utilisateur inséré avec succès !");

            // Si tu as une méthode selectAll dans UserService, tu peux l'afficher ici
            // System.out.println(us.selectAll());

        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
        }
    }
}