package dev.ignis.riaUserPages.localization;

import dev.ignis.riaUserPages.RiaUserPages;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages GUI and message localization.
 */
public class LocalizationManager {

    private final RiaUserPages plugin;
    private final Map<String, String> guiMessages;
    private String currentLanguage;

    public LocalizationManager(RiaUserPages plugin) {
        this.plugin = plugin;
        this.guiMessages = new HashMap<>();
    }

    /**
     * Load localization files.
     */
    public void load() {
        // Save default localization files if not exist
        saveDefaultLocalization("zh_cn");
        saveDefaultLocalization("en_us");

        // Load language setting from config
        String lang = plugin.getConfig().getString("language", "zh_cn").toLowerCase();
        this.currentLanguage = lang;

        // Load the localization file
        loadLocalizationFile(lang);
    }

    /**
     * Reload localization.
     */
    public void reload() {
        guiMessages.clear();
        load();
    }

    /**
     * Get current language.
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Get a localized GUI message.
     */
    public String get(String key) {
        return guiMessages.getOrDefault(key, "Missing: " + key);
    }

    /**
     * Get a localized GUI message with placeholders replaced.
     */
    public String get(String key, String... replacements) {
        String message = get(key);
        message = message.replace("&", "§");

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return message;
    }

    /**
     * Get a message with prefix.
     */
    public String getPrefixed(String key, String... replacements) {
        String prefix = get("prefix").replace("&", "§");
        return prefix + get(key, replacements);
    }

    private void saveDefaultLocalization(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            plugin.saveResource("lang/" + lang + ".yml", false);
        }
    }

    private void loadLocalizationFile(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Localization file not found: " + lang + ".yml, falling back to zh_cn");
            langFile = new File(plugin.getDataFolder(), "lang/zh_cn.yml");
            if (!langFile.exists()) {
                plugin.getLogger().severe("Default localization file not found!");
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);

        // Load all keys
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                guiMessages.put(key, config.getString(key));
            }
        }

        plugin.getLogger().info("Loaded localization: " + lang);
    }
}
