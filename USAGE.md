# RiaUserPages 命令使用手册

## 基础命令格式

所有命令都以 `/ria-player-profile` 开头。

**命令别名**:
- `/riaprofile` - 短格式别名
- `/rpp` - 极简别名

示例:
```
/ria-player-profile view Riako_1
/riaprofile view Riako_1
/rpp view Riako_1
```
以上三个命令效果相同。

---

## 玩家命令

### 打开个人主页

```
/ria-player-profile view
```

打开自己的个人主页展示界面。

**权限**: `ria.profile.use`

---

### 查看他人主页

```
/ria-player-profile view <玩家名>
```

查看指定玩家的个人主页。

**参数**:
- `<玩家名>` - 目标玩家的用户名

**权限**: `ria.profile.use`

**示例**:
```
/ria-player-profile view Riako_1
```

---

### 设置个人签名

```
/ria-player-profile setbio <签名内容>
```

设置显示在个人主页上的签名/简介。

**参数**:
- `<签名内容>` - 你的个人签名，支持空格，最多100字符

**权限**: `ria.profile.edit`

**示例**:
```
/ria-player-profile setbio 这是一个测试签名
/ria-player-profile setbio 喜欢建筑和探险！
```

---

### 给玩家留言

```
/ria-player-profile comment <玩家名> <留言内容>
```

在指定玩家的主页留言。

**参数**:
- `<玩家名>` - 目标玩家的用户名
- `<留言内容>` - 留言内容，支持空格

**权限**: `ria.profile.comment`

**示例**:
```
/ria-player-profile comment Riako_1 你的建筑真棒！
```

---

### 删除留言（命令方式）

```
/ria-player-profile comment delete <留言ID>
```

通过留言ID直接删除指定留言。

**参数**:
- `<留言ID>` - 要删除的留言ID（可在留言视图中查看）

**权限**: `ria.profile.comment.delete`（删除自己的留言）或 `ria.admin.*`（删除任意留言）

**示例**:
```
/ria-player-profile comment delete 123
```

**说明**: 也可以通过GUI方式删除留言（见下文"删除确认流程"）

---

## 管理员命令

管理员命令都需要权限 `ria.admin.*`

### 重载配置

```
/ria-player-profile admin reload
```

重新加载插件的配置文件和语言文件。

---

### 解锁展示页

```
/ria-player-profile admin unlock <玩家名>
```

为指定玩家解锁一页展示空间（每次执行+1页）。

**参数**:
- `<玩家名>` - 目标玩家的用户名

**示例**:
```
/ria-player-profile admin unlock Riako_1
```

**说明**: 解锁后玩家可以在新的页面放置展示物品。

---

### 锁定展示页

```
/ria-player-profile admin lock <玩家名>
```

减少指定玩家的展示页数（每次执行-1页），最少保留1页。

**参数**:
- `<玩家名>` - 目标玩家的用户名

**示例**:
```
/ria-player-profile admin lock Riako_1
```

**说明**: 
- 减少页数不会删除物品，只是限制访问
- 如果玩家有多页物品，减少后超出页数的物品会被隐藏，再次解锁后会恢复显示

---

### 清空展示页

```
/ria-player-profile admin clear <玩家名> [页码]
```

清空指定玩家某页的所有展示物品。

**参数**:
- `<玩家名>` - 目标玩家的用户名
- `[页码]` - 可选，要清空的页码，默认为第1页

**示例**:
```
/ria-player-profile admin clear Riako_1      # 清空第1页
/ria-player-profile admin clear Riako_1 2    # 清空第2页
```

**注意**: 此操作不可恢复，物品将被永久删除！

---

### 重置点赞数

```
/ria-player-profile admin resetlikes <玩家名>
```

重置指定玩家的总点赞数为0。

**参数**:
- `<玩家名>` - 目标玩家的用户名

**示例**:
```
/ria-player-profile admin resetlikes Riako_1
```

---

### 迁移玩家数据

```
/ria-player-profile admin migrate <源玩家> <目标玩家>
```

将源玩家的所有数据（展示物品、留言、点赞等）迁移到目标玩家。

**参数**:
- `<源玩家>` - 数据来源玩家
- `<目标玩家>` - 数据目标玩家

**示例**:
```
/ria-player-profile admin migrate OldName NewName
```

**注意**: 
- 默认情况下，如果目标玩家已有数据，迁移会失败
- 如需覆盖，需在配置中设置 `allow-overwrite: true`

---

## GUI 操作说明

### 个人主页界面

```
[头像] [日期] [留言] [点赞] [上一页] [下一页]
|                    展示区域 (7x6)                 |
```

**左侧功能区**:
- **玩家头像** - 点击切换到留言视图
- **注册日期** - 显示玩家首次加入时间
- **留言按钮** - 点击打开留言界面或添加留言
- **点赞按钮** - 点击给该玩家点赞
- **翻页按钮** - 浏览多页内容

**右侧展示区域**:
- 7列 x 6行 的物品展示空间
- 放入物品自动保存
- 点击物品触发删除确认

### 留言视图界面

显示所有留言的列表，每条留言显示：
- 留言者名称
- 留言时间
- 留言内容
- 删除提示（仅主页主人和管理员可见）

---

## 删除确认流程

### 删除展示物品

1. 打开个人主页
2. **点击**要删除的物品
3. 关闭 GUI
4. 在聊天栏点击"[确认]"或"[取消]"
5. 30秒内未操作自动取消

### 删除留言

1. 打开目标玩家的留言视图
2. 点击要删除的留言
3. 关闭 GUI
4. 在聊天栏点击"[确认]"或"[取消]"

---

## 权限汇总

| 权限节点 | 说明 |
|---------|------|
| `ria.profile.use` | 使用个人主页命令 |
| `ria.profile.edit` | 编辑个人资料（签名、物品） |
| `ria.profile.comment` | 留言权限 |
| `ria.profile.comment.delete` | 删除留言权限 |
| `ria.admin.*` | 所有管理员权限 |

---

## 快捷操作

| 操作     | 说明                               |
|--------|----------------------------------|
| Tab 补全 | 所有命令都支持 Tab 键自动补全玩家名和子命令         |
