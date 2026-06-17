# Yet Another Bingo Team Chest

[![Fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/supported/fabric_vector.svg)](https://fabricmc.net/)
[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/yab-teamchest)
[![Available on CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/yet-another-bingo-team-chest)
[![Open Source on GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/github_vector.svg)](https://github.com/ImCZFy/YetAnotherBingo-TeamChest)

[English](README.md) | 简体中文

这是一个用于 [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo) 的 Fabric 模组，提供队伍共享箱、队伍传送，以及可选的 Bingo 物品判定集成。

当前分支面向 Minecraft 26.1 和 26.2。26.1 模块使用 Yet Another Bingo API 2.10.0，26.2 模块使用 Yet Another Bingo API 2.11.0。

## 功能

- 每个 Bingo 队伍拥有一个持久化共享队伍箱。
- 队伍箱物品可以选择参与 Bingo 物品目标判定。
- 支持 Bingo 的消耗物品模式，队伍箱中的匹配物品也可能被消耗。
- 同队玩家之间可以使用队伍传送。
- 可通过指令打开服务端配置 GUI。
- 客户端安装后可使用快捷键打开队伍箱。
- Bingo 游戏重置时会清空所有队伍箱。

## 指令

| 指令 | 说明 |
| --- | --- |
| `/teamchest` | 打开你所在队伍的共享箱 |
| `/tc` | `/teamchest` 的别名 |
| `/teamchest toggle` | 启用或禁用队伍箱，仅 OP |
| `/tc toggle` | `/teamchest toggle` 的别名 |
| `/teamchest config` | 打开配置 GUI，仅 OP |
| `/tc config` | `/teamchest config` 的别名 |
| `/teamtp <玩家>` | 传送到同队玩家 |
| `/ttp <玩家>` | `/teamtp` 的别名 |
| `/tptoggle` | 启用或禁用队伍传送，仅 OP |

所有配置修改只能在 Bingo 游戏开始前进行。Bingo 离开准备阶段后，切换指令和配置 GUI 按钮都会拒绝修改配置。

## 配置

配置文件会生成在：

```text
config/yetanotherbingo-teamchest.toml
```

```toml
[team_chest]
rows = 3
enabled = true

[team_teleport]
enabled = true

[bingo]
count_team_chest_items = true
```

`count_team_chest_items` 控制队伍箱物品是否作为额外玩家库存暴露给 Yet Another Bingo。启用后，队伍箱中的物品可以完成 Bingo 物品目标。如果 Bingo 开启了消耗物品模式，匹配物品也可能从队伍箱中被消耗。

## 需求

- Minecraft 26.1 与 Yet Another Bingo API 2.10.0
- Minecraft 26.2 与 Yet Another Bingo API 2.11.0
- Java 25
- 26.1 使用 Fabric Loader 0.15.0 或更新版本，26.2 使用 Fabric Loader 0.19.3 或更新版本
- Fabric API

## 安装

1. 安装 Fabric Server。
2. 安装 Fabric API。
3. 安装 Yet Another Bingo。
4. 将 Team Chest jar 放入 `mods` 文件夹。
5. 启动服务器。

客户端也可以安装此模组，以使用快捷键和本地化。

## 构建

Windows 下仓库路径可能包含 `&`，请使用已经修复的 Gradle wrapper：

```powershell
.\gradlew.bat :mc26.1:build :mc26.2:build
```

构建产物会输出到：

```text
build/libs/
```

## 许可证

MIT License
