package entities;

import java.util.Objects;

public class Reponse {
    private int id;
    private int questionId;
    private String optionText;
    private boolean isCorrect;

    public Reponse() {
    }

    public Reponse(int id, int questionId, String optionText, boolean isCorrect) {
        this.id = id;
        this.questionId = questionId;
        this.optionText = optionText;
        this.isCorrect = isCorrect;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    @Override
    public String toString() {
        return "Reponse{" +
                "id=" + id +
                ", questionId=" + questionId +
                ", optionText='" + optionText + '\'' +
                ", isCorrect=" + isCorrect +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Reponse reponse)) return false;
        return id == reponse.id &&
                questionId == reponse.questionId &&
                Objects.equals(optionText, reponse.optionText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, questionId, optionText);
    }
}