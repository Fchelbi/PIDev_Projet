package services;

import entities.Call;
import utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class Callservice {

    private final Connection cnx;

    public Callservice() {
        cnx = MyDBConnexion.getInstance().getCnx();
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        String sql = """
            CREATE TABLE IF NOT EXISTS calls (
                id_call        INT AUTO_INCREMENT PRIMARY KEY,
                id_caller      INT NOT NULL,
                id_receiver    INT NOT NULL,
                status         ENUM('RINGING','ACCEPTED','REJECTED','ENDED','MISSED') DEFAULT 'RINGING',
                date_appel     DATETIME DEFAULT CURRENT_TIMESTAMP,
                duree_secondes INT DEFAULT 0,
                FOREIGN KEY (id_caller)   REFERENCES user(id_user) ON DELETE CASCADE,
                FOREIGN KEY (id_receiver) REFERENCES user(id_user) ON DELETE CASCADE
            )
        """;
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("⚠️ createTable calls: " + e.getMessage());
        }
    }

    /** Initier un appel → retourne l'id de l'appel */
    public int initiateCall(int callerId, int receiverId) throws SQLException {
        // Annuler les appels RINGING précédents entre ces deux
        cancelPreviousCalls(callerId, receiverId);

        String sql = "INSERT INTO calls (id_caller, id_receiver, status, date_appel) VALUES (?,?,'RINGING',NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, callerId);
            ps.setInt(2, receiverId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /** Vérifier si le receiver a un appel entrant RINGING */
    public Optional<Call> getIncomingCall(int receiverId) throws SQLException {
        String sql = "SELECT * FROM calls WHERE id_receiver=? AND status='RINGING' ORDER BY date_appel DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    /** Vérifier l'état d'un appel (pour le caller qui attend) */
    public Optional<Call> getCallById(int callId) throws SQLException {
        String sql = "SELECT * FROM calls WHERE id_call=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, callId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    /** Accepter l'appel */
    public void acceptCall(int callId) throws SQLException {
        updateStatus(callId, "ACCEPTED");
    }

    /** Rejeter l'appel */
    public void rejectCall(int callId) throws SQLException {
        updateStatus(callId, "REJECTED");
    }

    /** Terminer l'appel */
    public void endCall(int callId, int durationSeconds) throws SQLException {
        String sql = "UPDATE calls SET status='ENDED', duree_secondes=? WHERE id_call=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, durationSeconds);
            ps.setInt(2, callId);
            ps.executeUpdate();
        }
    }

    /** Marquer comme manqué si toujours RINGING après timeout */
    public void markMissed(int callId) throws SQLException {
        updateStatus(callId, "MISSED");
    }

    private void updateStatus(int callId, String status) throws SQLException {
        String sql = "UPDATE calls SET status=? WHERE id_call=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, callId);
            ps.executeUpdate();
        }
    }

    private void cancelPreviousCalls(int callerId, int receiverId) {
        try {
            String sql = "UPDATE calls SET status='ENDED' WHERE (id_caller=? OR id_caller=?) AND status='RINGING'";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, callerId); ps.setInt(2, receiverId);
                ps.executeUpdate();
            }
        } catch (SQLException e) { /* ignore */ }
    }

    private Call mapRow(ResultSet rs) throws SQLException {
        Call c = new Call();
        c.setId_call(rs.getInt("id_call"));
        c.setId_caller(rs.getInt("id_caller"));
        c.setId_receiver(rs.getInt("id_receiver"));
        c.setStatus(Call.Status.valueOf(rs.getString("status")));
        c.setDate_appel(rs.getTimestamp("date_appel").toLocalDateTime());
        c.setDuree_secondes(rs.getInt("duree_secondes"));
        return c;
    }
}