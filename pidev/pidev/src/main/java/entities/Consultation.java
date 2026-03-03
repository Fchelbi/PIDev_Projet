package entities;

public class Consultation {

    private int id;
    private int utilisateurId;
    private int psychologueId;
    private String dateConsultation;
    private String statut;

    // constructeur vide
    public Consultation() {}

    // constructeur complet
    public Consultation(int id, int utilisateurId, int psychologueId,
                        String dateConsultation, String statut) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.psychologueId = psychologueId;
        this.dateConsultation = dateConsultation;
        this.statut = statut;
    }

    // getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public int getPsychologueId() { return psychologueId; }
    public void setPsychologueId(int psychologueId) { this.psychologueId = psychologueId; }

    public String getDateConsultation() { return dateConsultation; }
    public void setDateConsultation(String dateConsultation) { this.dateConsultation = dateConsultation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
