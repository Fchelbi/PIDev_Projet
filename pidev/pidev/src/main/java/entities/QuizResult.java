package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class QuizResult {
    private int id;
    private int quizId;
    private int userId;
    private int score;
    private int totalPoints;
    private boolean passed;
    private LocalDateTime completedAt;

    public QuizResult() {
        this.completedAt = LocalDateTime.now();
    }

    public QuizResult(int id, int quizId, int userId, int score,
                      int totalPoints, boolean passed, LocalDateTime completedAt) {
        this.id = id;
        this.quizId = quizId;
        this.userId = userId;
        this.score = score;
        this.totalPoints = totalPoints;
        this.passed = passed;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public double getPercentage() {
        if (totalPoints == 0) return 0;
        return (score * 100.0) / totalPoints;
    }

    @Override
    public String toString() {
        return "QuizResult{id=" + id + ", score=" + score + "/" + totalPoints +
                ", passed=" + passed + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QuizResult r)) return false;
        return id == r.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}