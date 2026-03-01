package services;

import entities.Rapport;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class Servicerapport {

    private final Connection cnx;

    public Servicerapport() {
        cnx = MyDBConnexion.getInstance().getCnx();
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        String sql = """
            CREATE TABLE IF NOT EXISTS rapport (
                id_rapport      INT AUTO_INCREMENT PRIMARY KEY,
                id_patient      INT NOT NULL,
                id_coach        INT NOT NULL,
                contenu         TEXT,
                recommandations TEXT,
                nb_seances      INT DEFAULT 1,
                score_humeur    DOUBLE DEFAULT 5.0,
                periode         VARCHAR(255),
                date_creation   DATETIME DEFAULT CURRENT_TIMESTAMP,
                fichier_pdf     VARCHAR(512),
                FOREIGN KEY (id_patient) REFERENCES user(id_user) ON DELETE CASCADE,
                FOREIGN KEY (id_coach)   REFERENCES user(id_user) ON DELETE CASCADE
            )
        """;
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("⚠️ createTable rapport: " + e.getMessage());
        }
    }

    /** Enregistrer un nouveau rapport */
    public void save(Rapport r) throws SQLException {
        String sql = """
            INSERT INTO rapport (id_patient, id_coach, contenu, recommandations,
                nb_seances, score_humeur, periode, date_creation, fichier_pdf)
            VALUES (?,?,?,?,?,?,?,NOW(),?)
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getId_patient());
            ps.setInt(2, r.getId_coach());
            ps.setString(3, r.getContenu());
            ps.setString(4, r.getRecommandations());
            ps.setInt(5, r.getNb_seances());
            ps.setDouble(6, r.getScore_humeur());
            ps.setString(7, r.getPeriode());
            ps.setString(8, r.getFichier_pdf());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) r.setId_rapport(rs.getInt(1));
        }
    }

    /** Score humeur moyen du dernier rapport d'un patient */
    public double getLatestScoreForPatient(int patientId) throws SQLException {
        String sql = """
            SELECT score_humeur FROM rapport
            WHERE id_patient = ?
            ORDER BY date_creation DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("score_humeur");
        }
        return -1; // pas de rapport
    }

    /** Tous les rapports d'un patient (pour historique) */
    public List<Rapport> getByPatient(int patientId) throws SQLException {
        String sql = "SELECT * FROM rapport WHERE id_patient=? ORDER BY date_creation DESC";
        List<Rapport> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Score moyen de tous les patients d'un coach */
    public double getAverageScoreForCoach(int coachId) throws SQLException {
        String sql = """
            SELECT AVG(r.score_humeur) FROM rapport r
            INNER JOIN (
                SELECT id_patient, MAX(date_creation) AS latest
                FROM rapport WHERE id_coach=?
                GROUP BY id_patient
            ) sub ON r.id_patient=sub.id_patient AND r.date_creation=sub.latest
            WHERE r.id_coach=?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId); ps.setInt(2, coachId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double v = rs.getDouble(1);
                return rs.wasNull() ? -1 : v;
            }
        }
        return -1;
    }

    /** Distribution des états pour les patients d'un coach */
    public int[] getDistributionForCoach(int coachId) throws SQLException {
        // returns [bien, moyen, bas]
        int[] dist = {0, 0, 0};
        String sql = """
            SELECT r.id_patient, r.score_humeur FROM rapport r
            INNER JOIN (
                SELECT id_patient, MAX(date_creation) AS latest
                FROM rapport WHERE id_coach=?
                GROUP BY id_patient
            ) sub ON r.id_patient=sub.id_patient AND r.date_creation=sub.latest
            WHERE r.id_coach=?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, coachId); ps.setInt(2, coachId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double score = rs.getDouble("score_humeur");
                if (score >= 7) dist[0]++;
                else if (score >= 4) dist[1]++;
                else dist[2]++;
            }
        }
        return dist;
    }

    /** Stats globales (pour Admin) */
    public double getGlobalAverageScore() throws SQLException {
        String sql = """
            SELECT AVG(r.score_humeur) FROM rapport r
            INNER JOIN (
                SELECT id_patient, MAX(date_creation) AS latest
                FROM rapport GROUP BY id_patient
            ) sub ON r.id_patient=sub.id_patient AND r.date_creation=sub.latest
        """;
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                double v = rs.getDouble(1);
                return rs.wasNull() ? -1 : v;
            }
        }
        return -1;
    }

    public int[] getGlobalDistribution() throws SQLException {
        int[] dist = {0, 0, 0};
        String sql = """
            SELECT r.score_humeur FROM rapport r
            INNER JOIN (
                SELECT id_patient, MAX(date_creation) AS latest
                FROM rapport GROUP BY id_patient
            ) sub ON r.id_patient=sub.id_patient AND r.date_creation=sub.latest
        """;
        try (Statement st = cnx.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                double s = rs.getDouble(1);
                if (s >= 7) dist[0]++;
                else if (s >= 4) dist[1]++;
                else dist[2]++;
            }
        }
        return dist;
    }

    private Rapport mapRow(ResultSet rs) throws SQLException {
        Rapport r = new Rapport();
        r.setId_rapport(rs.getInt("id_rapport"));
        r.setId_patient(rs.getInt("id_patient"));
        r.setId_coach(rs.getInt("id_coach"));
        r.setContenu(rs.getString("contenu"));
        r.setRecommandations(rs.getString("recommandations"));
        r.setNb_seances(rs.getInt("nb_seances"));
        r.setScore_humeur(rs.getDouble("score_humeur"));
        r.setPeriode(rs.getString("periode"));
        r.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
        r.setFichier_pdf(rs.getString("fichier_pdf"));
        return r;
    }
}