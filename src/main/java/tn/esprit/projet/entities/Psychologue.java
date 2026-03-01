package tn.esprit.projet.entities;

public class Psychologue {
    private int id;
    private String nom;
    private String prenom; // Ajouté
    private String specialite;
    private String email;

    public Psychologue() {}

    // Constructeur complet mis à jour
    public Psychologue(int id, String nom, String prenom, String specialite, String email) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.specialite = specialite;
        this.email = email;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; } // Ajouté
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}