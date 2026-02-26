package dev.ignis.riaUserPages.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.ignis.riaUserPages.RiaUserPages;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database connections using HikariCP connection pool.
 */
public class DatabaseManager {

    private final RiaUserPages plugin;
    private HikariDataSource dataSource;
    private String tablePrefix;

    // DAOs
    private ProfileDAO profileDAO;
    private InventoryDAO inventoryDAO;
    private LikeDAO likeDAO;
    private CommentDAO commentDAO;

    public DatabaseManager(RiaUserPages plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the database connection pool and create tables.
     */
    public void initialize() {
        setupConnectionPool();
        createTables();
        initializeDAOs();
    }

    private void initializeDAOs() {
        this.profileDAO = new ProfileDAO(this);
        this.inventoryDAO = new InventoryDAO(this, plugin.getItemSerializer());
        this.likeDAO = new LikeDAO(this);
        this.commentDAO = new CommentDAO(this);
    }

    public ProfileDAO getProfileDAO() {
        return profileDAO;
    }

    public InventoryDAO getInventoryDAO() {
        return inventoryDAO;
    }

    public LikeDAO getLikeDAO() {
        return likeDAO;
    }

    public CommentDAO getCommentDAO() {
        return commentDAO;
    }

    /**
     * Close the connection pool.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Get a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Get the configured table prefix.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    private void setupConnectionPool() {
        ConfigurationSection dbConfig = plugin.getConfig().getConfigurationSection("database");
        if (dbConfig == null) {
            throw new IllegalStateException("Database configuration section is missing");
        }

        this.tablePrefix = dbConfig.getString("table-prefix", "ria_");

        HikariConfig config = new HikariConfig();
        
        String host = dbConfig.getString("host", "localhost");
        int port = dbConfig.getInt("port", 3306);
        String database = dbConfig.getString("database", "ria_db");
        String user = dbConfig.getString("user", "root");
        String password = dbConfig.getString("password", "");

        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", 
                host, port, database));
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Pool configuration
        ConfigurationSection poolConfig = dbConfig.getConfigurationSection("pool");
        if (poolConfig != null) {
            config.setMaximumPoolSize(poolConfig.getInt("maximum-pool-size", 10));
            config.setMinimumIdle(poolConfig.getInt("minimum-idle", 5));
            config.setConnectionTimeout(poolConfig.getLong("connection-timeout", 30000));
            config.setIdleTimeout(poolConfig.getLong("idle-timeout", 600000));
            config.setMaxLifetime(poolConfig.getLong("max-lifetime", 1800000));
        }

        config.setPoolName("RIA-Profile-Pool");

        this.dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Database connection pool initialized successfully");
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Profiles table
            stmt.executeUpdate(String.format(
                "CREATE TABLE IF NOT EXISTS %sprofiles (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16) NOT NULL," +
                "bio TEXT," +
                "joined_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "total_likes INT DEFAULT 0," +
                "unlocked_pages INT DEFAULT 1" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                tablePrefix
            ));

            // Inventories table
            stmt.executeUpdate(String.format(
                "CREATE TABLE IF NOT EXISTS %sinventories (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "page_number INT NOT NULL," +
                "slot_index INT NOT NULL," +
                "item_nbt LONGTEXT NOT NULL," +
                "UNIQUE KEY unique_inventory_slot (owner_uuid, page_number, slot_index)," +
                "FOREIGN KEY (owner_uuid) REFERENCES %sprofiles(uuid) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                tablePrefix, tablePrefix
            ));

            // Likes table
            stmt.executeUpdate(String.format(
                "CREATE TABLE IF NOT EXISTS %slikes (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "liker_uuid VARCHAR(36) NOT NULL," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "like_date DATE NOT NULL," +
                "UNIQUE KEY unique_like (liker_uuid, target_uuid, like_date)," +
                "FOREIGN KEY (target_uuid) REFERENCES %sprofiles(uuid) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                tablePrefix, tablePrefix
            ));

            // Comments table
            stmt.executeUpdate(String.format(
                "CREATE TABLE IF NOT EXISTS %scomments (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "author_uuid VARCHAR(36) NOT NULL," +
                "author_name VARCHAR(16) NOT NULL," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "content TEXT NOT NULL," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (target_uuid) REFERENCES %sprofiles(uuid) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                tablePrefix, tablePrefix
            ));

            plugin.getLogger().info("Database tables created successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
