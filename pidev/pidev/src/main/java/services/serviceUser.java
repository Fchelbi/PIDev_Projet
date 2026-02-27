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
    // ✅ AJOUTER CES MÉTHODES À serviceUser.java

    /**
     * Récupérer user par ID
     */
    public User getUserById(int userId) throws SQLException {
        String req = "SELECT * FROM user WHERE id_user = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
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
        return null;
    }

    /**
     * Récupérer user par email
     */
    public User getUserByEmail(String email) throws SQLException {
        String req = "SELECT * FROM user WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, email);
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
        return null;
    }

    /**
     * Mettre à jour mot de passe
     */
    public void updatePassword(String email, String newPassword) throws SQLException {
        String req = "UPDATE user SET mdp = ? WHERE email = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, newPassword);
        ps.setString(2, email);
        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("✅ Mot de passe mis à jour pour: " + email);
        } else {
            System.out.println("❌ Aucun user trouvé avec email: " + email);
        }
    }

    /**
     * Vérifie si un email est déjà utilisé par un AUTRE utilisateur (pour mise à jour profil)
     */
    public boolean emailExistsForOther(String email, int excludeUserId) {
        String req = "SELECT id_user FROM user WHERE email = ? AND id_user != ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ps.setInt(2, excludeUserId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    /**
     * Vérifie si un mot de passe est déjà utilisé par un AUTRE utilisateur (pour mise à jour profil)
     */
    public boolean passwordExistsForOther(String mdp, int excludeUserId) {
        String req = "SELECT id_user FROM user WHERE mdp = ? AND id_user != ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, mdp);
            ps.setInt(2, excludeUserId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    /**
     * Vérifie si un mot de passe est déjà utilisé (pour inscription)
     */
    public boolean passwordExists(String mdp) {
        String req = "SELECT id_user FROM user WHERE mdp = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, mdp);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }
}