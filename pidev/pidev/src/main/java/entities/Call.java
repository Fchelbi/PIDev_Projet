package entities;

import java.time.LocalDateTime;

public class Call {
    public enum Status { RINGING, ACCEPTED, REJECTED, ENDED, MISSED }

    private int id_call;
    private int id_caller;
    private int id_receiver;
    private Status status;
    private LocalDateTime date_appel;
    private int duree_secondes;
    private String callerIp;
    private int callerPort;

    public Call() { this.date_appel = LocalDateTime.now(); }

    public Call(int id_caller, int id_receiver) {
        this();
        this.id_caller  = id_caller;
        this.id_receiver= id_receiver;
        this.status     = Status.RINGING;
    }

    public int getId_call()              { return id_call; }
    public void setId_call(int v)        { id_call = v; }
    public int getId_caller()            { return id_caller; }
    public void setId_caller(int v)      { id_caller = v; }
    public int getId_receiver()          { return id_receiver; }
    public void setId_receiver(int v)    { id_receiver = v; }
    public Status getStatus()            { return status; }
    public void setStatus(Status v)      { status = v; }
    public LocalDateTime getDate_appel() { return date_appel; }
    public void setDate_appel(LocalDateTime v){ date_appel = v; }
    public int getDuree_secondes()       { return duree_secondes; }
    public void setDuree_secondes(int v) { duree_secondes = v; }
    public String getCallerIp()          { return callerIp; }
    public void setCallerIp(String v)    { callerIp = v; }
    public int getCallerPort()           { return callerPort; }
    public void setCallerPort(int v)     { callerPort = v; }
}