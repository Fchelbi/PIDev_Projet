package entities;

import java.time.LocalDateTime;

public class Message {
    private int id_message;
    private int id_expediteur;
    private int id_destinataire;
    private String contenu;
    private LocalDateTime date_envoi;
    private boolean lu;

    public Message() { this.date_envoi = LocalDateTime.now(); }

    public Message(int id_expediteur, int id_destinataire, String contenu) {
        this();
        this.id_expediteur   = id_expediteur;
        this.id_destinataire = id_destinataire;
        this.contenu         = contenu;
        this.lu              = false;
    }

    public int getId_message()         { return id_message; }
    public void setId_message(int v)   { this.id_message = v; }
    public int getId_expediteur()      { return id_expediteur; }
    public void setId_expediteur(int v){ this.id_expediteur = v; }
    public int getId_destinataire()    { return id_destinataire; }
    public void setId_destinataire(int v){ this.id_destinataire = v; }
    public String getContenu()         { return contenu; }
    public void setContenu(String v)   { this.contenu = v; }
    public LocalDateTime getDate_envoi(){ return date_envoi; }
    public void setDate_envoi(LocalDateTime v){ this.date_envoi = v; }
    public boolean isLu()              { return lu; }
    public void setLu(boolean v)       { this.lu = v; }
}