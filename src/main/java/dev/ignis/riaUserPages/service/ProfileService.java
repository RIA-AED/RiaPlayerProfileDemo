package dev.ignis.riaUserPages.service;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.database.ProfileDAO;
import dev.ignis.riaUserPages.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing player profiles.
 */
public class ProfileService {

    private final RiaUserPages plugin;
    private final ProfileDAO profileDAO;
    private final Map<UUID, PlayerProfile> profileCache;

    public ProfileService(RiaUserPages plugin) {
        this.plugin = plugin;
        this.profileDAO = plugin.getDatabaseManager().getProfileDAO();
        this.profileCache = new ConcurrentHashMap<>();
    }

    /**
     * Get or load a player's profile.
     */
    public PlayerProfile getProfile(UUID uuid) {
        // Check cache first
        PlayerProfile cached = profileCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // Try to get existing profile from database
        PlayerProfile existingProfile = profileDAO.getProfile(uuid).orElse(null);
        if (existingProfile != null) {
            // Only update username if player is currently online and name has changed
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && !onlinePlayer.getName().equals(existingProfile.getUsername())) {
                profileDAO.updateUsername(uuid, onlinePlayer.getName());
                existingProfile.setUsername(onlinePlayer.getName());
            }
            profileCache.put(uuid, existingProfile);
            return existingProfile;
        }
        
        // Profile doesn't exist - need to create
        // Try to get username from online player or offline player cache
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        String username = onlinePlayer != null ? onlinePlayer.getName() : Bukkit.getOfflinePlayer(uuid).getName();
        
        // If still can't get username, use UUID prefix as fallback
        if (username == null) {
            username = uuid.toString().substring(0, 8);
        }
        
        PlayerProfile newProfile = profileDAO.createProfile(uuid, username);
        profileCache.put(uuid, newProfile);
        return newProfile;
    }

    /**
     * Get a profile by player name.
     */
    public PlayerProfile getProfileByName(String name) {
        // Try online players first
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            return getProfile(player.getUniqueId());
        }

        // Try database lookup
        return profileDAO.getProfileByUsername(name).orElse(null);
    }

    /**
     * Set a player's bio.
     */
    public void setBio(UUID uuid, String bio) {
        profileDAO.updateBio(uuid, bio);
        
        // Update cache if present
        PlayerProfile profile = profileCache.get(uuid);
        if (profile != null) {
            profile.setBio(bio);
        }
    }

    /**
     * Unlock additional pages for a player.
     */
    public void unlockPages(UUID uuid, int pages) {
        PlayerProfile profile = getProfile(uuid);
        int newTotal = Math.min(profile.getUnlockedPages() + pages, 
                plugin.getConfigManager().getMaxPages());
        
        profileDAO.updateUnlockedPages(uuid, newTotal);
        profile.setUnlockedPages(newTotal);
    }

    /**
     * Set unlocked pages to a specific number (for admin lock command).
     */
    public void setUnlockedPages(UUID uuid, int pages) {
        PlayerProfile profile = getProfile(uuid);
        profileDAO.updateUnlockedPages(uuid, pages);
        profile.setUnlockedPages(pages);
    }

    /**
     * Clear a profile from cache (e.g., on player quit).
     */
    public void clearCache(UUID uuid) {
        profileCache.remove(uuid);
    }
}
