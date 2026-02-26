package dev.ignis.riaUserPages.gui;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.config.ConfigManager;
import dev.ignis.riaUserPages.localization.LocalizationManager;
import dev.ignis.riaUserPages.model.Comment;
import dev.ignis.riaUserPages.model.PendingAction;
import dev.ignis.riaUserPages.model.PlayerProfile;
import dev.ignis.riaUserPages.model.ProfileGUIState;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Manages the profile GUI display and interactions.
 */
public class ProfileGUI {

    private static final int GUI_SIZE = 54;
    private static final int DISPLAY_AREA_START = 2; // Column 2 (0-indexed)
    private static final int DISPLAY_AREA_WIDTH = 7;
    private static final int DISPLAY_AREA_HEIGHT = 6;
    private static final int DISPLAY_AREA_SLOTS = DISPLAY_AREA_WIDTH * DISPLAY_AREA_HEIGHT; // 42

    // Left column slot positions (0, 9, 18, 27, 36, 45)
    private static final int SLOT_PLAYER_HEAD = 0;
    private static final int SLOT_REGISTER_DATE = 9;
    private static final int SLOT_COMMENTS = 18;
    private static final int SLOT_LIKES = 27;
    private static final int SLOT_PREV_PAGE = 36;
    private static final int SLOT_NEXT_PAGE = 45;

    private final RiaUserPages plugin;
    private final ConfigManager config;
    private final LocalizationManager lang;
    private final Map<UUID, ProfileGUIState> activeGUIs;

    public ProfileGUI(RiaUserPages plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.lang = plugin.getLocalizationManager();
        this.activeGUIs = new HashMap<>();
    }

    /**
     * Check if a player is currently viewing a profile GUI.
     */
    public boolean isViewingProfileGUI(UUID playerUuid) {
        return activeGUIs.containsKey(playerUuid);
    }

    /**
     * Open a player's profile GUI.
     */
    public void openProfile(Player viewer, UUID targetUuid, int page) {
        PlayerProfile targetProfile = plugin.getProfileService().getProfile(targetUuid);
        if (targetProfile == null) {
            viewer.sendMessage(lang.getPrefixed("player-not-found"));
            return;
        }

        boolean editable = viewer.getUniqueId().equals(targetUuid);
        ProfileGUIState state = new ProfileGUIState(viewer.getUniqueId(), targetUuid, page, editable);
        
        String title = lang.get("gui.title", "%player%", targetProfile.getUsername(), "%page%", String.valueOf(page));
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, title);

        // Build the GUI
        buildGUI(inventory, state, targetProfile);

