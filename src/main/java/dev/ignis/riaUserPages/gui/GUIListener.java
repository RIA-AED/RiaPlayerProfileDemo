package dev.ignis.riaUserPages.gui;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.model.ProfileGUIState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for GUI-related events.
 */
public class GUIListener implements Listener {

    private final RiaUserPages plugin;
    private final ProfileGUI profileGUI;

    public GUIListener(RiaUserPages plugin) {
        this.plugin = plugin;
        this.profileGUI = plugin.getProfileGUI();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if player is viewing our GUI
        if (!profileGUI.isViewingProfileGUI(player.getUniqueId())) return;
        
        ProfileGUIState state = profileGUI.getGUIState(player.getUniqueId());
        if (state == null) return;

        // ALWAYS cancel the event first to prevent any item movement
        event.setCancelled(true);

        int slot = event.getRawSlot();
        
        // Clicked in player inventory (bottom inventory) - allow but cancel shift-clicks
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            // Only allow if it's not a shift-click
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // This is a shift-click into the GUI - always block
                return;
            }
            // Allow other clicks in player inventory
            event.setCancelled(false);
            return;
        }

        // Handle functional buttons
        if (profileGUI.isFunctionalSlot(slot)) {
            profileGUI.handleClick(player, slot, event.getCurrentItem());
            return;
        }

        // Handle display area interactions
        int displayIndex = profileGUI.getDisplayAreaIndex(slot);
        if (displayIndex >= 0) {
            // In COMMENTS mode, just handle click (for deletion)
            if (state.getViewMode() == ProfileGUIState.ViewMode.COMMENTS) {
                profileGUI.handleClick(player, slot, event.getCurrentItem());
                return;
            }
            
            // In PROFILE mode, allow editing if:
            // 1. Player is viewing their own profile, OR
            // 2. Player has admin permission
            boolean canEdit = state.isViewingOwnProfile() || player.hasPermission("ria.admin.*");
            if (canEdit) {
                handleDisplayAreaEdit(event, player, state, slot, displayIndex);
            }
            return;
        }
    }

    private void handleDisplayAreaEdit(InventoryClickEvent event, Player player, 
            ProfileGUIState state, int slot, int displayIndex) {
        
        InventoryAction action = event.getAction();
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Always cancelled at this point, we manually handle the visual updates
        
        switch (action) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
                // Placing an item - clone without consuming from inventory
                if (cursor != null && !cursor.getType().isAir()) {
                    ItemStack toPlace = cursor.clone();
                    toPlace.setAmount(1); // Always place 1 item
                    
                    // Save to database
                    profileGUI.saveItemToPage(player, slot, toPlace);
                    
                    // Update GUI visually (cursor stays the same)
                    event.getInventory().setItem(slot, toPlace);
                }
                break;

            case PICKUP_ALL:
            case PICKUP_ONE:
            case PICKUP_HALF:
            case PICKUP_SOME:
                // Removing an item - just remove from display, don't give to player
                if (current != null && !current.getType().isAir()) {
                    profileGUI.removeItemFromPage(player, slot);
                    // Clear the slot visually
                    event.getInventory().setItem(slot, null);
                }
                break;

            case SWAP_WITH_CURSOR:
                // Swapping items - save clone of cursor, but don't consume it
                if (cursor != null && !cursor.getType().isAir()) {
                    ItemStack toPlace = cursor.clone();
                    toPlace.setAmount(1);
                    profileGUI.saveItemToPage(player, slot, toPlace);
                    event.getInventory().setItem(slot, toPlace);
                } else {
                    // If cursor is empty, just remove the item
                    profileGUI.removeItemFromPage(player, slot);
                    event.getInventory().setItem(slot, null);
                }
                break;

            case HOTBAR_MOVE_AND_READD:
            case HOTBAR_SWAP:
                // Hotbar swap - clone from hotbar without removing
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (hotbarItem != null && !hotbarItem.getType().isAir()) {
                    ItemStack toPlace = hotbarItem.clone();
                    toPlace.setAmount(1);
                    profileGUI.saveItemToPage(player, slot, toPlace);
                    event.getInventory().setItem(slot, toPlace);
                } else {
                    // If hotbar slot is empty, remove the display item
                    profileGUI.removeItemFromPage(player, slot);
                    event.getInventory().setItem(slot, null);
                }
                break;

            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:
            case MOVE_TO_OTHER_INVENTORY:
            case UNKNOWN:
            default:
                // Do nothing for these actions
                break;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if player is viewing our GUI
        if (!profileGUI.isViewingProfileGUI(player.getUniqueId())) return;
        
        ProfileGUIState state = profileGUI.getGUIState(player.getUniqueId());
        if (state == null) return;

        // ALWAYS cancel drag first
        event.setCancelled(true);

        // In COMMENTS mode, do nothing else
        if (state.getViewMode() == ProfileGUIState.ViewMode.COMMENTS) {
            return;
        }

        // In PROFILE mode, check permissions
        boolean canEdit = state.isViewingOwnProfile() || player.hasPermission("ria.admin.*");
        if (!canEdit) {
            return;
        }

        // Check if all dragged slots are in the display area
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                if (profileGUI.isFunctionalSlot(slot) || profileGUI.getDisplayAreaIndex(slot) < 0) {
                    return; // Don't allow drag if any slot is outside display area
                }
            }
        }

        // Get the item being dragged (from cursor)
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null || draggedItem.getType().isAir()) {
            return;
        }

        // Place the item in all dragged slots
        ItemStack toPlace = draggedItem.clone();
        toPlace.setAmount(1);
        
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                int displayIndex = profileGUI.getDisplayAreaIndex(slot);
                if (displayIndex >= 0) {
                    profileGUI.saveItemToPage(player, slot, toPlace);
                    event.getInventory().setItem(slot, toPlace.clone());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Check if player was viewing our GUI
        if (profileGUI.isViewingProfileGUI(player.getUniqueId())) {
            profileGUI.removeGUIState(player.getUniqueId());
        }
    }
}
