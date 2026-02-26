package dev.ignis.riaUserPages.database;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Access Object for likes.
 */
public class LikeDAO {

    private final DatabaseManager databaseManager;
    private final String tableName;

    public LikeDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.tableName = databaseManager.getTablePrefix() + "likes";
    }

    /**
     * Check if a player has liked a target today.
     */
    public boolean hasLikedToday(UUID likerUuid, UUID targetUuid) {
        String sql = "SELECT 1 FROM " + tableName + " WHERE liker_uuid = ? AND target_uuid = ? AND like_date = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, likerUuid.toString());
            stmt.setString(2, targetUuid.toString());
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check like status", e);
        }
    }

    /**
     * Get the number of likes given by a player today.
     */
    public int getTodayLikeCount(UUID likerUuid) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE liker_uuid = ? AND like_date = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, likerUuid.toString());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get today like count", e);
        }
        return 0;
    }

    /**
     * Add a like.
     */
    public void addLike(UUID likerUuid, UUID targetUuid) {
        String sql = "INSERT INTO " + tableName + " (liker_uuid, target_uuid, like_date) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, likerUuid.toString());
            stmt.setString(2, targetUuid.toString());
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add like", e);
        }
    }

    /**
     * Delete all likes for a target (for reset).
     */
    public void deleteAllLikes(UUID targetUuid) {
        String sql = "DELETE FROM " + tableName + " WHERE target_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete likes", e);
        }
    }

    /**
     * Update liker UUID (for migration).
     */
    public void updateLikerUuid(String oldUuid, String newUuid) {
        String sql = "UPDATE " + tableName + " SET liker_uuid = ? WHERE liker_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUuid);
            stmt.setString(2, oldUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update liker UUID", e);
        }
    }

    /**
     * Update target UUID (for migration).
     */
    public void updateTargetUuid(String oldUuid, String newUuid) {
        String sql = "UPDATE " + tableName + " SET target_uuid = ? WHERE target_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUuid);
            stmt.setString(2, oldUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update target UUID", e);
        }
    }
}
