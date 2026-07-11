# 🎮 StatTweaks - Minecraft Stat Configuration Mod

> **Total control over your game stats.** Dynamically modify attributes of items and entities from both Vanilla and other Mods.

**StatTweaks** is a robust mod for Minecraft 1.21.1+ (available for **Fabric** and **NeoForge**) that allows you to adjust almost any game parameter through a single JSON configuration file. From sword damage to boss health or apple nutrition.

Ideal for modpack creators looking for perfect balance without writing code.

---

## ✨ Key Features

- ✅ **Item Adjustments**: Modify damage, attack speed, durability, efficiency, stack size and more.
- ✅ **Entity Adjustments**: Change max health, speed, attack damage and detection range of any mob.
- ✅ **Data Components**: Full support for 1.20.5+ *Data Components* (rarity, food, fire resistance, etc.).
- ✅ **Tag Support**: Apply changes to entire groups of items or entities using tags (e.g., `#minecraft:swords` or `#minecraft:skeletons`).
- ✅ **Hot-Reload**: Use `/stattweaks reload` to apply changes instantly without restarting the game.
- ✅ **Base Display Mode**: Option to see exact final values in tooltips instead of Minecraft's relative bonuses.

---

## ⚙️ Configuration

The configuration file is located at `config/CPT_StatTweaks_Config.json`.

### General Structure
```json
{
  "tooltip_mode": "relative",
  "items": { ... },
  "entities": { ... }
}
```

### 1. Item Adjustments
You can modify attributes and components. The mod prioritizes specific IDs over tags.

#### Special Attributes:
- `stattweaks:durability`: Maximum durability.
- `stattweaks:efficiency`: Mining speed for tools.
- `stattweaks:stack_size`: Maximum stack size (1-99).

#### Item Example:
```json
"minecraft:diamond_axe": {
  "attributes": {
    "minecraft:generic.attack_damage": 15.0,
    "stattweaks:durability": 3000,
    "stattweaks:efficiency": 25.0
  },
  "components": {
    "minecraft:rarity": "rare",
    "minecraft:fire_resistant": {}
  }
}
```

### 2. Entity Adjustments
Modify the base attributes of any living creature.

#### Entity Example:
```json
"minecraft:zombie": {
  "attributes": {
    "minecraft:generic.max_health": 40.0,
    "minecraft:generic.movement_speed": 0.35,
    "minecraft:generic.attack_damage": 8.0
  }
}
```

### 3. Mod Compatibility 🧩
StatTweaks is fully compatible with any mod that uses the standard Minecraft registry system. You can modify items, entities, and attributes from any other mod by using their full **Resource Location** (`modid:name`).

#### Modded Example:
```json
{
  "items": {
    "farmersdelight:iron_knife": {
      "attributes": {
        "minecraft:generic.attack_damage": 6.0,
        "stattweaks:durability": 1200
      }
    }
  },
  "entities": {
    "alexsmobs:grizzly_bear": {
      "attributes": {
        "minecraft:generic.max_health": 80.0,
        "minecraft:generic.attack_damage": 12.0
      }
    }
  }
}
```

> **Note:** For other mods to be compatible, they must use the standard Minecraft Attribute and Data Component systems. Custom attributes from mods (e.g., `modid:custom_attribute`) are also supported.

---

## 📊 Tooltip Modes

You can change how stats look in your inventory with the `"tooltip_mode"` option:

- **`"relative"` (Default)**: Shows bonuses relative to an empty hand (e.g., `+9 Damage`).
- **`"base"`**: Shows the absolute final value (e.g., `10 Damage`). It is much more intuitive to know the real power of an item.

---

## ⌨️ Commands

- `/stattweaks reload`: Reloads the JSON file, applies changes on the server, and synchronizes them with all connected clients. (Requires permission level 2/OP).
  - **Note for 1.21.9:** This command does not work correctly in this version. You must rejoin the world to apply changes.

---

## 🛠️ Installation and Requirements

- **Version**: Minecraft 1.21.1
- **Mod Loader**: Fabric (requires Fabric API) or NeoForge.
- **Multiplayer**: Must be installed on both server and clients for correct stat and tooltip synchronization.

---

## 📝 Additional Notes

- When using tags (`#`), values are applied additively.
- Tools modified with `efficiency` automatically update their mining rules for the corresponding blocks.
- If an item has already been created, some changes (like max durability) will apply visually, but the item's current damage might remain proportional.

> **⚠️ Important for Minecraft 1.21.9:** The reload command does not work correctly in this version. You must exit and re-enter the world to apply any changes made to the JSON configuration.
