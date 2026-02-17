package services;

import entities.User;
import utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class serviceUser implements CRUD<User> {
    private final Connection cnx;

    public serviceUser() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(User user) throws SQLException {
        String req = "INSERT INTO `user`(`nom`, `prenom`, `email`, `mdp`, `role`, `num_tel`, `photo`) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getMdp());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getNum_tel());
        ps.setString(7, user.getPhoto());
        ps.executeUpdate();
    }

    @Override
    public void updateOne(User user) throws SQLException {
        String req = "UPDATE `user` SET `nom`=?, `prenom`=?, `email`=?, `mdp`=?, `role`=?, `num_tel`=?, `photo`=? WHERE `id_user`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getMdp());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getNum_tel());
        ps.setString(7, user.getPhoto());
        ps.setInt(8, user.getId_user());
        ps.executeUpdate();
    }

    @Override
    public void deleteOne(User user) throws SQLException {
        String req = "DELETE FROM `user` WHERE `id_user`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, user.getId_user());
        ps.executeUpdate();
    }

    @Override
    public List<User> selectALL() throws SQLException {
        List<User> userList = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            User u = new User();
            u.setId_user(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setMdp(rs.getString("mdp"));
            u.setRole(rs.getString("role"));
            u.setNum_tel(rs.getString("num_tel"));
            u.setPhoto(rs.getString("photo"));
            userList.add(u);
        }
        return userList;
    }

    public User login(String email, String mdp) {
        String req = "SELECT * FROM `user` WHERE email = ? AND mdp = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ps.setString(2, mdp);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId_user(rs.getInt("id_user"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setMdp(rs.getString("mdp"));
                u.setRole(rs.getString("role"));
                u.setNum_tel(rs.getString("num_tel"));
                u.setPhoto(rs.getString("photo"));
                return u;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public boolean emailExists(String email) {
        String req = "SELECT * FROM `user` WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    public void signUp(User user) throws SQLException {
        if (emailExists(user.getEmail())) {
            throw new SQLException("Cet email est déjà utilisé !");
        }
        insertOne(user);
    }
}