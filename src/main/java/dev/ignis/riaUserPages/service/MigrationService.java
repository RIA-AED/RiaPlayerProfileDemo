package dev.ignis.riaUserPages.service;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.config.ConfigManager;
import dev.ignis.riaUserPages.database.*;
import dev.ignis.riaUserPages.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for migrating player data between accounts.
 */
public class MigrationService {

    private final RiaUserPages plugin;
    private final DatabaseManager databaseManager;
    private final ProfileDAO profileDAO;
    private final InventoryDAO inventoryDAO;
    private final LikeDAO likeDAO;
    private final CommentDAO commentDAO;
    private final ConfigManager config;

    public MigrationService(RiaUserPages plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.profileDAO = databaseManager.getProfileDAO();
        this.inventoryDAO = databaseManager.getInventoryDAO();
        this.likeDAO = databaseManager.getLikeDAO();
        this.commentDAO = databaseManager.getCommentDAO();
        this.config = plugin.getConfigManager();
    }

    /**
     * Result of a migration operation.
     */
    public enum MigrationResult {
        SUCCESS,
        SOURCE_NOT_FOUND,
        TARGET_EXISTS,
        SAME_PLAYER,
        ERROR
    }

    /**
     * Migrate all data from source player to target player.
     *
     * @param sourceIdentifier Source UUID string or username
     * @param targetIdentifier Target UUID string or username
     * @return Future with the migration result
     */
    public CompletableFuture<MigrationResult> migrateData(String sourceIdentifier, String targetIdentifier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Resolve source UUID
                String sourceUuid = resolveUuid(sourceIdentifier);
                if (sourceUuid == null) {
                    return MigrationResult.SOURCE_NOT_FOUND;
                }

                // Resolve target UUID and username
                Player targetPlayer = Bukkit.getPlayer(targetIdentifier);
                String targetUuid;
                String targetUsername;

                if (targetPlayer != null) {
                    targetUuid = targetPlayer.getUniqueId().toString();
                    targetUsername = targetPlayer.getName();
                } else {
                    // Try to parse as UUID
                    try {
                        UUID uuid = UUID.fromString(targetIdentifier);
                        targetUuid = uuid.toString();
                        targetUsername = Bukkit.getOfflinePlayer(uuid).getName();
                        if (targetUsername == null) {
                            targetUsername = targetIdentifier;
                        }
                    } catch (IllegalArgumentException e) {
                        // Try to find by username
                        Optional<PlayerProfile> profile = profileDAO.getProfileByUsername(targetIdentifier);
                        if (profile.isPresent()) {
                            targetUuid = profile.get().getUuid().toString();
                            targetUsername = profile.get().getUsername();
                        } else {
                            return MigrationResult.SOURCE_NOT_FOUND;
                        }
                    }
                }

                // Check if same player
                if (sourceUuid.equalsIgnoreCase(targetUuid)) {
                    return MigrationResult.SAME_PLAYER;
                }

                // Check if target already has data
                Optional<PlayerProfile> targetProfile = profileDAO.getProfile(UUID.fromString(targetUuid));
                if (targetProfile.isPresent() && !config.isAllowOverwrite()) {
                    return MigrationResult.TARGET_EXISTS;
                }

                // Perform migration in a transaction
                performMigration(sourceUuid, targetUuid, targetUsername);

                // Clear cache entries
                plugin.getProfileService().clearCache(UUID.fromString(sourceUuid));
                plugin.getProfileService().clearCache(UUID.fromString(targetUuid));

                return MigrationResult.SUCCESS;
            } catch (Exception e) {
                plugin.getLogger().severe("Error during migration: " + e.getMessage());
                e.printStackTrace();
                return MigrationResult.ERROR;
            }
        });
    }

    private String resolveUuid(String identifier) {
        // Try online player first
        Player player = Bukkit.getPlayer(identifier);
        if (player != null) {
            return player.getUniqueId().toString();
        }

        // Try to parse as UUID
        try {
            UUID uuid = UUID.fromString(identifier);
            return uuid.toString();
        } catch (IllegalArgumentException e) {
            // Try to find by username
            Optional<PlayerProfile> profile = profileDAO.getProfileByUsername(identifier);
            return profile.map(p -> p.getUuid().toString()).orElse(null);
        }
    }

    private void performMigration(String oldUuid, String newUuid, String newUsername) throws SQLException {
        Connection conn = databaseManager.getConnection();
        try {
            conn.setAutoCommit(false);

            // Update profiles table
            profileDAO.updateUuid(oldUuid, newUuid, newUsername);

            // Update inventories table
            inventoryDAO.updateOwnerUuid(oldUuid, newUuid);

            // Update likes table (both liker and target)
            likeDAO.updateLikerUuid(oldUuid, newUuid);
            likeDAO.updateTargetUuid(oldUuid, newUuid);

            // Update comments table (both author and target)
            commentDAO.updateAuthorUuid(oldUuid, newUuid, newUsername);
            commentDAO.updateTargetUuid(oldUuid, newUuid);

            conn.commit();
            plugin.getLogger().info("Successfully migrated data from " + oldUuid + " to " + newUuid);
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }
}
