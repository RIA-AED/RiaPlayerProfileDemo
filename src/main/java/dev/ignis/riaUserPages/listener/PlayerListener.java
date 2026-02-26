package dev.ignis.riaUserPages.listener;

import dev.ignis.riaUserPages.RiaUserPages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player join/quit events.
 */
public class PlayerListener implements Listener {

    private final RiaUserPages plugin;

    public PlayerListener(RiaUserPages plugin) {
        this.plugin = plugin;
    }

    /**
     * Create player profile on join if it doesn't exist.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Run asynchronously to avoid blocking the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // This will create the profile if it doesn't exist
                plugin.getProfileService().getProfile(event.getPlayer().getUniqueId());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create profile for player: " + event.getPlayer().getName());
                e.printStackTrace();
            }
        });
    }

    /**
     * Clear profile cache on quit to free memory.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getProfileService().clearCache(event.getPlayer().getUniqueId());
    }
}
