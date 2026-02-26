package dev.ignis.riaUserPages.database;

import dev.ignis.riaUserPages.model.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for comments.
 */
public class CommentDAO {

    private final DatabaseManager databaseManager;
    private final String tableName;

    public CommentDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.tableName = databaseManager.getTablePrefix() + "comments";
    }

    /**
     * Get all comments for a target player.
     */
    public List<Comment> getComments(UUID targetUuid) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " WHERE target_uuid = ? ORDER BY created_at DESC";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetUuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                comments.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get comments", e);
        }
        return comments;
    }

    /**
     * Get recent comments for a target player (limited count).
     */
    public List<Comment> getRecentComments(UUID targetUuid, int limit) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " WHERE target_uuid = ? ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetUuid.toString());
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                comments.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get recent comments", e);
        }
        return comments;
    }

    /**
     * Get comment count for a target player.
     */
    public int getCommentCount(UUID targetUuid) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE target_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get comment count", e);
        }
        return 0;
    }

    /**
     * Get a specific comment by ID.
     */
    public Optional<Comment> getComment(long id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get comment", e);
        }
        return Optional.empty();
    }

    /**
     * Add a new comment.
     */
    public Comment addComment(UUID authorUuid, String authorName, UUID targetUuid, String content) {
        String sql = "INSERT INTO " + tableName + " (author_uuid, author_name, target_uuid, content) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, authorUuid.toString());
            stmt.setString(2, authorName);
            stmt.setString(3, targetUuid.toString());
            stmt.setString(4, content);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                return getComment(id).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add comment", e);
        }
        throw new RuntimeException("Failed to retrieve created comment");
    }

    /**
     * Delete a comment by ID.
     */
    public boolean deleteComment(long id) {
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete comment", e);
        }
    }

    /**
     * Delete all comments for a target.
     */
    public void deleteAllComments(UUID targetUuid) {
        String sql = "DELETE FROM " + tableName + " WHERE target_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all comments", e);
        }
    }

    /**
     * Update author UUID (for migration).
     */
    public void updateAuthorUuid(String oldUuid, String newUuid, String newName) {
        String sql = "UPDATE " + tableName + " SET author_uuid = ?, author_name = ? WHERE author_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUuid);
            stmt.setString(2, newName);
            stmt.setString(3, oldUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update author UUID", e);
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

    private Comment mapResultSet(ResultSet rs) throws SQLException {
        return new Comment(
            rs.getLong("id"),
            UUID.fromString(rs.getString("author_uuid")),
            rs.getString("author_name"),
            UUID.fromString(rs.getString("target_uuid")),
            rs.getString("content"),
            rs.getTimestamp("created_at")
        );
    }
}
