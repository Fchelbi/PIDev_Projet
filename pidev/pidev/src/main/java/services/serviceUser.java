package services;

import entities.User;
import utils.MyDBConnexion;
import utils.Passwordutil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class serviceUser implements CRUD<User> {
    private final Connection cnx;

    public serviceUser() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    // ── CRUD ─────────────────────────────────────────────────

    @Override
    public void insertOne(User user) throws SQLException {
        // Hacher le mot de passe avant insertion
        String hashedMdp = Passwordutil.isHashed(user.getMdp())
                ? user.getMdp()
                : Passwordutil.hash(user.getMdp());

        String req = "INSERT INTO `user`(`nom`,`prenom`,`email`,`mdp`,`role`,`num_tel`,`photo`) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, hashedMdp);
            ps.setString(5, user.getRole());
            ps.setString(6, user.getNum_tel());
            ps.setString(7, user.getPhoto());
            ps.executeUpdate();
        }
    }

    @Override
    public void updateOne(User user) throws SQLException {
        // Hacher seulement si c'est un nouveau mot de passe en clair
        String mdpToStore = Passwordutil.isHashed(user.getMdp())
                ? user.getMdp()
                : Passwordutil.hash(user.getMdp());

        String req = "UPDATE `user` SET `nom`=?,`prenom`=?,`email`=?,`mdp`=?,`role`=?,`num_tel`=?,`photo`=? WHERE `id_user`=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, mdpToStore);
            ps.setString(5, user.getRole());
            ps.setString(6, user.getNum_tel());
            ps.setString(7, user.getPhoto());
            ps.setInt(8, user.getId_user());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteOne(User user) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM `user` WHERE `id_user`=?")) {
            ps.setInt(1, user.getId_user());
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> selectALL() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM `user`")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Authentification ──────────────────────────────────────

    /**
     * Login sécurisé: récupère user par email, vérifie mdp avec PasswordUtil.verify()
     * Compatible migration: accepte les anciens mdp en clair le temps de la migration
     */
    public User login(String email, String plainPassword) {
        String req = "SELECT * FROM `user` WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedMdp = rs.getString("mdp");
                if (Passwordutil.verify(plainPassword, storedMdp)) {
                    // Si l'ancien mdp était en clair → le hacher maintenant (migration auto)
                    if (!Passwordutil.isHashed(storedMdp)) {
                        migratePassword(email, plainPassword);
                    }
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    /** Migration auto: hacher un ancien mdp en clair */
    private void migratePassword(String email, String plainPassword) {
        try {
            updatePassword(email, plainPassword); // updatePassword hashera via PasswordUtil
        } catch (SQLException e) {
            System.err.println("Migration mdp error: " + e.getMessage());
        }
    }

    // ── Méthodes utilitaires ──────────────────────────────────

    public boolean emailExists(String email) {
        try (PreparedStatement ps = cnx.prepareStatement("SELECT id_user FROM `user` WHERE email = ?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public boolean emailExistsForOther(String email, int excludeUserId) {
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT id_user FROM user WHERE email = ? AND id_user != ?")) {
            ps.setString(1, email); ps.setInt(2, excludeUserId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /**
     * passwordExists: compare en clair (ancien) OU vérifie hash (nouveau)
     * Utilisé pour éviter les doublons à l'inscription — rendu inutile avec le hachage
     * (chaque hash est unique grâce au sel) → toujours retourne false
     */
    public boolean passwordExists(String mdp) {
        // Avec sel aléatoire, 2 hashes du même mdp sont différents → pas de doublon possible
        return false;
    }

    public boolean passwordExistsForOther(String mdp, int excludeUserId) {
        return false; // idem
    }

    public void signUp(User user) throws SQLException {
        if (emailExists(user.getEmail()))
            throw new SQLException("Cet email est déjà utilisé !");
        insertOne(user); // insertOne hashera le mdp
    }

    public User getUserById(int userId) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM user WHERE id_user = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public User getUserByEmail(String email) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM user WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Mettre à jour le mot de passe (hashé automatiquement) */
    public void updatePassword(String email, String newPlainPassword) throws SQLException {
        String hashed = Passwordutil.hash(newPlainPassword);
        try (PreparedStatement ps = cnx.prepareStatement("UPDATE user SET mdp = ? WHERE email = ?")) {
            ps.setString(1, hashed);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
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
}