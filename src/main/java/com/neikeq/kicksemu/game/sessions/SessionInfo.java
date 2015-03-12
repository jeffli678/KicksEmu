package com.neikeq.kicksemu.game.sessions;

import com.neikeq.kicksemu.storage.MySqlManager;
import com.neikeq.kicksemu.storage.SqlUtils;
import com.neikeq.kicksemu.utils.RandomGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionInfo {

    public static final String table = "sessions";

    public static int getPlayerId(int sessionId, Connection ... con) {
        return getInt("player_id", table, sessionId, con);
    }

    public static void setPlayerId(int playerId, int sessionId, Connection ... con) {
        SqlUtils.setInt("player_id", playerId, table, sessionId, con);
    }

    public static int getUserId(int sessionId, Connection ... con) {
        return getInt("user_id", table, sessionId, con);
    }

    public static void reduceExpiration(int sessionId) {
        String sql = "UPDATE sessions SET expiration = CURRENT_TIMESTAMP + " +
                "INTERVAL 1 MINUTE WHERE id = ?";

        try (Connection con = MySqlManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);

            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static void resetExpiration(int sessionId) {
        String sql = "UPDATE sessions SET expiration = CURRENT_TIMESTAMP + " +
                "INTERVAL 1 DAY WHERE id = ?";

        try (Connection con = MySqlManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);

            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static int generateSessionId() {
        String sql = "SELECT FLOOR(1 + RAND() * 4294967294) AS random_session_id FROM sessions" +
                " WHERE \"random_session_id\" NOT IN (SELECT id FROM sessions) LIMIT 1";

        try (Connection con = MySqlManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("random_session_id");
                } else {
                    return RandomGenerator.randomInt();
                }
            }
        } catch (Exception e) {
            return -1;
        }
    }

    public static void insertSession(int sessionId, int userId, int playerId) {
        String sql = "INSERT INTO sessions (id, user_id, player_id, expiration) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP + INTERVAL 1 DAY)";

        try (Connection con = MySqlManager.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.setInt(2, userId);
            stmt.setInt(3, playerId);

            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static int getInt(String column, String table, int id, Connection ... con) {
        String query = "SELECT " + column + " FROM " + table +
                " WHERE id = ? AND expiration > CURRENT_TIMESTAMP";

        try {
            Connection connection = con.length > 0 ? con[0] : MySqlManager.getConnection();

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(column);
                    } else {
                        return -1;
                    }
                }
            } finally {
                if (con.length <= 0) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            return -1;
        }
    }
}
