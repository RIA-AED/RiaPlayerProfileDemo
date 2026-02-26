package dev.ignis.riaUserPages.model;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a comment on a player's profile.
 */
public class Comment {

    private final long id;
    private final UUID authorUuid;
    private final String authorName;
    private final UUID targetUuid;
    private final String content;
    private final Timestamp createdAt;

    public Comment(long id, UUID authorUuid, String authorName, UUID targetUuid, String content, Timestamp createdAt) {
        this.id = id;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.targetUuid = targetUuid;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public UUID getAuthorUuid() {
        return authorUuid;
    }

    public String getAuthorName() {
        return authorName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getContent() {
        return content;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
