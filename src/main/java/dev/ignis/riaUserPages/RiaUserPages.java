package dev.ignis.riaUserPages;

import dev.ignis.riaUserPages.command.ProfileCommand;
import dev.ignis.riaUserPages.config.ConfigManager;
import dev.ignis.riaUserPages.database.*;
import dev.ignis.riaUserPages.gui.GUIListener;
import dev.ignis.riaUserPages.gui.ProfileGUI;
import dev.ignis.riaUserPages.localization.LocalizationManager;
import dev.ignis.riaUserPages.serialization.BukkitItemSerializer;
import dev.ignis.riaUserPages.serialization.ItemSerializer;
import dev.ignis.riaUserPages.service.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for RIA Player Profile.
 */
public final class RiaUserPages extends JavaPlugin {

    private static RiaUserPages instance;

    private ConfigManager configManager;
    private LocalizationManager localizationManager;
    private DatabaseManager databaseManager;
    private ItemSerializer itemSerializer;

    // Services
    private ProfileService profileService;
    private LikeService likeService;
    private CommentService commentService;
    private MigrationService migrationService;
    private PendingActionManager pendingActionManager;

    // GUI
    private ProfileGUI profileGUI;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.load();

        // Initialize localization
        localizationManager = new LocalizationManager(this);
        localizationManager.load();

        // Initialize item serializer
        itemSerializer = new BukkitItemSerializer();

        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize services
        profileService = new ProfileService(this);
        likeService = new LikeService(this);
        commentService = new CommentService(this);
        migrationService = new MigrationService(this);
        pendingActionManager = new PendingActionManager(this);

        // Initialize GUI
        profileGUI = new ProfileGUI(this);

        // Register commands
        getCommand("ria-player-profile").setExecutor(new ProfileCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("RIA Player Profile has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("RIA Player Profile has been disabled!");
    }

    public static RiaUserPages getInstance() {
        return instance;
    }

    // Getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LocalizationManager getLocalizationManager() {
        return localizationManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ItemSerializer getItemSerializer() {
        return itemSerializer;
    }

    public ProfileService getProfileService() {
        return profileService;
    }

    public LikeService getLikeService() {
        return likeService;
    }

    public CommentService getCommentService() {
        return commentService;
    }

    public MigrationService getMigrationService() {
        return migrationService;
    }

    public ProfileGUI getProfileGUI() {
        return profileGUI;
    }

    public PendingActionManager getPendingActionManager() {
        return pendingActionManager;
    }
}
