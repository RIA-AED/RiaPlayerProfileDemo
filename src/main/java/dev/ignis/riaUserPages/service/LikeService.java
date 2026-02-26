package dev.ignis.riaUserPages.service;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.config.ConfigManager;
import dev.ignis.riaUserPages.database.LikeDAO;
import dev.ignis.riaUserPages.database.ProfileDAO;
import dev.ignis.riaUserPages.model.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing profile likes.
 * Simplified: only daily limit matters, can like same target multiple times.
 */
public class LikeService {

    private final RiaUserPages plugin;
    private final LikeDAO likeDAO;
    private final ProfileDAO profileDAO;
    private final ConfigManager config;

    public LikeService(RiaUserPages plugin) {
        this.plugin = plugin;
        this.likeDAO = plugin.getDatabaseManager().getLikeDAO();
        this.profileDAO = plugin.getDatabaseManager().getProfileDAO();
        this.config = plugin.getConfigManager();
    }

    /**
     * Result of a like operation.
     */
    public enum LikeResult {
        SUCCESS,
        LIMIT_REACHED,
        ERROR
    }

    /**
     * Like a player's profile.
     * Can like same target multiple times, only limited by daily quota.
     */
    public CompletableFuture<LikeResult> likeProfile(Player liker, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if trying to like self
                if (liker.getUniqueId().equals(targetUuid)) {
                    return LikeResult.ERROR;
                }

                // Check daily limit only
                int todayLikes = likeDAO.getTodayLikeCount(liker.getUniqueId());
                if (todayLikes >= config.getLikeLimitPerDay()) {
                    return LikeResult.LIMIT_REACHED;
                }

                // Add like
                likeDAO.addLike(liker.getUniqueId(), targetUuid);

                // Update total likes on profile
                PlayerProfile profile = plugin.getProfileService().getProfile(targetUuid);
                int newTotal = profile.getTotalLikes() + 1;
                profileDAO.updateTotalLikes(targetUuid, newTotal);
                profile.setTotalLikes(newTotal);

                return LikeResult.SUCCESS;
            } catch (Exception e) {
                plugin.getLogger().severe("Error liking profile: " + e.getMessage());
                return LikeResult.ERROR;
            }
        });
    }

    /**
     * Get remaining likes for a player today.
     */
    public int getRemainingLikes(UUID likerUuid) {
        int todayLikes = likeDAO.getTodayLikeCount(likerUuid);
        return Math.max(0, config.getLikeLimitPerDay() - todayLikes);
    }

    /**
     * Reset all likes for a player.
     */
    public void resetLikes(UUID targetUuid) {
        likeDAO.deleteAllLikes(targetUuid);
        profileDAO.updateTotalLikes(targetUuid, 0);
        
        PlayerProfile profile = plugin.getProfileService().getProfile(targetUuid);
        if (profile != null) {
            profile.setTotalLikes(0);
        }
    }
}