        // Store state and open
        activeGUIs.put(viewer.getUniqueId(), state);
        viewer.openInventory(inventory);
    }

    /**
     * Refresh the GUI for a player.
     */
    public void refreshGUI(Player viewer) {
        ProfileGUIState state = activeGUIs.get(viewer.getUniqueId());
        if (state == null) return;

        PlayerProfile targetProfile = plugin.getProfileService().getProfile(state.getTargetUuid());
        if (targetProfile == null) return;

        Inventory inventory = viewer.getOpenInventory().getTopInventory();
        if (inventory == null) return;

        buildGUI(inventory, state, targetProfile);
    }

    /**
     * Build or refresh the GUI contents.
     */
    private void buildGUI(Inventory inventory, ProfileGUIState state, PlayerProfile profile) {
        inventory.clear();

        // Left column - functional items
        setPlayerHead(inventory, SLOT_PLAYER_HEAD, profile, state);
        setRegisterDateItem(inventory, SLOT_REGISTER_DATE, profile);
        setCommentsItem(inventory, SLOT_COMMENTS, profile, state);
        setLikesItem(inventory, SLOT_LIKES, profile, state);
        setPageNavigation(inventory, SLOT_PREV_PAGE, SLOT_NEXT_PAGE, state, profile);

        // Separator column (column 1)
        setSeparators(inventory);

        // Display area (columns 2-8, rows 0-5) - depends on view mode
        if (state.getViewMode() == ProfileGUIState.ViewMode.COMMENTS) {
            loadCommentItems(inventory, state, profile);
        } else {
            loadDisplayItems(inventory, state, profile);
        }
    }

    private void setPlayerHead(Inventory inventory, int slot, PlayerProfile profile, ProfileGUIState state) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            // Change display name based on view mode
            if (state.getViewMode() == ProfileGUIState.ViewMode.COMMENTS) {
                meta.setDisplayName(lang.get("player-head.name-comments", "%player%", profile.getUsername()));
            } else {
                meta.setDisplayName(lang.get("player-head.name-profile", "%player%", profile.getUsername()));
            }
            
            List<String> lore = new ArrayList<>();
            // Bio on first line
            String bio = profile.getBio();
            if (bio == null || bio.isEmpty()) {
                bio = lang.get("player-head.bio-empty");
            }
            lore.add(lang.get("player-head.bio-label") + bio);
            lore.add("");
            lore.add(lang.get("player-head.likes-label") + profile.getTotalLikes());
            lore.add(lang.get("player-head.pages-label") + profile.getUnlockedPages());
            lore.add("");
            lore.add(lang.get("player-head.click-hint"));
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        inventory.setItem(slot, head);
    }

    private void setRegisterDateItem(Inventory inventory, int slot, PlayerProfile profile) {
        ItemStack item = new ItemStack(config.getRegisterDateItem());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get("register-date.name"));
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String dateStr = sdf.format(new Date(profile.getJoinedDate().getTime()));
            
            meta.setLore(Arrays.asList(
                lang.get("register-date.joined-label") + dateStr
            ));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void setCommentsItem(Inventory inventory, int slot, PlayerProfile profile, ProfileGUIState state) {
        int commentCount = plugin.getDatabaseManager().getCommentDAO().getCommentCount(profile.getUuid());
        Material material = getCommentButtonMaterial(commentCount);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get("comments.name", "%count%", String.valueOf(commentCount)));
            
            List<String> lore = new ArrayList<>();
            lore.add(lang.get("comments.click-hint"));
            lore.add("");
            
            // Show recent comments
            List<Comment> recentComments = plugin.getDatabaseManager().getCommentDAO()
                    .getRecentComments(profile.getUuid(), config.getCommentsDisplayCount());
            
            if (recentComments.isEmpty()) {
                lore.add(lang.get("comments.no-comments"));
            } else {
                lore.add(lang.get("comments.recent-label"));
                for (Comment comment : recentComments) {
                    String content = comment.getContent();
                    if (content.length() > 20) {
                        content = content.substring(0, 17) + "...";
                    }
                    lore.add("§7" + comment.getAuthorName() + ": §f" + content);
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void setLikesItem(Inventory inventory, int slot, PlayerProfile profile, ProfileGUIState state) {
        ItemStack item = new ItemStack(config.getLikeButtonItem());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get("likes.name"));
            
            int remainingLikes = plugin.getLikeService().getRemainingLikes(state.getViewerUuid());
            boolean hasLikedToday = plugin.getLikeService().hasLikedToday(state.getViewerUuid(), profile.getUuid());
            
            List<String> lore = new ArrayList<>();
            lore.add(lang.get("likes.total-label") + profile.getTotalLikes());
            lore.add(lang.get("likes.remaining-label") + remainingLikes);
            lore.add("");
            
            if (state.isViewingOwnProfile()) {
                lore.add(lang.get("likes.cannot-like-self"));
            } else if (hasLikedToday) {
                lore.add(lang.get("likes.already-liked"));
            } else if (remainingLikes > 0) {
                lore.add(lang.get("likes.can-like"));
            } else {
                lore.add(lang.get("likes.no-likes-remaining"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void setPageNavigation(Inventory inventory, int prevSlot, int nextSlot, ProfileGUIState state, PlayerProfile profile) {
        int currentPage = state.getCurrentPage();
        int unlockedPages = profile.getUnlockedPages();

        // Previous page button
        ItemStack prevItem = new ItemStack(config.getPrevPageItem());
        ItemMeta prevMeta = prevItem.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName(lang.get("pagination.prev"));
            if (currentPage <= 1) {
                prevMeta.setLore(Arrays.asList(lang.get("pagination.first-page")));
                // Make it look disabled (grayed out)
                prevItem = new ItemStack(Material.GRAY_DYE);
                prevItem.setItemMeta(prevMeta);
            } else {
                prevMeta.setLore(Arrays.asList("§7Go to page " + (currentPage - 1)));
                prevItem.setItemMeta(prevMeta);
            }
        }
        inventory.setItem(prevSlot, prevItem);

        // Next page button
        ItemStack nextItem = new ItemStack(config.getNextPageItem());
        ItemMeta nextMeta = nextItem.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(lang.get("pagination.next"));
            if (currentPage >= unlockedPages) {
                nextMeta.setLore(Arrays.asList(lang.get("pagination.last-page")));
                // Make it look disabled
                nextItem = new ItemStack(Material.GRAY_DYE);
                nextItem.setItemMeta(nextMeta);
            } else {
                nextMeta.setLore(Arrays.asList(lang.get("pagination.page-info", "%current%", String.valueOf(currentPage + 1), "%max%", String.valueOf(unlockedPages))));
                nextItem.setItemMeta(nextMeta);
            }
        }
        inventory.setItem(nextSlot, nextItem);
    }

    private void setSeparators(Inventory inventory) {
        ItemStack separator = new ItemStack(config.getSeparatorItem());
        ItemMeta meta = separator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.get("separator.name"));
            separator.setItemMeta(meta);
        }
        
        for (int row = 0; row < 6; row++) {
            inventory.setItem(row * 9 + 1, separator);
        }
    }

    private void loadDisplayItems(Inventory inventory, ProfileGUIState state, PlayerProfile profile) {
        Map<Integer, ItemStack> items = plugin.getDatabaseManager().getInventoryDAO()
                .getPageItems(profile.getUuid(), state.getCurrentPage());
        
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slotIndex = entry.getKey();
            if (slotIndex >= 0 && slotIndex < DISPLAY_AREA_SLOTS) {
                // Convert display area index to inventory slot
                int row = slotIndex / DISPLAY_AREA_WIDTH;
                int col = slotIndex % DISPLAY_AREA_WIDTH;
                int inventorySlot = row * 9 + DISPLAY_AREA_START + col;
                
                inventory.setItem(inventorySlot, entry.getValue());
            }
        }
    }

    private void loadCommentItems(Inventory inventory, ProfileGUIState state, PlayerProfile profile) {
        List<Comment> comments = plugin.getDatabaseManager().getCommentDAO()
                .getComments(profile.getUuid());
        
        // Calculate pagination
        int commentsPerPage = DISPLAY_AREA_SLOTS; // 42 comments per page
        int totalPages = (int) Math.ceil((double) comments.size() / commentsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        int currentPage = Math.min(state.getCurrentPage(), totalPages);
        int startIndex = (currentPage - 1) * commentsPerPage;
        int endIndex = Math.min(startIndex + commentsPerPage, comments.size());
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        for (int i = startIndex; i < endIndex; i++) {
            Comment comment = comments.get(i);
            int displayIndex = i - startIndex;
            
            // Convert display index to inventory slot
            int row = displayIndex / DISPLAY_AREA_WIDTH;
            int col = displayIndex % DISPLAY_AREA_WIDTH;
            int inventorySlot = row * 9 + DISPLAY_AREA_START + col;
            
            // Create player head for comment
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + comment.getAuthorName());
                
                List<String> lore = new ArrayList<>();
                lore.add(lang.get("comment-item.date-label") + sdf.format(new Date(comment.getCreatedAt().getTime())));
                lore.add("");
                
                // Split comment content into multiple lines if needed
                String content = comment.getContent();
                int maxLineLength = 30;
                for (int j = 0; j < content.length(); j += maxLineLength) {
                    int end = Math.min(j + maxLineLength, content.length());
                    lore.add("§f" + content.substring(j, end));
                }
                
                // Add delete hint if viewer can delete
                Player viewer = state.getViewer();
                boolean canDelete = state.isViewingOwnProfile() || 
                        (viewer != null && viewer.hasPermission("ria.admin.*"));
                if (canDelete) {
                    lore.add("");
                    lore.add(lang.get("comment-item.click-delete"));
                    lore.add(lang.get("comments.id-label") + comment.getId());
                }
                
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            
            // Store comment ID in item's persistent data for click handling
            head = createCommentItem(head, comment.getId());
            inventory.setItem(inventorySlot, head);
        }
        
        // Fill empty slots with glass panes
        for (int i = endIndex - startIndex; i < DISPLAY_AREA_SLOTS; i++) {
            int row = i / DISPLAY_AREA_WIDTH;
            int col = i % DISPLAY_AREA_WIDTH;
            int inventorySlot = row * 9 + DISPLAY_AREA_START + col;
            
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang.get("empty-slot.name"));
                empty.setItemMeta(meta);
            }
            inventory.setItem(inventorySlot, empty);
        }
    }

    private ItemStack createCommentItem(ItemStack item, long commentId) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "comment_id"),
                org.bukkit.persistence.PersistentDataType.LONG,
                commentId
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public Long getCommentIdFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        return meta.getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "comment_id"),
            org.bukkit.persistence.PersistentDataType.LONG
        );
    }

    private Material getCommentButtonMaterial(int commentCount) {
        Material material = config.getCommentBaseItem();
        
        Map<Integer, Material> thresholds = config.getCommentThresholds();
        int highestMatch = 0;
        
        for (Map.Entry<Integer, Material> entry : thresholds.entrySet()) {
            if (commentCount >= entry.getKey() && entry.getKey() > highestMatch) {
                highestMatch = entry.getKey();
                material = entry.getValue();
            }
        }
        
        return material;
    }

    /**
     * Handle GUI click events.
     */
    public void handleClick(Player player, int slot, ItemStack clickedItem) {
        ProfileGUIState state = activeGUIs.get(player.getUniqueId());
        if (state == null) return;

        PlayerProfile profile = plugin.getProfileService().getProfile(state.getTargetUuid());
        if (profile == null) return;

        switch (slot) {
            case SLOT_PLAYER_HEAD:
                // Toggle between profile and comments view
                state.toggleViewMode();
                state.setCurrentPage(1); // Reset to page 1 when switching
                refreshGUI(player);
                break;
            case SLOT_COMMENTS:
                handleCommentsClick(player, state, profile);
                break;
            case SLOT_LIKES:
                handleLikesClick(player, state, profile);
                break;
            case SLOT_PREV_PAGE:
                if (state.getCurrentPage() > 1) {
                    state.setCurrentPage(state.getCurrentPage() - 1);
                    refreshGUI(player);
                }
                break;
            case SLOT_NEXT_PAGE:
                int maxPages = getMaxPagesForCurrentView(state, profile);
                if (state.getCurrentPage() < maxPages) {
                    state.setCurrentPage(state.getCurrentPage() + 1);
                    refreshGUI(player);
                }
                break;
            default:
                // Check if it's in the display area
                if (state.getViewMode() == ProfileGUIState.ViewMode.COMMENTS) {
                    handleCommentAreaClick(player, slot, clickedItem, state, profile);
                } else {
                    handleDisplayAreaClick(player, slot, state);
                }
                break;
        }
    }

    private int getMaxPagesForCurrentView(ProfileGUIState state, PlayerProfile profile) {
        if (state.getViewMode() == ProfileGUIState.ViewMode.COMMENTS) {
            int commentCount = plugin.getDatabaseManager().getCommentDAO().getCommentCount(profile.getUuid());
            int totalPages = (int) Math.ceil((double) commentCount / DISPLAY_AREA_SLOTS);
            return Math.max(1, totalPages);
        } else {
            return profile.getUnlockedPages();
        }
    }

    private void handleCommentsClick(Player player, ProfileGUIState state, PlayerProfile profile) {
        // Close GUI and send clickable message
        player.closeInventory();
        
        TextComponent message = new TextComponent(lang.get("prefix").replace("&", "§") + lang.get("comments.click-hint") + " " + profile.getUsername());
        message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                "/ria-player-profile comment " + profile.getUsername() + " "));
        player.spigot().sendMessage(message);
    }

    private void handleLikesClick(Player player, ProfileGUIState state, PlayerProfile profile) {
        if (state.isViewingOwnProfile()) {
            player.sendMessage(lang.getPrefixed("cannot-like-self"));
            return;
        }

        plugin.getLikeService().likeProfile(player, profile.getUuid()).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS:
                        int remaining = plugin.getLikeService().getRemainingLikes(player.getUniqueId());
                        player.sendMessage(lang.getPrefixed("like-success", 
                                "%player%", profile.getUsername(),
                                "%remaining%", String.valueOf(remaining)));
                        refreshGUI(player);
                        break;
                    case ALREADY_LIKED:
                        player.sendMessage(lang.getPrefixed("like-already"));
                        break;
                    case LIMIT_REACHED:
                        player.sendMessage(lang.getPrefixed("like-limit-reached"));
                        break;
                    case ERROR:
                        player.sendMessage(lang.getPrefixed("database-error"));
                        break;
                }
            });
        });
    }

    private void handleDisplayAreaClick(Player player, int slot, ProfileGUIState state) {
        if (!state.isEditable()) return;

        // Calculate display area index from inventory slot
        int row = slot / 9;
        int col = slot % 9;
        
        if (col < DISPLAY_AREA_START) return; // Not in display area
        
        int displayCol = col - DISPLAY_AREA_START;
        if (displayCol >= DISPLAY_AREA_WIDTH) return;
        
        int displayIndex = row * DISPLAY_AREA_WIDTH + displayCol;
        if (displayIndex >= DISPLAY_AREA_SLOTS) return;

        // Handle item placement/removal - this is handled by the inventory listener
        // The actual item modification happens in the GUI listener
    }

    private void handleCommentAreaClick(Player player, int slot, ItemStack clickedItem, ProfileGUIState state, PlayerProfile profile) {
        // Check if viewer can delete comments
        boolean canDelete = state.isViewingOwnProfile() || player.hasPermission("ria.admin.*");
        if (!canDelete) return;

        // Get comment ID from clicked item
        Long commentId = getCommentIdFromItem(clickedItem);
        if (commentId == null) return;

        // Close GUI and ask for confirmation
        player.closeInventory();
        
        // Store pending action
        PendingAction action = new PendingAction(player.getUniqueId(), PendingAction.ActionType.DELETE_COMMENT, commentId);
        plugin.getPendingActionManager().addPendingAction(player.getUniqueId(), action);
        
        // Send confirmation message with clickable buttons
        player.sendMessage("");
        player.sendMessage(lang.getPrefixed("comment-delete-confirm", "%id%", String.valueOf(commentId)));
        
        TextComponent confirmButton = new TextComponent(lang.get("confirm.confirm-button"));
        confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/ria-player-profile confirm"));
        
        TextComponent cancelButton = new TextComponent(lang.get("confirm.cancel-button"));
        cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/ria-player-profile cancel"));
        
        TextComponent space = new TextComponent(" §7| ");
        
        player.spigot().sendMessage(confirmButton, space, cancelButton);
        player.sendMessage(lang.get("confirm.timeout").replace("&", "§"));
        player.sendMessage("");
    }

    /**
     * Request confirmation for deleting a display item.
     */
    public void requestItemDeletionConfirmation(Player player, int slot) {
        ProfileGUIState state = activeGUIs.get(player.getUniqueId());
        if (state == null) return;
        
        // Check if player can edit
        boolean canEdit = state.isViewingOwnProfile() || player.hasPermission("ria.admin.*");
        if (!canEdit) return;
        
        // Get display index and store info before closing GUI
        int displayIndex = getDisplayAreaIndex(slot);
        if (displayIndex < 0) return;
        
        int currentPage = state.getCurrentPage();
        UUID targetUuid = state.getTargetUuid();
        
        // Close GUI and ask for confirmation
        player.closeInventory();
        
        // Store pending action with all necessary info
        PendingAction action = new PendingAction(player.getUniqueId(), PendingAction.ActionType.DELETE_DISPLAY_ITEM, slot, currentPage, targetUuid);
        plugin.getPendingActionManager().addPendingAction(player.getUniqueId(), action);
        
        // Send confirmation message with clickable buttons
        player.sendMessage("");
        player.sendMessage(lang.getPrefixed("item-delete-confirm"));
        
        TextComponent confirmButton = new TextComponent(lang.get("confirm.confirm-button"));
        confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/ria-player-profile confirm"));
        
        TextComponent cancelButton = new TextComponent(lang.get("confirm.cancel-button"));
        cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                "/ria-player-profile cancel"));
        
        TextComponent space = new TextComponent(" §7| ");
        
        player.spigot().sendMessage(confirmButton, space, cancelButton);
        player.sendMessage(lang.get("confirm.timeout").replace("&", "§"));
        player.sendMessage("");
    }

    /**
     * Get the display area slot index from inventory slot, or -1 if not in display area.
     */
    public int getDisplayAreaIndex(int inventorySlot) {
        int row = inventorySlot / 9;
        int col = inventorySlot % 9;
        
        if (col < DISPLAY_AREA_START || col >= DISPLAY_AREA_START + DISPLAY_AREA_WIDTH) {
            return -1;
        }
        
        int displayCol = col - DISPLAY_AREA_START;
        int index = row * DISPLAY_AREA_WIDTH + displayCol;
        
        return index < DISPLAY_AREA_SLOTS ? index : -1;
    }

    /**
     * Check if a slot is in the functional area (left column).
     */
    public boolean isFunctionalSlot(int slot) {
        return slot == SLOT_PLAYER_HEAD || slot == SLOT_REGISTER_DATE || 
               slot == SLOT_COMMENTS || slot == SLOT_LIKES || 
               slot == SLOT_PREV_PAGE || slot == SLOT_NEXT_PAGE;
    }

    /**
     * Get the GUI state for a player.
     */
    public ProfileGUIState getGUIState(UUID playerUuid) {
        return activeGUIs.get(playerUuid);
    }

    /**
     * Remove a player's GUI state.
     */
    public void removeGUIState(UUID playerUuid) {
        activeGUIs.remove(playerUuid);
    }

    /**
     * Save an item to the current page.
     */
    public void saveItemToPage(Player player, int slot, ItemStack item) {
        ProfileGUIState state = activeGUIs.get(player.getUniqueId());
        if (state == null) return;
        
        // Check if player can edit: own profile or admin
        boolean canEdit = state.isViewingOwnProfile() || player.hasPermission("ria.admin.*");
        if (!canEdit) return;

        int displayIndex = getDisplayAreaIndex(slot);
        if (displayIndex < 0) return;

        plugin.getDatabaseManager().getInventoryDAO().saveItem(
                state.getTargetUuid(), state.getCurrentPage(), displayIndex, item);
    }

    /**
     * Remove an item from the current page.
     */
    public void removeItemFromPage(Player player, int slot) {
        ProfileGUIState state = activeGUIs.get(player.getUniqueId());
        if (state == null) return;
        
        // Check if player can edit: own profile or admin
        boolean canEdit = state.isViewingOwnProfile() || player.hasPermission("ria.admin.*");
        if (!canEdit) return;

        int displayIndex = getDisplayAreaIndex(slot);
        if (displayIndex < 0) return;

        // Check if slot has an item
        ItemStack currentItem = player.getOpenInventory().getTopInventory().getItem(slot);
        if (currentItem == null || currentItem.getType().isAir()) {
            return; // Nothing to delete
        }

        // Request confirmation
        requestItemDeletionConfirmation(player, slot);
    }

    /**
     * Actually delete the item after confirmation.
     * This method receives the PendingAction which contains all necessary info.
     */
    public void confirmItemDeletion(Player player, PendingAction action) {
        if (action == null || action.getType() != PendingAction.ActionType.DELETE_DISPLAY_ITEM) {
            player.sendMessage(lang.getPrefixed("no-pending-action"));
            return;
        }

        int slot = action.getSlot();
        int page = action.getPage();
        UUID targetUuid = action.getTargetUuid();
        
        int displayIndex = getDisplayAreaIndex(slot);
        if (displayIndex < 0) {
            player.sendMessage(lang.getPrefixed("database-error"));
            return;
        }

        plugin.getDatabaseManager().getInventoryDAO().removeItem(targetUuid, page, displayIndex);
        
        player.sendMessage(lang.getPrefixed("item-delete-success"));
    }
}
