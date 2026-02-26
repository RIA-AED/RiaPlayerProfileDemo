package dev.ignis.riaUserPages.model;

import java.util.UUID;

/**
 * Represents a pending action that requires user confirmation.
 */
public class PendingAction {
    
    public enum ActionType {
        DELETE_COMMENT,
        DELETE_DISPLAY_ITEM
    }
    
    private final UUID playerUuid;
    private final ActionType type;
    private final long commentId;   // For DELETE_COMMENT
    private final int slot;         // For DELETE_DISPLAY_ITEM
    private final int page;         // For DELETE_DISPLAY_ITEM
    private final UUID targetUuid;  // For DELETE_DISPLAY_ITEM
    private final long timestamp;
    
    // For comment deletion
    public PendingAction(UUID playerUuid, ActionType type, long commentId) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.commentId = commentId;
        this.slot = -1;
        this.page = -1;
        this.targetUuid = null;
        this.timestamp = System.currentTimeMillis();
    }
    
    // For display item deletion
    public PendingAction(UUID playerUuid, ActionType type, int slot, int page, UUID targetUuid) {
        this.playerUuid = playerUuid;
        this.type = type;
        this.commentId = -1;
        this.slot = slot;
        this.page = page;
        this.targetUuid = targetUuid;
        this.timestamp = System.currentTimeMillis();
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public ActionType getType() {
        return type;
    }
    
    public long getCommentId() {
        return commentId;
    }
    
    public int getSlot() {
        return slot;
    }
    
    public int getPage() {
        return page;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - timestamp > timeoutMs;
    }
}
