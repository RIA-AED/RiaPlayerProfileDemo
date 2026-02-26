package dev.ignis.riaUserPages.model;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a player's profile data.
 */
public class PlayerProfile {

    private final UUID uuid;
    private String username;
    private String bio;
    private final Timestamp joinedDate;
    private int totalLikes;
    private int unlockedPages;

    public PlayerProfile(UUID uuid, String username, String bio, Timestamp joinedDate, int totalLikes, int unlockedPages) {
        this.uuid = uuid;
        this.username = username;
        this.bio = bio;
        this.joinedDate = joinedDate;
        this.totalLikes = totalLikes;
        this.unlockedPages = unlockedPages;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Timestamp getJoinedDate() {
        return joinedDate;
    }

    public int getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(int totalLikes) {
        this.totalLikes = totalLikes;
    }

    public int getUnlockedPages() {
        return unlockedPages;
    }

    public void setUnlockedPages(int unlockedPages) {
        this.unlockedPages = unlockedPages;
    }
}
