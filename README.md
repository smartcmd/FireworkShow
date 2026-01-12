# 🎆 FireworkShow

> ✨ A firework show plugin for AllayMC servers

[![Allay API](https://img.shields.io/badge/Allay%20API-0.21.0-blue)](https://github.com/AllayMC/Allay)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

## 📖 Introduction

FireworkShow is a feature-rich firework display plugin that allows server administrators to automatically spawn fireworks at designated locations. Supports fully customizable firework effects, timed spawning, night-only mode, and more.

## ✨ Features

- 🎯 **Multi-location Support** - Configure fireworks at multiple locations across different worlds
- ⏰ **Timed Spawning** - Configurable spawn intervals (in game ticks)
- 🌙 **Night-only Mode** - Optionally spawn fireworks only during in-game night
- 🎨 **Fully Customizable** - Custom explosion types, colors, fade colors, twinkle and trail effects
- 🖥️ **In-game UI** - Convenient form interface to manage all settings
- 🌍 **Multi-language Support** - Supports Chinese (zh_CN) and English (en_US)
- 💾 **Persistent Storage** - Configuration automatically saved to YAML file

## 📋 Commands

| Command                                 | Description                           | Permission                          |
|-----------------------------------------|---------------------------------------|-------------------------------------|
| `/fireworkshow ui`                      | 🖥️ Open management UI (players only) | `fireworkshow.command.fireworkshow` |
| `/fireworkshow list`                    | 📋 List all firework positions        | `fireworkshow.command.fireworkshow` |
| `/fireworkshow add <world> <x> <y> <z>` | ➕ Add a new position                  | `fireworkshow.command.fireworkshow` |
| `/fireworkshow remove <world> <index>`  | ➖ Remove a position                   | `fireworkshow.command.fireworkshow` |
| `/fireworkshow toggle <world> <index>`  | 🔄 Toggle position enabled state      | `fireworkshow.command.fireworkshow` |

**Aliases:** `fwshow`, `fws`

## 🎨 Firework Types

| Type           | Description     |
|----------------|-----------------|
| `SMALL_SPHERE` | 🔵 Small sphere |
| `LARGE_SPHERE` | ⭕ Large sphere  |
| `STAR`         | ⭐ Star shape    |
| `CREEPER`      | 💀 Creeper face |
| `BURST`        | 💥 Burst        |

## 🎨 Available Colors

```
black, red, green, brown, blue, purple, cyan, light_gray,
gray, pink, lime, yellow, light_blue, magenta, orange, white
```

## ⚙️ Configuration Example

```yaml
configVersion: 1
positions:
  - worldName: world
    x: 100
    y: 64
    z: 200
    enabled: true
    nightOnly: false
    spawnTick: 40
    flightTimeMultiplier: 2
    explosions:
      - type: LARGE_SPHERE
        colors:
          - RED
          - YELLOW
        fade:
          - ORANGE
        twinkle: true
        trail: true
```

## 📦 Installation

1. Download the latest `FireworkShow-x.x.x-shaded.jar`
2. Place the JAR file in your server's `plugins` directory
3. Restart the server
4. Use `/fireworkshow ui` to start configuring

## 🛠️ Building

```bash
# Build the plugin
./gradlew shadowJar

# Run local test server
./gradlew runServer
```

Build output located at `build/libs/FireworkShow-x.x.x-shaded.jar`

## 📁 Project Structure

```
src/main/java/me/daoge/fireworkshow/
├── 🎆 FireworkShow.java              # Main plugin class
├── 📍 FireworkPosition.java          # Position data class
├── 🔧 FireworkUtils.java             # Utility methods
├── 💬 FireworkShowCommand.java       # Command handler
├── 👁️ FireworkShowEventListener.java # Event listener
├── 🖥️ FireworkShowUI.java            # Form UI
└── 🔑 TrKeys.java                    # Translation key constants

src/main/resources/assets/lang/
├── 🇺🇸 en_US.json                    # English translations
└── 🇨🇳 zh_CN.json                    # Chinese translations
```

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  🎆 Made with ❤️ for AllayMC 🎆
</p>
