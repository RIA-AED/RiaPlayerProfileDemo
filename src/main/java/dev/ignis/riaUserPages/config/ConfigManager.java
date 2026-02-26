package dev.ignis.riaUserPages.config;

import dev.ignis.riaUserPages.RiaUserPages;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages plugin configuration and provides easy access to settings.
 */
public class ConfigManager {

    private final RiaUserPages plugin;
    private FileConfiguration config;

    // Database settings
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUser;
    private String dbPassword;
    private String tablePrefix;

    // UI settings
    private Material separatorItem;
    private Material registerDateItem;
    private Material likeButtonItem;
    private Material prevPageItem;
    private Material nextPageItem;
    private Material commentBaseItem;
    private Map<Integer, Material> commentThresholds;
    private int commentsDisplayCount;

    // Settings
    private int likeLimitPerDay;
    private int defaultPages;
    private int maxPages;
    private int maxCommentLength;

    // Migration settings
    private boolean allowOverwrite;

    public ConfigManager(RiaUserPages plugin) {
        this.plugin = plugin;
    }

    /**
     * Load configuration from file.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        loadDatabaseSettings();
        loadUISettings();
        loadSettings();
        loadMigrationSettings();
    }

    /**
     * Reload configuration.
     */
    public void reload() {
        load();
    }

    private void loadDatabaseSettings() {
        ConfigurationSection db = config.getConfigurationSection("database");
        if (db != null) {
            this.dbHost = db.getString("host", "localhost");
            this.dbPort = db.getInt("port", 3306);
            this.dbDatabase = db.getString("database", "ria_db");
            this.dbUser = db.getString("user", "root");
            this.dbPassword = db.getString("password", "");
            this.tablePrefix = db.getString("table-prefix", "ria_");
        }
    }

    private void loadUISettings() {
        ConfigurationSection ui = config.getConfigurationSection("ui");
        if (ui != null) {
            this.separatorItem = parseMaterial(ui.getString("separator-item", "YELLOW_STAINED_GLASS_PANE"));
            this.registerDateItem = parseMaterial(ui.getString("register-date-item", "CLOCK"));
            this.likeButtonItem = parseMaterial(ui.getString("like-button-item", "SUNFLOWER"));
            this.prevPageItem = parseMaterial(ui.getString("prev-page-item", "ARROW"));
            this.nextPageItem = parseMaterial(ui.getString("next-page-item", "ARROW"));
            
            // Comment button settings
            ConfigurationSection commentBtn = ui.getConfigurationSection("comment-button");
            if (commentBtn != null) {
                this.commentBaseItem = parseMaterial(commentBtn.getString("base-item", "AMETHYST_SHARD"));
                this.commentThresholds = new HashMap<>();
                
                ConfigurationSection thresholds = commentBtn.getConfigurationSection("thresholds");
                if (thresholds != null) {
                    for (String key : thresholds.getKeys(false)) {
                        try {
                            int count = Integer.parseInt(key);
                            Material material = parseMaterial(thresholds.getString(key));
                            if (material != null) {
                                commentThresholds.put(count, material);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            
            this.commentsDisplayCount = ui.getInt("comments-display-count", 3);
        }
    }

    private void loadSettings() {
        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            this.likeLimitPerDay = settings.getInt("like-limit-per-day", 10);
            this.defaultPages = settings.getInt("default-pages", 1);
            this.maxPages = settings.getInt("max-pages", 3);
            this.maxCommentLength = settings.getInt("max-comment-length", 100);
        }
    }

    private void loadMigrationSettings() {
        ConfigurationSection migration = config.getConfigurationSection("migration");
        if (migration != null) {
            this.allowOverwrite = migration.getBoolean("allow-overwrite", false);
        }
    }

    private Material parseMaterial(String name) {
        if (name == null) return Material.STONE;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + name);
            return Material.STONE;
        }
    }

    // Getters

    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getTablePrefix() { return tablePrefix; }

    public Material getSeparatorItem() { return separatorItem; }
    public Material getRegisterDateItem() { return registerDateItem; }
    public Material getLikeButtonItem() { return likeButtonItem; }
    public Material getPrevPageItem() { return prevPageItem; }
    public Material getNextPageItem() { return nextPageItem; }
    public Material getCommentBaseItem() { return commentBaseItem; }
    public Map<Integer, Material> getCommentThresholds() { return commentThresholds; }
    public int getCommentsDisplayCount() { return commentsDisplayCount; }

    public int getLikeLimitPerDay() { return likeLimitPerDay; }
    public int getDefaultPages() { return defaultPages; }
    public int getMaxPages() { return maxPages; }
    public int getMaxCommentLength() { return maxCommentLength; }

    public boolean isAllowOverwrite() { return allowOverwrite; }
}
