package entities;

import java.time.LocalDateTime;

public class Message {
    public enum Type { TEXT, CALL_IN, CALL_OUT, CALL_MISSED }

    private int id_message;
    private int id_expediteur;
    private int id_destinataire;
    private String contenu;
    private LocalDateTime date_envoi;
    private boolean lu;
    private boolean modifie;
    private Type type;

    public Message() {
        this.date_envoi = LocalDateTime.now();
        this.type = Type.TEXT;
    }

    public Message(int id_expediteur, int id_destinataire, String contenu) {
        this();
        this.id_expediteur   = id_expediteur;
        this.id_destinataire = id_destinataire;
        this.contenu         = contenu;
        this.lu              = false;
        this.modifie         = false;
    }

    public int getId_message()           { return id_message; }
    public void setId_message(int v)     { id_message = v; }
    public int getId_expediteur()        { return id_expediteur; }
    public void setId_expediteur(int v)  { id_expediteur = v; }
    public int getId_destinataire()      { return id_destinataire; }
    public void setId_destinataire(int v){ id_destinataire = v; }
    public String getContenu()           { return contenu; }
    public void setContenu(String v)     { contenu = v; }
    public LocalDateTime getDate_envoi() { return date_envoi; }
    public void setDate_envoi(LocalDateTime v){ date_envoi = v; }
    public boolean isLu()                { return lu; }
    public void setLu(boolean v)         { lu = v; }
    public boolean isModifie()           { return modifie; }
    public void setModifie(boolean v)    { modifie = v; }
    public Type getType()                { return type; }
    public void setType(Type v)          { type = v; }
}