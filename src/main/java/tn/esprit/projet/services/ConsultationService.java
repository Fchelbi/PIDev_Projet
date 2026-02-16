package tn.esprit.projet.services;

import tn.esprit.projet.entities.Consultation;
import tn.esprit.projet.utils.MyDBConnexion;

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
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Consultation c = new Consultation(
                    rs.getInt("id"),
                    rs.getInt("utilisateur_id"),
                    rs.getInt("psychologue_id"),
                    rs.getString("date_consultation"),
                    rs.getString("statut")
            );
            list.add(c);
        }

        return list;
    }

    // ✅ Réserver consultation
    public void reserverConsultation(int psyId, int userId) throws SQLException {

        String req = "INSERT INTO consultation_en_ligne " +
                "(psychologue_id, utilisateur_id, date_consultation, statut) " +
                "VALUES (?, ?, NOW(), 'En attente')";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, psyId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    // ✅ Vérifier disponibilité
    public boolean estDisponible(int psyId, Date jour, Time heure) throws SQLException {

        String req = "SELECT COUNT(*) FROM consultation_en_ligne " +
                "WHERE psychologue_id = ? AND date_consultation = ?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, psyId);
        ps.setDate(2, jour);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) == 0;
        }

        return false;
    }
}
