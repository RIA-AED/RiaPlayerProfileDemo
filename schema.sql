-- ============================================
-- RIA Player Profile - Database Schema
-- ============================================
-- 表名前缀可配置，默认为 ria_
-- 此脚本仅在需要手动建表时使用
-- 插件会自动创建这些表
-- ============================================

-- 用户档案表
CREATE TABLE IF NOT EXISTS ria_profiles (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(16) NOT NULL,
    bio TEXT,
    joined_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_likes INT DEFAULT 0,
    unlocked_pages INT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 物品存储表
CREATE TABLE IF NOT EXISTS ria_inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_uuid VARCHAR(36) NOT NULL,
    page_number INT NOT NULL,
    slot_index INT NOT NULL,
    item_nbt LONGTEXT NOT NULL,
    UNIQUE KEY unique_inventory_slot (owner_uuid, page_number, slot_index),
    FOREIGN KEY (owner_uuid) REFERENCES ria_profiles(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 点赞记录表
CREATE TABLE IF NOT EXISTS ria_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    liker_uuid VARCHAR(36) NOT NULL,
    target_uuid VARCHAR(36) NOT NULL,
    like_date DATE NOT NULL,
    UNIQUE KEY unique_like (liker_uuid, target_uuid, like_date),
    FOREIGN KEY (target_uuid) REFERENCES ria_profiles(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 留言表
CREATE TABLE IF NOT EXISTS ria_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_uuid VARCHAR(36) NOT NULL,
    author_name VARCHAR(16) NOT NULL,
    target_uuid VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (target_uuid) REFERENCES ria_profiles(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
