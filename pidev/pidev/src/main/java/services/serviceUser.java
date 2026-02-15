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
        String req = "INSERT INTO `user`(`nom`, `prenom`, `email`, `mdp`, `role`, `num_tel`) VALUES (?, ?, ?, ?, ?, ?)";


        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getMdp());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getNum_tel());

        ps.executeUpdate();
        System.out.println("User ajouté !");
    }

    @Override
    public void updateOne(User user) throws SQLException {
        String req = "UPDATE `user` SET `nom`=?, `prenom`=?, `email`=?, `mdp`=?, `role`=?, `num_tel`=? WHERE `id_user`=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getMdp());
        ps.setString(5, user.getRole());
        ps.setString(6, user.getNum_tel());
        ps.setInt(7, user.getId_user());

        ps.executeUpdate();
        System.out.println("User modifié !");
    }

    @Override
    public void deleteOne(User user) throws SQLException {
        String req = "DELETE FROM `user` WHERE `id_user`=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, user.getId_user());
        ps.executeUpdate();
        System.out.println("User supprimé !");
    }

    @Override
    public List<User> selectALL() throws SQLException {
        List<User> userList = new ArrayList<>();
        String req = "SELECT * FROM `user`";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            User u = new User();
            // Thabet f asmewi les colonnes fil base de données mte3ek
            u.setId_user(rs.getInt("id_user"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setEmail(rs.getString("email"));
            u.setMdp(rs.getString("mdp"));
            u.setRole(rs.getString("role"));
            u.setNum_tel(rs.getString("num_tel"));

            userList.add(u);
        }

        return userList;
    }
    // Méthode bch thabbet user mawjoud walla lé (Login)
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
                return u;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null; // Ken ma 9ahouch yraja3 null
    }

    // Méthode bch thabbet eske l email déjà utilisé (pour Sign Up)
    public boolean emailExists(String email) {
        String req = "SELECT * FROM `user` WHERE email = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // Traja3 true ken l9at l email
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return false;
    }
    public void signUp(User user) throws SQLException {

        // 1. Etape de vérification (Validation métier)
        if (emailExists(user.getEmail())) {
            // Ken l email mawjoud, nwa9fou l khedma w nkharjou erreur
            throw new SQLException("Echec Inscription : Cet email est déjà utilisé !");
        }

        // 2. Ken l email ndhif, n3aytou l méthode l CRUD bch nsajlou
        insertOne(user);

        System.out.println("Compte créé avec succès pour : " + user.getNom());
    }
}