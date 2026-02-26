package dev.ignis.riaUserPages.serialization;

import org.bukkit.inventory.ItemStack;

/**
 * Interface for item serialization/deserialization.
 * Allows for easy swapping of serialization strategies.
 */
public interface ItemSerializer {

    /**
     * Serialize an ItemStack to a string.
     *
     * @param item The item to serialize
     * @return The serialized string representation
     */
    String serialize(ItemStack item);

    /**
     * Deserialize a string back to an ItemStack.
     *
     * @param data The serialized string
     * @return The deserialized ItemStack, or null if invalid
     */
    ItemStack deserialize(String data);
}
