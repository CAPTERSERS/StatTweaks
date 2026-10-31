# StatTweaks — Minecraft 1.21.9

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.9-6F4E37?labelColor=5A8F32)](https://www.minecraft.net/)
[![Architectury](https://img.shields.io/badge/Architectury-18.0.5-000000?labelColor=F05020)](https://github.com/architectury/architectury-api)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-0.134.0%2B1.21.9-303020?labelColor=C8C4A0)](https://fabricmc.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.9.9--beta-222427?labelColor=D7742F)](https://neoforged.net/)

This README is the source for Minecraft 1.21.9. The project summary is on [`main`](https://github.com/CAPTERSERS/StatTweaks/tree/main). The changelog for this branch is [`CHANGELOG.md`](CHANGELOG.md).

StatTweaks is a Fabric and NeoForge mod for adjusting item and entity attributes with a JSON file, without leaving the vanilla registry.

## Requirements

From `gradle.properties` on this branch:

- Minecraft 1.21.9
- Architectury API 18.0.5
- Fabric API 0.134.0+1.21.9
- NeoForge 21.9.9-beta

`build.gradle` compiles with Java 21. Multiplayer needs the mod on the server and on every client so stats and tooltips stay in sync.

## Build

This branch has `settings.gradle`, `build.gradle`, and `gradle/wrapper/gradle-wrapper.properties`. That properties file pins Gradle 8.12.1. There is no `gradlew` or `gradlew.bat`.

```bash
git clone -b 1.21.9 https://github.com/CAPTERSERS/StatTweaks.git
cd StatTweaks
```

With Gradle 8.12.1 on your PATH:

```bash
gradle build
```

Jars are written to `fabric/build/libs` and `neoforge/build/libs`.

## Configuration

The configuration file is `config/CPT_StatTweaks_Config.json`.

### General structure

```json
{
  "tooltip_mode": "relative",
  "items": { ... },
  "entities": { ... }
}
```

### 1. Item adjustments

You can modify attributes and components. The mod prioritizes specific IDs over tags.

#### Special attributes

- `stattweaks:durability`: maximum durability.
- `stattweaks:efficiency`: mining speed for tools.
- `stattweaks:stack_size`: maximum stack size (1-99).

#### Item example

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

### 2. Entity adjustments

Modify the base attributes of any living creature.

#### Entity example

```json
"minecraft:zombie": {
  "attributes": {
    "minecraft:generic.max_health": 40.0,
    "minecraft:generic.movement_speed": 0.35,
    "minecraft:generic.attack_damage": 8.0
  }
}
```

### 3. Mod compatibility

StatTweaks works with any mod that uses the standard Minecraft registry. Refer to items, entities, and attributes by their full resource location (`modid:name`).

#### Modded example

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

Other mods need to use the standard Minecraft attribute and data component systems. Custom attributes (`modid:custom_attribute`) are supported.

## Tooltip modes

`tooltip_mode` controls how stats appear in the inventory:

- `"relative"` (default): bonuses relative to an empty hand, for example `+9 Damage`.
- `"base"`: the absolute final value, for example `10 Damage`.

## Commands

- `/stattweaks reload` does not apply changes on Minecraft 1.21.9. Leave the world and rejoin to apply the JSON configuration.

## Additional notes

- Tag values (`#`) are applied additively.
- Tools modified with `efficiency` update their mining rules for the corresponding blocks.
- If an item already exists, some changes (such as max durability) apply visually, while the item's current damage can stay proportional.
- On Minecraft 1.21.9 the reload command does not work. Rejoin the world to apply JSON changes.

## License

Code: MIT. See [`LICENSE`](LICENSE).
