package services;

import entities.Message;
import utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Servicemessage {

    private final Connection cnx;

    public Servicemessage() {
        cnx = MyDBConnexion.getInstance().getCnx();
        createTableIfNeeded();
    }

    /** Crée la table si elle n'existe pas encore */
    private void createTableIfNeeded() {
        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id_message    INT AUTO_INCREMENT PRIMARY KEY,
                id_expediteur INT NOT NULL,
                id_destinataire INT NOT NULL,
                contenu       TEXT NOT NULL,
                date_envoi    DATETIME DEFAULT CURRENT_TIMESTAMP,
                lu            BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (id_expediteur)   REFERENCES user(id_user) ON DELETE CASCADE,
                FOREIGN KEY (id_destinataire) REFERENCES user(id_user) ON DELETE CASCADE
            )
        """;
        try (Statement st = cnx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("⚠️ createTable messages: " + e.getMessage());
        }
    }

    /** Envoyer un message */
    public void sendMessage(Message m) throws SQLException {
        String sql = "INSERT INTO messages (id_expediteur, id_destinataire, contenu, date_envoi, lu) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getId_expediteur());
            ps.setInt(2, m.getId_destinataire());
            ps.setString(3, m.getContenu());
            ps.setTimestamp(4, Timestamp.valueOf(m.getDate_envoi()));
            ps.setBoolean(5, false);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) m.setId_message(rs.getInt(1));
        }
    }

    /** Conversation entre 2 utilisateurs (ordonnée par date) */
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

    /** Derniers messages par contact (pour la liste conversations) */
    public List<Message> getLastMessages(int userId) throws SQLException {
        String sql = """
            SELECT m.* FROM messages m
            INNER JOIN (
                SELECT MAX(id_message) as max_id
                FROM messages
                WHERE id_expediteur=? OR id_destinataire=?
                GROUP BY LEAST(id_expediteur,id_destinataire), GREATEST(id_expediteur,id_destinataire)
            ) sub ON m.id_message = sub.max_id
            ORDER BY m.date_envoi DESC
        """;
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Nombre messages non lus pour un user */
    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE id_destinataire=? AND lu=FALSE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Marquer conversation comme lue */
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
        return m;
    }
}
