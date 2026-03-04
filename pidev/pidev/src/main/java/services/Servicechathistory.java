package services;

import utils.MyDBConnexion;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour sauvegarder et charger l'historique des conversations IA
 * Table: chat_history (id, id_patient, role, content, session_id, created_at)
 */
public class Servicechathistory {

    private final Connection cnx;

    public Servicechathistory() {
        cnx = MyDBConnexion.getInstance().getCnx();
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        String sql = "CREATE TABLE IF NOT EXISTS `chat_history` ("
                + "`id` INT AUTO_INCREMENT PRIMARY KEY,"
                + "`id_patient` INT NOT NULL,"
                + "`session_id` VARCHAR(64) NOT NULL,"
                + "`role` VARCHAR(16) NOT NULL,"
                + "`content` TEXT NOT NULL,"
                + "`created_at` DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("createTable chat_history: " + e.getMessage());
        }
    }

    /** Sauvegarder un message */
    public void saveMessage(int patientId, String sessionId, String role, String content) {
        String sql = "INSERT INTO chat_history (id_patient, session_id, role, content) VALUES (?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, sessionId);
            ps.setString(3, role);
            ps.setString(4, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("saveMessage: " + e.getMessage());
        }
    }

    /** Recuperer les sessions d'un patient (une session = une conversation) */
    public List<SessionInfo> getSessions(int patientId) {
        List<SessionInfo> sessions = new ArrayList<>();
        String sql = "SELECT session_id, MIN(created_at) as started_at, COUNT(*) as msg_count "
                + "FROM chat_history WHERE id_patient = ? AND role = 'user' "
                + "GROUP BY session_id ORDER BY started_at DESC LIMIT 30";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sessions.add(new SessionInfo(
                        rs.getString("session_id"),
                        rs.getTimestamp("started_at").toLocalDateTime(),
                        rs.getInt("msg_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("getSessions: " + e.getMessage());
        }
        return sessions;
    }

    /** Recuperer tous les messages d'une session */
    public List<MessageRecord> getMessages(int patientId, String sessionId) {
        List<MessageRecord> msgs = new ArrayList<>();
        String sql = "SELECT role, content, created_at FROM chat_history "
                + "WHERE id_patient = ? AND session_id = ? ORDER BY created_at ASC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, sessionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                msgs.add(new MessageRecord(
                        rs.getString("role"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("getMessages: " + e.getMessage());
        }
        return msgs;
    }

    /** Supprimer une session */
    public void deleteSession(int patientId, String sessionId) {
        String sql = "DELETE FROM chat_history WHERE id_patient = ? AND session_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteSession: " + e.getMessage());
        }
    }

    // --- Inner classes ---

    public static class SessionInfo {
        public final String sessionId;
        public final LocalDateTime startedAt;
        public final int msgCount;
        public SessionInfo(String sid, LocalDateTime at, int count) {
            this.sessionId = sid; this.startedAt = at; this.msgCount = count;
        }
    }

    public static class MessageRecord {
        public final String role;
        public final String content;
        public final LocalDateTime createdAt;
        public MessageRecord(String role, String content, LocalDateTime at) {
            this.role = role; this.content = content; this.createdAt = at;
        }
    }
}