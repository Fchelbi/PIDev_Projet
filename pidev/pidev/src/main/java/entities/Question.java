package entities;

import java.util.Objects;

public class Question {
    private int id;
    private int quizId;
    private String questionText;
    private int points;

    public Question() {}

    public Question(int id, int quizId, String questionText, int points) {
        this.id = id;
        this.quizId = quizId;
        this.questionText = questionText;
        this.points = points;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    @Override
    public String toString() {
        return "Question{id=" + id + ", text='" + questionText + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Question q)) return false;
        return id == q.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}