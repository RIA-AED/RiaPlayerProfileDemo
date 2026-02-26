package dev.ignis.riaUserPages.database;

import dev.ignis.riaUserPages.model.PlayerProfile;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for player profiles.
 */
public class ProfileDAO {

    private final DatabaseManager databaseManager;
    private final String tableName;

    public ProfileDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.tableName = databaseManager.getTablePrefix() + "profiles";
    }

    /**
     * Get or create a player profile.
     */
    public PlayerProfile getOrCreateProfile(UUID uuid, String username) {
        Optional<PlayerProfile> existing = getProfile(uuid);
        if (existing.isPresent()) {
            PlayerProfile profile = existing.get();
            // Update username if changed
            if (!profile.getUsername().equals(username)) {
                updateUsername(uuid, username);
                profile.setUsername(username);
            }
            return profile;
        }
        return createProfile(uuid, username);
    }

    /**
     * Get profile by UUID.
     */
    public Optional<PlayerProfile> getProfile(UUID uuid) {
        String sql = "SELECT * FROM " + tableName + " WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get profile", e);
        }
        return Optional.empty();
    }

    /**
     * Get profile by username (case-insensitive).
     */
    public Optional<PlayerProfile> getProfileByUsername(String username) {
        String sql = "SELECT * FROM " + tableName + " WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get profile by username", e);
        }
        return Optional.empty();
    }

    /**
     * Create a new profile.
     */
    public PlayerProfile createProfile(UUID uuid, String username) {
        String sql = "INSERT INTO " + tableName + " (uuid, username, bio, unlocked_pages) VALUES (?, ?, ?, 1)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setString(3, "");
            stmt.executeUpdate();
            return getProfile(uuid).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create profile", e);
        }
    }

    /**
     * Update profile bio.
     */
    public void updateBio(UUID uuid, String bio) {
        String sql = "UPDATE " + tableName + " SET bio = ? WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bio);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update bio", e);
        }
    }

    /**
     * Update username.
     */
    public void updateUsername(UUID uuid, String username) {
        String sql = "UPDATE " + tableName + " SET username = ? WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update username", e);
        }
    }

    /**
     * Update total likes.
     */
    public void updateTotalLikes(UUID uuid, int totalLikes) {
        String sql = "UPDATE " + tableName + " SET total_likes = ? WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, totalLikes);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update total likes", e);
        }
    }

    /**
     * Update unlocked pages.
     */
    public void updateUnlockedPages(UUID uuid, int unlockedPages) {
        String sql = "UPDATE " + tableName + " SET unlocked_pages = ? WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, unlockedPages);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update unlocked pages", e);
        }
    }

    /**
     * Delete a profile and all related data.
     */
    public void deleteProfile(UUID uuid) {
        String sql = "DELETE FROM " + tableName + " WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete profile", e);
        }
    }

    /**
     * Update profile UUID (for migration).
     */
    public void updateUuid(String oldUuid, String newUuid, String newUsername) {
        String sql = "UPDATE " + tableName + " SET uuid = ?, username = ? WHERE uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUuid);
            stmt.setString(2, newUsername);
            stmt.setString(3, oldUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update UUID", e);
        }
    }

    private PlayerProfile mapResultSet(ResultSet rs) throws SQLException {
        return new PlayerProfile(
            UUID.fromString(rs.getString("uuid")),
            rs.getString("username"),
            rs.getString("bio"),
            rs.getTimestamp("joined_date"),
            rs.getInt("total_likes"),
            rs.getInt("unlocked_pages")
        );
    }
}
