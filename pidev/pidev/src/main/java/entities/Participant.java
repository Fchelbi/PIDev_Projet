package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Participant {
    private int id;
    private int userId;
    private int formationId;
    private LocalDateTime dateInscription;

    public Participant() {
    }

    public Participant(int id, int userId, int formationId, LocalDateTime dateInscription) {
        this.id = id;
        this.userId = userId;
        this.formationId = formationId;
        this.dateInscription = dateInscription;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + id +
                ", userId=" + userId +
                ", formationId=" + formationId +
                ", dateInscription=" + dateInscription +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Participant that)) return false;
        return id == that.id &&
                userId == that.userId &&
                formationId == that.formationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, formationId);
    }
}