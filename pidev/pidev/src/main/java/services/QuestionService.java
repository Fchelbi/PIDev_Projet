package services;

import entities.Question;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements CRUD<Question> {

    private Connection cnx;

    public QuestionService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Question question) throws SQLException {
        String req = "INSERT INTO `question`(`quiz_id`, `question_text`, `points`) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, question.getQuizId());
        ps.setString(2, question.getQuestionText());
        ps.setInt(3, question.getPoints());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Question question) throws SQLException {
        String req = "UPDATE `question` SET `quiz_id`=?, `question_text`=?, `points`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, question.getQuizId());
        ps.setString(2, question.getQuestionText());
        ps.setInt(3, question.getPoints());
        ps.setInt(4, question.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Question question) throws SQLException {
        String req = "DELETE FROM `question` WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, question.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Question> selectALL() throws SQLException {
        List<Question> list = new ArrayList<>();
        String req = "SELECT * FROM `question`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Question(rs.getInt("id"), rs.getInt("quiz_id"),
                    rs.getString("question_text"), rs.getInt("points")));
        }
        return list;
    }

    public List<Question> selectByQuiz(int quizId) throws SQLException {
        List<Question> list = new ArrayList<>();
        String req = "SELECT * FROM `question` WHERE `quiz_id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, quizId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Question(rs.getInt("id"), rs.getInt("quiz_id"),
                    rs.getString("question_text"), rs.getInt("points")));
        }
        return list;
    }
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM question";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) return rs.getInt(1);
        return 0;
    }
}