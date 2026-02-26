package dev.ignis.riaUserPages.serialization;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * Item serializer using Bukkit's native YAML serialization.
 * This is the default implementation that uses ItemStack.serialize().
 */
public class BukkitItemSerializer implements ItemSerializer {

    @Override
    public String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item.serialize());
        return config.saveToString();
    }

    @Override
    public ItemStack deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(data);
            
            if (!config.isConfigurationSection("item")) {
                System.err.println("[RiaUserPages] Deserialize failed: 'item' is not a configuration section");
                return null;
            }
            
            // Get the configuration section for "item"
            org.bukkit.configuration.ConfigurationSection itemSection = config.getConfigurationSection("item");
            if (itemSection == null) {
                System.err.println("[RiaUserPages] Deserialize failed: item section is null");
                return null;
            }
            
            // Convert configuration section to map
            java.util.Map<String, Object> itemMap = itemSection.getValues(true);
            if (itemMap == null || itemMap.isEmpty()) {
                System.err.println("[RiaUserPages] Deserialize failed: itemMap is null or empty");
                return null;
            }
            
            ItemStack item = ItemStack.deserialize(itemMap);
            if (item == null || item.getType().isAir()) {
                System.err.println("[RiaUserPages] Deserialize warning: resulting item is null or air");
            }
            return item;
        } catch (Exception e) {
            System.err.println("[RiaUserPages] Deserialize exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
