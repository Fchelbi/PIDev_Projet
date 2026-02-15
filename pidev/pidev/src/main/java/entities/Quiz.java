package entities;

import java.util.Objects;

public class Quiz {
    private int id;
    private int formationId;
    private String title;
    private int passingScore;

    public Quiz() {
    }

    public Quiz(int id, int formationId, String title, int passingScore) {
        this.id = id;
        this.formationId = formationId;
        this.title = title;
        this.passingScore = passingScore;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFormationId() {
        return formationId;
    }

    public void setFormationId(int formationId) {
        this.formationId = formationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(int passingScore) {
        this.passingScore = passingScore;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", formationId=" + formationId +
                ", title='" + title + '\'' +
                ", passingScore=" + passingScore +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quiz quiz)) return false;
        return id == quiz.id &&
                formationId == quiz.formationId &&
                Objects.equals(title, quiz.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, formationId, title);
    }
}