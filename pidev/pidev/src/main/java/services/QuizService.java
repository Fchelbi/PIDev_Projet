package services;

import entities.Quiz;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService implements CRUD<Quiz> {

    private Connection cnx;

    public QuizService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Quiz quiz) throws SQLException {
        String req = "INSERT INTO `quiz`(`formation_id`, `title`, `passing_score`) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, quiz.getFormationId());
        ps.setString(2, quiz.getTitle());
        ps.setInt(3, quiz.getPassingScore());
        ps.executeUpdate();
        System.out.println("Quiz added successfully!");
    }

    @Override
    public void updateOne(Quiz quiz) throws SQLException {
        String req = "UPDATE `quiz` SET `formation_id` = ?, `title` = ?, `passing_score` = ? WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, quiz.getFormationId());
        ps.setString(2, quiz.getTitle());
        ps.setInt(3, quiz.getPassingScore());
        ps.setInt(4, quiz.getId());
        ps.executeUpdate();
        System.out.println("Quiz updated successfully!");
    }

    @Override
    public void deleteOne(Quiz quiz) throws SQLException {
        String req = "DELETE FROM `quiz` WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, quiz.getId());
        ps.executeUpdate();
        System.out.println("Quiz deleted successfully!");
    }

    @Override
    public List<Quiz> selectALL() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        String req = "SELECT * FROM `quiz`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Quiz q = new Quiz(
                    rs.getInt("id"),
                    rs.getInt("formation_id"),
                    rs.getString("title"),
                    rs.getInt("passing_score")
            );
            list.add(q);
        }
        return list;
    }

    // Get quiz by formation
    public Quiz selectByFormation(int formationId) throws SQLException {
        String req = "SELECT * FROM `quiz` WHERE `formation_id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, formationId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Quiz(
                    rs.getInt("id"),
                    rs.getInt("formation_id"),
                    rs.getString("title"),
                    rs.getInt("passing_score")
            );
        }
        return null;
    }

    // Check if formation already has quiz
    public boolean formationHasQuiz(int formationId) throws SQLException {
        String req = "SELECT COUNT(*) FROM `quiz` WHERE `formation_id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, formationId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }
}