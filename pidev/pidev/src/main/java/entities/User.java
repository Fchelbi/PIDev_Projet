package entities;

import java.util.Objects;

public class User {
    private int id_user;
    private String nom;
    private String prenom;
    private String email;
    private String mdp;
    private String role;
    private String num_tel;
    private String photo; // ✅ NOUVEAU

    public User() {}

    public User(int id_user, String nom, String prenom, String email, String mdp, String role, String num_tel) {
        this.id_user = id_user;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.mdp = mdp;
        this.role = role;
        this.num_tel = num_tel;
        this.photo = null;
    }

    // ✅ Constructeur avec photo
    public User(int id_user, String nom, String prenom, String email, String mdp, String role, String num_tel, String photo) {
        this(id_user, nom, prenom, email, mdp, role, num_tel);
        this.photo = photo;
    }

    public int getId_user() { return id_user; }
    public void setId_user(int id_user) { this.id_user = id_user; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMdp() { return mdp; }
    public void setMdp(String mdp) { this.mdp = mdp; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getNum_tel() { return num_tel; }
    public void setNum_tel(String num_tel) { this.num_tel = num_tel; }

    // ✅ NOUVEAU
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    @Override
    public String toString() {
        return "User{id=" + id_user + ", nom='" + nom + "', prenom='" + prenom +
                "', email='" + email + "', role='" + role + "', photo='" + photo + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return id_user == user.id_user && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() { return Objects.hash(id_user, email); }
}