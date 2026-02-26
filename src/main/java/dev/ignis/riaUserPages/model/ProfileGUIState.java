package dev.ignis.riaUserPages.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Tracks the state of a profile GUI session.
 */
public class ProfileGUIState {

    public enum ViewMode {
        PROFILE,    // 显示玩家主页（物品展示）
        COMMENTS    // 显示留言列表
    }

    private final UUID viewerUuid;
    private final UUID targetUuid;
    private int currentPage;
    private final boolean editable;
    private ViewMode viewMode;

    public ProfileGUIState(UUID viewerUuid, UUID targetUuid, int currentPage, boolean editable) {
        this.viewerUuid = viewerUuid;
        this.targetUuid = targetUuid;
        this.currentPage = currentPage;
        this.editable = editable;
        this.viewMode = ViewMode.PROFILE;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public Player getViewer() {
        return Bukkit.getPlayer(viewerUuid);
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isViewingOwnProfile() {
        return viewerUuid.equals(targetUuid);
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setViewMode(ViewMode viewMode) {
        this.viewMode = viewMode;
    }

    public void toggleViewMode() {
        this.viewMode = (this.viewMode == ViewMode.PROFILE) ? ViewMode.COMMENTS : ViewMode.PROFILE;
    }
}
