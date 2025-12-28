# Yet Another Bingo – Team Chest

A server-side Fabric mod that provides a shared team chest for
[Yet Another Bingo](https://modrinth.com/mod/yet-another-minecraft-bingo).

Players in the same Bingo team share a single inventory.

---

## Features

- One shared chest per Bingo team
- For clients and servers (optional for clients)
- Automatically resets when the Bingo game ends or resets
- Supports all Minecraft versions supported by Yet Another Bingo
- Compatible with all modern Fabric Loader versions

---

## Commands

| Command | Description |
|------|-----------|
| `/teamchest` | Open your team's shared chest |
| `/tc` | Alias for `/teamchest` |
| `/teamchest toggle` | Enable or disable team chests (OP only) |

---

## Requirements

- Minecraft **1.20.6 or newer**
- Fabric Loader **0.15.0 or newer**
- Fabric API
- Yet Another Bingo **2.9.0 or newer**
- Java **21**

---

## Installation (Server)

1. Install Fabric Server
2. Install Fabric API
3. Install Yet Another Bingo
4. Put `YetAnotherBingo-TeamChest.jar` into the `mods` folder
5. Start the server

Clients do **not** need to install this mod.

---

## Compatibility

This mod is compiled once and works across:

- Minecraft 1.20.6 → latest (tested up to 1.21.x)
- All Fabric Loader versions supported by Fabric
- Any server configuration supported by Yet Another Bingo

---

## License

MIT License
