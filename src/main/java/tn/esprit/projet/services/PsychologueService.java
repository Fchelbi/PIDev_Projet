package tn.esprit.projet.services;

import tn.esprit.projet.entities.Psychologue; // Fixed missing import from image_f25bc9.jpg
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PsychologueService {
    private Connection cnx;

    public PsychologueService() {
        this.cnx = MyDBConnexion.getInstance().getCnx();
    }

    // CREATE
    public void insert(Psychologue p) throws SQLException {
        String req = "INSERT INTO psychologue (nom, prenom, email, specialite, statut) VALUES (?, ?, ?, ?, 'ACTIF')";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getPrenom());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getSpecialite());
            ps.executeUpdate();
        }
    }

    // READ
    public List<Psychologue> getAll() throws SQLException {
        List<Psychologue> list = new ArrayList<>();
        String req = "SELECT * FROM psychologue";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Psychologue p = new Psychologue();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setSpecialite(rs.getString("specialite"));
                p.setEmail(rs.getString("email"));
                list.add(p);
            }
        }
        return list;
    }

    // UPDATE
    public void update(Psychologue p) throws SQLException {
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

    // DELETE
    public void delete(int id) throws SQLException {
        String req = "DELETE FROM psychologue WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}