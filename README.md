# RiaUserPages - RIA 个人主页插件

一个功能完善的 Minecraft Spigot 玩家个人主页展示插件，支持物品展示、留言系统和点赞功能。

## 功能特性

- **个人主页展示**：6行箱子GUI，支持多页展示物品
- **留言系统**：玩家可以在他人主页留言，支持删除管理
- **点赞系统**：每日限制点赞次数
- **多页支持**：管理员可解锁多页展示空间
- **本地化支持**：内置中文和英文，默认中文
- **MySQL 存储**：使用 HikariCP 连接池，支持高并发

## 环境要求

- Java 17+
- Spigot/Paper 1.20.1+
- MySQL 8.0+ (或兼容的数据库)

## 安装方法

1. 下载插件 JAR 文件，放入服务器的 `plugins` 文件夹
2. 启动服务器，生成默认配置文件
3. 编辑 `plugins/RiaUserPages/config.yml` 配置数据库连接
4. 重启服务器或使用 `/ria-player-profile admin reload` 重载配置

## 配置文件

### config.yml

```yaml
# 数据库配置
database:
  host: localhost
  port: 3306
  name: minecraft
  username: root
  password: your_password
  # MySQL 连接参数
  url-parameters: "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

# 插件设置
settings:
  # 命令相关配置
  command-prefix: "ria-player-profile"
  
  # 最大展示页数
  max-pages: 10
  
  # 最大留言长度
  max-comment-length: 100
  
  # 每日点赞限制
  daily-like-limit: 3

# GUI 物品配置 (可自定义材料类型)
gui-items:
  # 上一页按钮
  prev-page: LIME_DYE
  # 下一页按钮
  next-page: LIME_DYE
  # 分隔符
  separator: BLACK_STAINED_GLASS_PANE
  
  # 留言按钮 (根据留言数变化)
  comment-base: BOOK
  comment-thresholds:
    0: BOOK
    5: ENCHANTED_BOOK
    10: WRITABLE_BOOK
```

### 本地化文件

插件支持多语言，语言文件位于 `plugins/RiaUserPages/lang/` 目录：

- `zh_cn.yml` - 简体中文（默认）
- `en_us.yml` - English

修改 `config.yml` 中的 `language` 选项切换语言。

## 命令使用

### 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ria-player-profile` | `ria.profile.use` | 打开自己的个人主页 |
| `/ria-player-profile view <玩家>` | `ria.profile.use` | 查看指定玩家的主页 |
| `/ria-player-profile setbio <签名>` | `ria.profile.edit` | 设置个人签名 |
| `/ria-player-profile comment <玩家> <内容>` | `ria.profile.comment` | 给玩家留言 |

### 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ria-player-profile admin reload` | `ria.admin.*` | 重载配置 |
| `/ria-player-profile admin unlock <玩家>` | `ria.admin.*` | 为玩家解锁一页展示空间 |
| `/ria-player-profile admin lock <玩家>` | `ria.admin.*` | 减少玩家一页展示空间 |
| `/ria-player-profile admin clear <玩家> [页码]` | `ria.admin.*` | 清空玩家指定页的物品 |
| `/ria-player-profile admin resetlikes <玩家>` | `ria.admin.*` | 重置玩家点赞数 |
| `/ria-player-profile admin migrate <源玩家> <目标玩家>` | `ria.admin.*` | 迁移玩家数据 |

## 使用指南

### 查看个人主页

1. 输入 `/ria-player-profile` 打开自己的主页
2. 点击左侧玩家头像可切换到留言视图
3. 使用翻页按钮浏览多页内容

### 管理展示物品

1. 打开个人主页，确保在**物品展示视图**（非留言视图）
2. 在右侧展示区域（7x6格）放入物品
3. 物品会自动保存到数据库
4. **删除物品**：Shift+点击物品，关闭GUI后在聊天栏确认删除

### 留言操作

1. 打开目标玩家的个人主页
2. 点击左侧的留言按钮，或输入 `/ria-player-profile comment <玩家> <内容>`
3. 在留言视图中可查看所有留言
4. **删除留言**：留言所有者或主页主人可删除留言

### 点赞操作

1. 打开目标玩家的个人主页
2. 点击左侧的点赞按钮
3. 每日点赞次数有限制

## 权限节点

```yaml
ria.profile.use:       # 使用个人主页
ria.profile.edit:      # 编辑个人资料（签名、物品）
ria.profile.comment:   # 留言权限
ria.profile.comment.delete:  # 删除留言权限
ria.admin.*:           # 所有管理员权限
```

## 常见问题

### Q: 离线玩家查询显示"找不到该玩家"
A: 玩家必须至少登录过一次服务器，插件才能为其创建数据记录。

### Q: 如何修改最大页数？
A: 在 `config.yml` 中修改 `settings.max-pages`，然后使用 `/profile admin reload` 重载。

### Q: 支持哪些数据库？
A: 支持 MySQL 8.0+，理论上也支持 MariaDB 等兼容 MySQL 协议的数据库。

### Q: 如何备份数据？
A: 插件数据存储在 MySQL 中，请使用 MySQL 的备份工具进行备份。

## 技术信息

- **插件版本**: 1.0.0
- **作者**: ignis
- **开源协议**: MIT
