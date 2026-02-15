package services;

import entities.Formation;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationService implements CRUD<Formation> {

    private Connection cnx;

    public FormationService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Formation formation) throws SQLException {
        String req = "INSERT INTO `formation`(`title`, `description`, `video_url`, `category`) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, formation.getTitle());
        ps.setString(2, formation.getDescription());
        ps.setString(3, formation.getVideoUrl());
        ps.setString(4, formation.getCategory());
        ps.executeUpdate();
        System.out.println("Formation added successfully!");
    }

    @Override
    public void updateOne(Formation formation) throws SQLException {
        String req = "UPDATE `formation` SET `title` = ?, `description` = ?, `video_url` = ?, `category` = ? WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, formation.getTitle());
        ps.setString(2, formation.getDescription());
        ps.setString(3, formation.getVideoUrl());
        ps.setString(4, formation.getCategory());
        ps.setInt(5, formation.getId());
        ps.executeUpdate();
        System.out.println("Formation updated successfully!");
    }

    @Override
    public void deleteOne(Formation formation) throws SQLException {
        String req = "DELETE FROM `formation` WHERE `id` = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, formation.getId());
        ps.executeUpdate();
        System.out.println("Formation deleted successfully!");
    }

    @Override
    public List<Formation> selectALL() throws SQLException {
        List<Formation> list = new ArrayList<>();
        String req = "SELECT * FROM `formation`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Formation f = new Formation(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("video_url"),
                    rs.getString("category")
            );
            list.add(f);
        }
        return list;
    }
}