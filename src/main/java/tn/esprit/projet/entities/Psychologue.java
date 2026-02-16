package tn.esprit.projet.entities;

public class Psychologue {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String specialite;

    // Default Constructor
    public Psychologue() {}

    // Parameterized Constructor (Fixes error in image_f3bd04.jpg)
    public Psychologue(int id, String nom, String prenom, String email, String specialite) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.specialite = specialite;
    }

    // Getters and Setters (Resolves error in image_f25460.jpg)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }
}