# Yet Another Bingo Team Chest

[![Fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/supported/fabric_vector.svg)](https://fabricmc.net/)
[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)

[![Available on Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/yab-teamchest)
[![Available on CurseForge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/yet-another-bingo-team-chest)
[![Open Source on GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@2/assets/cozy/available/github_vector.svg)](https://github.com/ImCZFy/YetAnotherBingo-TeamChest)

English | [简体中文](README_zh-CN.md)

A Fabric mod for [Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo) that adds shared team chests, team teleport, and optional Bingo item-scoring integration.

This branch targets Minecraft 26.1 and 26.2. The 26.1 module uses Yet Another Bingo API 2.10.0, and the 26.2 module uses Yet Another Bingo API 2.11.0.

## Features

- One persistent shared chest per Bingo team.
- Team chest items can optionally count toward Bingo item objectives.
- Supports Bingo consume-items mode through the shared team chest inventory.
- Team teleport command for players in the same Bingo team.
- Server-side configuration GUI opened by command.
- Client keybind for opening the team chest when installed on the client.
- Team chests are cleared when the Bingo game resets.

## Commands

| Command | Description |
| --- | --- |
| `/teamchest` | Open your team's shared chest |
| `/tc` | Alias for `/teamchest` |
| `/teamchest toggle` | Enable or disable team chests, OP only |
| `/tc toggle` | Alias for `/teamchest toggle` |
| `/teamchest config` | Open the config GUI, OP only |
| `/tc config` | Alias for `/teamchest config` |
| `/teamtp <player>` | Teleport to a player in your team |
| `/ttp <player>` | Alias for `/teamtp` |
| `/tptoggle` | Enable or disable team teleport, OP only |

Configuration changes are only allowed before the Bingo game starts. Once Bingo leaves pregame, toggle commands and config GUI buttons will refuse to modify settings.

## Configuration

The config file is created at:

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

`count_team_chest_items` controls whether items in the team chest are exposed to Yet Another Bingo as an extra player inventory. When enabled, items stored in the team chest can satisfy Bingo item objectives. If Bingo consume-items mode is enabled, matching items may also be consumed from the team chest.

## Requirements

- Minecraft 26.1 with Yet Another Bingo API 2.10.0
- Minecraft 26.2 with Yet Another Bingo API 2.11.0
- Java 25
- Fabric Loader 0.15.0 or newer for 26.1, 0.19.3 or newer for 26.2
- Fabric API

## Installation

1. Install Fabric Server.
2. Install Fabric API.
3. Install Yet Another Bingo.
4. Put the Team Chest jar into the `mods` folder.
5. Start the server.

Clients may also install this mod for the keybind and localization.

## Building

On Windows, this repository path may contain `&`, so use the fixed Gradle wrapper:

```powershell
.\gradlew.bat :mc26.1:build :mc26.2:build
```

The jar is written to:

```text
build/libs/
```

## License

MIT License
