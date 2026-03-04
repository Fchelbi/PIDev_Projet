package entities;

import java.time.LocalDateTime;

public class Rapport {
    private int id_rapport;
    private int id_patient;
    private int id_coach;
    private String contenu;
    private String recommandations;
    private int nb_seances;
    private double score_humeur;
    private String periode;
    private LocalDateTime date_creation;
    private String fichier_pdf;

    public Rapport() {
        this.date_creation = LocalDateTime.now();
    }

    public Rapport(int id_patient, int id_coach, String contenu,
                   String recommandations, int nb_seances,
                   double score_humeur, String periode) {
        this();
        this.id_patient = id_patient;
        this.id_coach = id_coach;
        this.contenu = contenu;
        this.recommandations = recommandations;
        this.nb_seances = nb_seances;
        this.score_humeur = score_humeur;
        this.periode = periode;
    }

    // Getters & Setters
    public int getId_rapport() { return id_rapport; }
    public void setId_rapport(int id) { this.id_rapport = id; }
    public int getId_patient() { return id_patient; }
    public void setId_patient(int id) { this.id_patient = id; }
    public int getId_coach() { return id_coach; }
    public void setId_coach(int id) { this.id_coach = id; }
    public String getContenu() { return contenu; }
    public void setContenu(String c) { this.contenu = c; }
    public String getRecommandations() { return recommandations; }
    public void setRecommandations(String r) { this.recommandations = r; }
    public int getNb_seances() { return nb_seances; }
    public void setNb_seances(int n) { this.nb_seances = n; }
    public double getScore_humeur() { return score_humeur; }
    public void setScore_humeur(double s) { this.score_humeur = s; }
    public String getPeriode() { return periode; }
    public void setPeriode(String p) { this.periode = p; }
    public LocalDateTime getDate_creation() { return date_creation; }
    public void setDate_creation(LocalDateTime d) { this.date_creation = d; }
    public String getFichier_pdf() { return fichier_pdf; }
    public void setFichier_pdf(String f) { this.fichier_pdf = f; }

    @Override
    public String toString() {
        return "Rapport{patient=" + id_patient + ", coach=" + id_coach +
                ", seances=" + nb_seances + ", humeur=" + score_humeur + "}";
    }
}
