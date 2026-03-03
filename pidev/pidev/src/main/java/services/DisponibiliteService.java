package services;

import utils.MyDBConnexion;
import java.sql.*;

public class DisponibiliteService {
    private Connection cnx;

    public DisponibiliteService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    public void ajouterDispo(int psyId, java.sql.Date jour, java.sql.Time heureDebut) throws SQLException {
        // Requête simplifiée sans heure_fin
        String req = "INSERT INTO disponibilite (psychologue_id, jour, heure_debut, statut) VALUES (?, ?, ?, 'Libre')";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, psyId);
            ps.setDate(2, jour);
            ps.setTime(3, heureDebut);
            ps.executeUpdate();
        }
    }
}