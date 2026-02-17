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

        // On fait une jointure (JOIN) pour récupérer le nom de la table psychologue
        String req = "SELECT c.*, p.nom AS nom_psy " +
                "FROM consultation_en_ligne c " +
                "JOIN psychologue p ON c.psychologue_id = p.id " +
                "WHERE c.utilisateur_id = ?";

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

            // ASTUCE : On va détourner temporairement un champ String de l'entité
            // ou utiliser une Map. Mais le plus simple en JavaFX est d'utiliser
            // une propriété personnalisée dans le contrôleur.

            // Si ton entité Consultation a un champ 'dateConsultation' qui est un String,
            // pour ce test, on peut vérifier si on peut stocker le nom quelque part.
            // SINON, la méthode la plus propre est de créer une classe "ConsultationDTO"
            // ou d'ajouter juste une variable String nomPsy dans Consultation.

            list.add(c);
        }
        return list;
    }

    // Version modifiée pour accepter la date du formulaire
    public void reserverConsultation(int psyId, int userId, String dateReservation) throws SQLException {
        String req = "INSERT INTO consultation_en_ligne (psychologue_id, utilisateur_id, date_consultation, statut) VALUES (?, ?, ?, 'En attente')";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, psyId);
        ps.setInt(2, userId);
        ps.setString(3, dateReservation);
        ps.executeUpdate();
    }
}


