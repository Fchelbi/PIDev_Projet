package services;

import entities.Reponse;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements CRUD<Reponse> {

    private Connection cnx;

    public ReponseService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Reponse reponse) throws SQLException {
        String req = "INSERT INTO `reponse`(`question_id`, `option_text`, `is_correct`) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, reponse.getQuestionId());
        ps.setString(2, reponse.getOptionText());
        ps.setBoolean(3, reponse.isCorrect());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Reponse reponse) throws SQLException {
        String req = "UPDATE `reponse` SET `question_id`=?, `option_text`=?, `is_correct`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, reponse.getQuestionId());
        ps.setString(2, reponse.getOptionText());
        ps.setBoolean(3, reponse.isCorrect());
        ps.setInt(4, reponse.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Reponse reponse) throws SQLException {
        String req = "DELETE FROM `reponse` WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, reponse.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Reponse> selectALL() throws SQLException {
        List<Reponse> list = new ArrayList<>();
        String req = "SELECT * FROM `reponse`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Reponse(rs.getInt("id"), rs.getInt("question_id"),
                    rs.getString("option_text"), rs.getBoolean("is_correct")));
        }
        return list;
    }

    public List<Reponse> selectByQuestion(int questionId) throws SQLException {
        List<Reponse> list = new ArrayList<>();
        String req = "SELECT * FROM `reponse` WHERE `question_id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, questionId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Reponse(rs.getInt("id"), rs.getInt("question_id"),
                    rs.getString("option_text"), rs.getBoolean("is_correct")));
        }
        return list;
    }
}