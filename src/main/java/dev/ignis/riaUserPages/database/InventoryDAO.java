package dev.ignis.riaUserPages.database;

import dev.ignis.riaUserPages.serialization.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data Access Object for player inventory items.
 */
public class InventoryDAO {

    private final DatabaseManager databaseManager;
    private final String tableName;
    private final ItemSerializer itemSerializer;

    public InventoryDAO(DatabaseManager databaseManager, ItemSerializer itemSerializer) {
        this.databaseManager = databaseManager;
        this.tableName = databaseManager.getTablePrefix() + "inventories";
        this.itemSerializer = itemSerializer;
    }

    /**
     * Get all items for a specific page.
     */
    public Map<Integer, ItemStack> getPageItems(UUID ownerUuid, int pageNumber) {
        Map<Integer, ItemStack> items = new HashMap<>();
        String sql = "SELECT slot_index, item_nbt FROM " + tableName + " WHERE owner_uuid = ? AND page_number = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setInt(2, pageNumber);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int slot = rs.getInt("slot_index");
                String nbtData = rs.getString("item_nbt");
                ItemStack item = itemSerializer.deserialize(nbtData);
                if (item != null) {
                    items.put(slot, item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get page items", e);
        }
        return items;
    }

    /**
     * Save an item to a specific slot.
     */
    public void saveItem(UUID ownerUuid, int pageNumber, int slotIndex, ItemStack item) {
        String sql = "INSERT INTO " + tableName + " (owner_uuid, page_number, slot_index, item_nbt) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE item_nbt = VALUES(item_nbt)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setInt(2, pageNumber);
            stmt.setInt(3, slotIndex);
            stmt.setString(4, itemSerializer.serialize(item));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item", e);
        }
    }

    /**
     * Remove an item from a specific slot.
     */
    public void removeItem(UUID ownerUuid, int pageNumber, int slotIndex) {
        String sql = "DELETE FROM " + tableName + " WHERE owner_uuid = ? AND page_number = ? AND slot_index = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setInt(2, pageNumber);
            stmt.setInt(3, slotIndex);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove item", e);
        }
    }

    /**
     * Clear all items on a specific page.
     */
    public int clearPage(UUID ownerUuid, int pageNumber) {
        String sql = "DELETE FROM " + tableName + " WHERE owner_uuid = ? AND page_number = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setInt(2, pageNumber);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear page", e);
        }
    }

    /**
     * Clear all items for a player.
     */
    public void clearAllItems(UUID ownerUuid) {
        String sql = "DELETE FROM " + tableName + " WHERE owner_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear all items", e);
        }
    }

    /**
     * Update owner UUID (for migration).
     */
    public void updateOwnerUuid(String oldUuid, String newUuid) {
        String sql = "UPDATE " + tableName + " SET owner_uuid = ? WHERE owner_uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newUuid);
            stmt.setString(2, oldUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update owner UUID", e);
        }
    }
}
