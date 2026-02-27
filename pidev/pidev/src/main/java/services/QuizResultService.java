package services;

import entities.QuizResult;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizResultService implements CRUD<QuizResult> {

    private Connection cnx;

    public QuizResultService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(QuizResult result) throws SQLException {
        String req = "INSERT INTO `quiz_result`(`quiz_id`, `user_id`, `score`, " +
                "`total_points`, `passed`, `completed_at`) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, result.getQuizId());
        ps.setInt(2, result.getUserId());
        ps.setInt(3, result.getScore());
        ps.setInt(4, result.getTotalPoints());
        ps.setBoolean(5, result.isPassed());
        ps.setTimestamp(6, Timestamp.valueOf(result.getCompletedAt()));
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) {
            result.setId(keys.getInt(1));
        }
    }

    @Override
    public void updateOne(QuizResult result) throws SQLException {
        String req = "UPDATE `quiz_result` SET `score`=?, `total_points`=?, " +
                "`passed`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, result.getScore());
        ps.setInt(2, result.getTotalPoints());
        ps.setBoolean(3, result.isPassed());
        ps.setInt(4, result.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(QuizResult result) throws SQLException {
        String req = "DELETE FROM `quiz_result` WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, result.getId());
        ps.executeUpdate();
    }

    @Override
    public List<QuizResult> selectALL() throws SQLException {
        List<QuizResult> list = new ArrayList<>();
        String req = "SELECT * FROM `quiz_result` ORDER BY `completed_at` DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(mapResult(rs));
        }
        return list;
    }

    public List<QuizResult> selectByUser(int userId) throws SQLException {
        List<QuizResult> list = new ArrayList<>();
        String req = "SELECT * FROM `quiz_result` WHERE `user_id`=? " +
                "ORDER BY `completed_at` DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResult(rs));
        }
        return list;
    }

    public List<QuizResult> selectByQuiz(int quizId) throws SQLException {
        List<QuizResult> list = new ArrayList<>();
        String req = "SELECT * FROM `quiz_result` WHERE `quiz_id`=? " +
                "ORDER BY `completed_at` DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, quizId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResult(rs));
        }
        return list;
    }

    public QuizResult selectBestByUserAndQuiz(int userId, int quizId) throws SQLException {
        String req = "SELECT * FROM `quiz_result` WHERE `user_id`=? AND `quiz_id`=? " +
                "ORDER BY `score` DESC LIMIT 1";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setInt(2, quizId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapResult(rs);
        return null;
    }

    public boolean hasUserPassedQuiz(int userId, int quizId) throws SQLException {
        String req = "SELECT COUNT(*) FROM `quiz_result` WHERE `user_id`=? " +
                "AND `quiz_id`=? AND `passed`=1";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setInt(2, quizId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }

    private QuizResult mapResult(ResultSet rs) throws SQLException {
        return new QuizResult(
                rs.getInt("id"),
                rs.getInt("quiz_id"),
                rs.getInt("user_id"),
                rs.getInt("score"),
                rs.getInt("total_points"),
                rs.getBoolean("passed"),
                rs.getTimestamp("completed_at") != null ?
                        rs.getTimestamp("completed_at").toLocalDateTime() : null
        );
    }
}