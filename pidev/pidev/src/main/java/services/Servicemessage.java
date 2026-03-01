package services;

import entities.Message;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Servicemessage {

    private final Connection cnx;

    public Servicemessage() {
        cnx = MyDBConnexion.getInstance().getCnx();
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id_message      INT AUTO_INCREMENT PRIMARY KEY,
                id_expediteur   INT NOT NULL,
                id_destinataire INT NOT NULL,
                contenu         TEXT NOT NULL,
                date_envoi      DATETIME DEFAULT CURRENT_TIMESTAMP,
                lu              BOOLEAN DEFAULT FALSE,
                modifie         BOOLEAN DEFAULT FALSE,
                type            ENUM('TEXT','CALL_IN','CALL_OUT','CALL_MISSED') DEFAULT 'TEXT',
                FOREIGN KEY (id_expediteur)   REFERENCES user(id_user) ON DELETE CASCADE,
                FOREIGN KEY (id_destinataire) REFERENCES user(id_user) ON DELETE CASCADE
            )
        """;
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
            // Ajouter les colonnes si upgrade depuis ancienne version
            tryAddColumn("ALTER TABLE messages ADD COLUMN IF NOT EXISTS modifie BOOLEAN DEFAULT FALSE");
            tryAddColumn("ALTER TABLE messages ADD COLUMN IF NOT EXISTS type ENUM('TEXT','CALL_IN','CALL_OUT','CALL_MISSED') DEFAULT 'TEXT'");
        } catch (SQLException e) {
            System.err.println("⚠️ createTable messages: " + e.getMessage());
        }
    }

    private void tryAddColumn(String sql) {
        try (Statement st = cnx.createStatement()) { st.execute(sql); }
        catch (SQLException e) { /* ignore if already exists */ }
    }

    // ── Envoyer un message ────────────────────────────────────
    public void sendMessage(Message m) throws SQLException {
        String sql = "INSERT INTO messages (id_expediteur, id_destinataire, contenu, date_envoi, lu, modifie, type) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getId_expediteur());
            ps.setInt(2, m.getId_destinataire());
            ps.setString(3, m.getContenu());
            ps.setTimestamp(4, Timestamp.valueOf(m.getDate_envoi()));
            ps.setBoolean(5, false);
            ps.setBoolean(6, false);
            ps.setString(7, m.getType().name());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) m.setId_message(rs.getInt(1));
        }
    }

    // ── Modifier un message ───────────────────────────────────
    public void editMessage(int messageId, String newContenu) throws SQLException {
        String sql = "UPDATE messages SET contenu=?, modifie=TRUE WHERE id_message=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, newContenu);
            ps.setInt(2, messageId);
            ps.executeUpdate();
        }
    }

    // ── Supprimer un message ──────────────────────────────────
    public void deleteMessage(int messageId) throws SQLException {
        String sql = "DELETE FROM messages WHERE id_message=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        }
    }

    // ── Supprimer toute la conversation ──────────────────────
    public void deleteConversation(int userId1, int userId2) throws SQLException {
        String sql = "DELETE FROM messages WHERE (id_expediteur=? AND id_destinataire=?) OR (id_expediteur=? AND id_destinataire=?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId1); ps.setInt(2, userId2);
            ps.setInt(3, userId2); ps.setInt(4, userId1);
            ps.executeUpdate();
        }
    }

    // ── Conversation entre 2 users ────────────────────────────
    public List<Message> getConversation(int userId1, int userId2) throws SQLException {
        String sql = """
            SELECT * FROM messages
            WHERE (id_expediteur=? AND id_destinataire=?)
               OR (id_expediteur=? AND id_destinataire=?)
            ORDER BY date_envoi ASC
        """;
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId1); ps.setInt(2, userId2);
            ps.setInt(3, userId2); ps.setInt(4, userId1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Nb non lus total pour un user ────────────────────────
    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE id_destinataire=? AND lu=FALSE AND type='TEXT'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Nb non lus PAR expéditeur (pour badge Instagram) ─────
    public int countUnreadFrom(int senderId, int receiverId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE id_expediteur=? AND id_destinataire=? AND lu=FALSE AND type='TEXT'";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, senderId); ps.setInt(2, receiverId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Aperçu du dernier message d'une conversation ─────────
    public String getLastMessagePreview(int userId1, int userId2) throws SQLException {
        String sql = """
            SELECT contenu, type, id_expediteur FROM messages
            WHERE (id_expediteur=? AND id_destinataire=?) OR (id_expediteur=? AND id_destinataire=?)
            ORDER BY date_envoi DESC LIMIT 1
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId1); ps.setInt(2, userId2);
            ps.setInt(3, userId2); ps.setInt(4, userId1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                String contenu = rs.getString("contenu");
                boolean mine = rs.getInt("id_expediteur") == userId1;
                if ("CALL_IN".equals(type) || "CALL_OUT".equals(type))
                    return (mine ? "📞 Appel passé · " : "📞 Appel reçu · ") + contenu;
                if ("CALL_MISSED".equals(type)) return "📵 Appel manqué · " + contenu;
                return (mine ? "Vous: " : "") + contenu;
            }
        }
        return "";
    }

    // ── Derniers msgs non lus par expéditeur → pour notifs ───
    public Map<Integer, String> getUnreadSenderPreviews(int receiverId) throws SQLException {
        String sql = """
            SELECT id_expediteur, contenu FROM messages
            WHERE id_destinataire=? AND lu=FALSE AND type='TEXT'
            ORDER BY date_envoi DESC
        """;
        Map<Integer, String> result = new LinkedHashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int sid = rs.getInt("id_expediteur");
                if (!result.containsKey(sid))
                    result.put(sid, rs.getString("contenu"));
            }
        }
        return result;
    }

    // ── Marquer conversation comme lue ────────────────────────
    public void markAsRead(int expediteurId, int destinataireId) throws SQLException {
        String sql = "UPDATE messages SET lu=TRUE WHERE id_expediteur=? AND id_destinataire=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, expediteurId); ps.setInt(2, destinataireId);
            ps.executeUpdate();
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId_message(rs.getInt("id_message"));
        m.setId_expediteur(rs.getInt("id_expediteur"));
        m.setId_destinataire(rs.getInt("id_destinataire"));
        m.setContenu(rs.getString("contenu"));
        m.setDate_envoi(rs.getTimestamp("date_envoi").toLocalDateTime());
        m.setLu(rs.getBoolean("lu"));
        m.setModifie(rs.getBoolean("modifie"));
        try {
            String typeStr = rs.getString("type");
            m.setType(typeStr != null ? Message.Type.valueOf(typeStr) : Message.Type.TEXT);
        } catch (IllegalArgumentException e) { m.setType(Message.Type.TEXT); }
        return m;
    }
}