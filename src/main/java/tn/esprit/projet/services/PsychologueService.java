package tn.esprit.projet.services;

import tn.esprit.projet.entities.Psychologue;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PsychologueService {
    private Connection cnx;

    public PsychologueService() {
        this.cnx = MyDBConnexion.getInstance().getCnx();
    }

    // ✅ CREATE
    public void insert(Psychologue p) throws SQLException {
        // Ajout du champ prenom dans la requête
        String req = "INSERT INTO psychologue (nom, prenom, email, specialite) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getSpecialite());
            ps.executeUpdate();
        }
    }

    // ✅ READ ALL
    public List<Psychologue> getAll() throws SQLException {
        List<Psychologue> list = new ArrayList<>();
        String req = "SELECT * FROM psychologue";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                // Utilisation du constructeur avec prenom
                list.add(new Psychologue(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("specialite"),
                        rs.getString("email")
                ));
            }
        }
        return list;
    }

    // ✅ READ BY ID
    public Psychologue getById(int id) throws SQLException {
        String req = "SELECT * FROM psychologue WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Psychologue(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("specialite"),
                            rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    // ✅ UPDATE
    public void update(Psychologue p) throws SQLException {
        // Ajout du prenom dans le SET
        String req = "UPDATE psychologue SET nom = ?, prenom = ?, email = ?, specialite = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getSpecialite());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
        }
    }

    // ✅ DELETE
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM psychologue WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}