package dev.ignis.riaUserPages.service;

import dev.ignis.riaUserPages.RiaUserPages;
import dev.ignis.riaUserPages.database.CommentDAO;
import dev.ignis.riaUserPages.model.Comment;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing profile comments.
 */
public class CommentService {

    private final RiaUserPages plugin;
    private final CommentDAO commentDAO;

    public CommentService(RiaUserPages plugin) {
        this.plugin = plugin;
        this.commentDAO = plugin.getDatabaseManager().getCommentDAO();
    }

    /**
     * Add a comment to a profile.
     */
    public CompletableFuture<Boolean> addComment(Player author, UUID targetUuid, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                commentDAO.addComment(author.getUniqueId(), author.getName(), targetUuid, content);
                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Error adding comment: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get all comments for a profile.
     */
    public List<Comment> getComments(UUID targetUuid) {
        return commentDAO.getComments(targetUuid);
    }

    /**
     * Get recent comments for a profile.
     */
    public List<Comment> getRecentComments(UUID targetUuid, int limit) {
        return commentDAO.getRecentComments(targetUuid, limit);
    }

    /**
     * Get comment count for a profile.
     */
    public int getCommentCount(UUID targetUuid) {
        return commentDAO.getCommentCount(targetUuid);
    }

    /**
     * Delete a comment.
     */
    public boolean deleteComment(long commentId) {
        return commentDAO.deleteComment(commentId);
    }

    /**
     * Get a specific comment.
     */
    public Comment getComment(long commentId) {
        return commentDAO.getComment(commentId).orElse(null);
    }
}
