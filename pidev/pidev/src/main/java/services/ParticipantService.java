package services;

import entities.Participant;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantService implements CRUD<Participant> {

    private Connection cnx;

    public ParticipantService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Participant participant) throws SQLException {
        String req = "INSERT INTO `participant`(`user_id`, `formation_id`) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, participant.getUserId());
        ps.setInt(2, participant.getFormationId());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(Participant participant) throws SQLException {
        String req = "UPDATE `participant` SET `user_id`=?, `formation_id`=? WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, participant.getUserId());
        ps.setInt(2, participant.getFormationId());
        ps.setInt(3, participant.getId());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(Participant participant) throws SQLException {
        String req = "DELETE FROM `participant` WHERE `id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, participant.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Participant> selectALL() throws SQLException {
        List<Participant> list = new ArrayList<>();
        String req = "SELECT * FROM `participant`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new Participant(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getInt("formation_id"),
                    rs.getTimestamp("date_inscription") != null ?
                            rs.getTimestamp("date_inscription").toLocalDateTime() : null
            ));
        }
        return list;
    }

    public List<Participant> selectByFormation(int formationId) throws SQLException {
        List<Participant> list = new ArrayList<>();
        String req = "SELECT * FROM `participant` WHERE `formation_id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, formationId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new Participant(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getInt("formation_id"),
                    rs.getTimestamp("date_inscription") != null ?
                            rs.getTimestamp("date_inscription").toLocalDateTime() : null
            ));
        }
        return list;
    }

    public boolean isAlreadyRegistered(int userId, int formationId) throws SQLException {
        String req = "SELECT COUNT(*) FROM `participant` WHERE `user_id`=? AND `formation_id`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ps.setInt(2, formationId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }
}