package dev.ignis.riaUserPages.service;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.model.PendingAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pending actions that require user confirmation.
 */
public class PendingActionManager {

    private static final long ACTION_TIMEOUT_MS = 30000; // 30 seconds timeout
    
    private final RiaUserPages plugin;
    private final Map<UUID, PendingAction> pendingActions;

    public PendingActionManager(RiaUserPages plugin) {
        this.plugin = plugin;
        this.pendingActions = new ConcurrentHashMap<>();
        
        // Start cleanup task
        startCleanupTask();
    }

    /**
     * Add a pending action for a player.
     */
    public void addPendingAction(UUID playerUuid, PendingAction action) {
        pendingActions.put(playerUuid, action);
    }

    /**
     * Get and remove a pending action for a player.
     */
    public PendingAction getAndRemovePendingAction(UUID playerUuid) {
        return pendingActions.remove(playerUuid);
    }

    /**
     * Check if player has a pending action.
     */
    public boolean hasPendingAction(UUID playerUuid) {
        PendingAction action = pendingActions.get(playerUuid);
        if (action == null) {
            return false;
        }
        
        // Check if expired
        if (action.isExpired(ACTION_TIMEOUT_MS)) {
            pendingActions.remove(playerUuid);
            return false;
        }
        
        return true;
    }

    /**
     * Cancel a pending action.
     */
    public void cancelPendingAction(UUID playerUuid) {
        pendingActions.remove(playerUuid);
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            pendingActions.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired(ACTION_TIMEOUT_MS);
                if (expired) {
                    // Notify player on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            player.sendMessage("§cConfirmation timed out. Action cancelled.");
                        }
                    });
                }
                return expired;
            });
        }, 200L, 200L); // Run every 10 seconds
    }
}
