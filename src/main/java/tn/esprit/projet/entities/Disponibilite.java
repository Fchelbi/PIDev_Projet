package tn.esprit.projet.entities;
import java.sql.Date;
import java.sql.Time;

public class Disponibilite {
    private int id, psychologueId;
    private Date jour;
    private Time heureDebut, heureFin;
    private String statut;

    public Disponibilite(int id, int psychologueId, Date jour, Time heureDebut, Time heureFin) {
        this.id = id;
        this.psychologueId = psychologueId;
        this.jour = jour;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
    }

    public int getPsychologueId() { return psychologueId; }
    public Date getJour() { return jour; }
    public Time getHeureDebut() { return heureDebut; }
    public Time getHeureFin() { return heureFin; }
}