package services;

import entities.Consultation;
import utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService {
    private Connection cnx;

    public ConsultationService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    // ✅ Récupérer les consultations d'un patient
    public List<Consultation> getConsultationsByPatient(int userId) throws SQLException {
        List<Consultation> list = new ArrayList<>();
        String req = "SELECT * FROM consultation_en_ligne WHERE utilisateur_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Consultation(
                            rs.getInt("id"),
                            rs.getInt("utilisateur_id"),
                            rs.getInt("psychologue_id"),
                            rs.getString("date_consultation"),
                            rs.getString("statut")
                    ));
                }
            }
        }
        return list;
    }

    // ✅ Réserver
    public void reserverConsultation(int psyId, int userId, String dateReservation) throws SQLException {
        String req = "INSERT INTO consultation_en_ligne (psychologue_id, utilisateur_id, date_consultation, statut) VALUES (?, ?, ?, 'En attente')";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, psyId);
            ps.setInt(2, userId);
            ps.setString(3, dateReservation);
            ps.executeUpdate();
        }
    }

    // ✅ Mettre à jour le statut (Accepter / À replanifier)
    public void updateStatut(int consultationId, String nouveauStatut) throws SQLException {
        String req = "UPDATE consultation_en_ligne SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, consultationId);
            ps.executeUpdate();
        }
    }

    // ✅ Récupérer les consultations d'un psy
    public List<Consultation> getConsultationsByPsychologue(int psyId) throws SQLException {
        List<Consultation> list = new ArrayList<>();
        String req = "SELECT * FROM consultation_en_ligne WHERE psychologue_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, psyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Consultation(
                            rs.getInt("id"),
                            rs.getInt("utilisateur_id"),
                            rs.getInt("psychologue_id"),
                            rs.getString("date_consultation"),
                            rs.getString("statut")
                    ));
                }
            }
        }
        return list;
    }

    // 🔥 Vérifier s'il existe déjà un RDV confirmé au même moment
    public boolean hasConflit(int psyId, String dateConsultation, int consultationId) throws SQLException {

        String req = "SELECT COUNT(*) FROM consultation_en_ligne " +
                "WHERE psychologue_id = ? " +
                "AND date_consultation = ? " +
                "AND statut = 'Confirmé' " +
                "AND id <> ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, psyId);
            ps.setString(2, dateConsultation);
            ps.setInt(3, consultationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // 🔥 conflit si > 0
                }
            }
        }
        return false;
    }

    // ✅ Supprimer consultation
    public void deleteConsultation(int consultationId) throws SQLException {
        String req = "DELETE FROM consultation_en_ligne WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, consultationId);
            ps.executeUpdate();
        }
    }

    public void updateStatutAndLink(int id, String statut, String meetLink) throws SQLException {
        String req = "UPDATE consultation SET status = ?, meet_link = ? WHERE id_consultation = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, statut);
            ps.setString(2, meetLink);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }
}