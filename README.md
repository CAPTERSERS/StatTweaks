# StatTweaks — Minecraft 1.21.1

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-6F4E37?labelColor=5A8F32)](https://www.minecraft.net/)
[![Architectury](https://img.shields.io/badge/Architectury-13.0.8-000000?labelColor=F05020)](https://github.com/architectury/architectury-api)
[![Fabric API](https://img.shields.io/badge/Fabric%20API-0.116.7%2B1.21.1-303020?labelColor=C8C4A0)](https://fabricmc.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.215-222427?labelColor=D7742F)](https://neoforged.net/)

This README is the source for Minecraft 1.21.1. The project summary is on [`main`](https://github.com/CAPTERSERS/StatTweaks/tree/main). The changelog for this branch is [`CHANGELOG.md`](CHANGELOG.md).

StatTweaks is a Fabric and NeoForge mod for adjusting item and entity attributes with a JSON file, without leaving the vanilla registry.

## Requirements

From `gradle.properties` on this branch:

- Minecraft 1.21.1
- Architectury API 13.0.8
- Fabric API 0.116.7+1.21.1
- NeoForge 21.1.215

`build.gradle` compiles with Java 21. Multiplayer needs the mod on the server and on every client so stats and tooltips stay in sync.

## Build

This branch has `settings.gradle`, `build.gradle`, and `gradle/wrapper/gradle-wrapper.properties`. That properties file pins Gradle 8.12.1. There is no `gradlew` or `gradlew.bat`.

```bash
git clone -b 1.21.1 https://github.com/CAPTERSERS/StatTweaks.git
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

#### Special attributes (for the `attributes` section)

- `stattweaks:durability`: maximum durability of the item.
- `stattweaks:efficiency`: mining speed for tools. This updates tool mining rules.

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
    "minecraft:fire_resistant": {},
    "minecraft:max_stack_size": 32
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

### 3. Available attributes (items and entities)

| Attribute ID | What it does | Example value |
|---|---|---|
| `minecraft:generic.max_health` | Maximum health points | `40.0` |
| `minecraft:generic.movement_speed` | Walking/swimming speed | `0.35` |
| `minecraft:generic.attack_damage` | Melee damage dealt | `8.0` |
| `minecraft:generic.attack_speed` | Attack cooldown (attacks per second) | `1.8` |
| `minecraft:generic.armor` | Armor defense points | `10.0` |
| `minecraft:generic.armor_toughness` | Armor toughness (reduces damage from strong hits) | `2.0` |
| `minecraft:generic.knockback_resistance` | Resistance to knockback | `0.5` |
| `minecraft:generic.flying_speed` | Flight speed (for flying mobs/creative mode) | `0.8` |
| `minecraft:generic.follow_range` | Detection range for mobs | `32.0` |
| `minecraft:generic.luck` | Luck bonus (loot tables) | `5.0` |
| `minecraft:generic.step_height` | Step height (how high the entity can climb) | `1.5` |
| `minecraft:generic.water_movement_efficiency` | Water movement speed multiplier | `0.8` |
| `minecraft:generic.burning_time_scale` | Fire damage frequency modifier | `1.2` |
| `minecraft:generic.explosion_knockback_resistance` | Reduces explosion knockback | `0.6` |
| `minecraft:generic.temperature_modifier` | Temperature modifier for cold/heat effects | `2.0` |
| `minecraft:horse.jump_strength` | Jump strength for horses | `1.5` |

You can also use attributes from other mods by their resource location (for example, `modid:custom_attribute`).

### 4. Available data components (items)

| Component ID | What it does | Example value |
|---|---|---|
| `minecraft:max_stack_size` | Maximum stack size (1-64) | `32` |
| `minecraft:max_damage` | Maximum durability | `1561` |
| `minecraft:damage` | Current damage/durability left | `0` |
| `minecraft:rarity` | Item rarity/color | `"rare"` (common, uncommon, rare, epic) |
| `minecraft:unbreakable` | Unbreakable flag (no durability loss) | `{}` |
| `minecraft:enchantments` | Item enchantments | `{"levels": {"minecraft:sharpness": 5}}` |
| `minecraft:stored_enchantments` | Enchantments for enchanted books | `{"levels": {"minecraft:sharpness": 5}}` |
| `minecraft:custom_name` | Custom display name | `{"text": "Legendary Sword"}` |
| `minecraft:lore` | Item lore/description lines | `[{"text": "Line 1"}, {"text": "Line 2"}]` |
| `minecraft:food` | Food properties | `{"nutrition": 8, "saturation_modifier": 0.6}` |
| `minecraft:consumable` | Consumable properties (animation, cooldown) | `{"consume_seconds": 1.6, "animation": "drink"}` |
| `minecraft:use_cooldown` | Cooldown between uses | `{"seconds": 5}` |
| `minecraft:enchantable` | Maximum enchantment level | `{"value": 1}` |
| `minecraft:repairable` | Items to repair with | `{"items": ["minecraft:iron_ingot"]}` |
| `minecraft:dyed_color` | RGB dye color (for armor/leather) | `{"rgb": 16711680, "show_in_tooltip": true}` |
| `minecraft:equippable` | Equippable slot and effects | `{"slot": "head", "camera_overlay": ""}` |
| `minecraft:glider` | Glider/elytra properties | `{}` |
| `minecraft:tool` | Tool mining rules and efficiency | `{"default_mining_speed": 1.0, "damage_per_block": 1}` |
| `minecraft:weapon` | Weapon damage modifiers | `{"damage": 7.0}` |
| `minecraft:can_break` | Can break blocks despite creative/adventure mode | `{"predicates": ["minecraft:dirt"]}` |
| `minecraft:can_place_on` | Can place on blocks | `{"predicates": ["minecraft:stone"]}` |
| `minecraft:tooltip_display` | Custom tooltip display settings | `{}` |
| `minecraft:trim` | Armor trim (pattern + material) | `{"material": "minecraft:iron", "pattern": "minecraft:coast"}` |
| `minecraft:bundle_contents` | Bundle inventory contents | `[{...}, {...}]` |
| `minecraft:potion_contents` | Potion effects and color | `{"custom_color": 16711680}` |
| `minecraft:suspicious_stew_effects` | Suspicious stew effects | `[{"effect": "minecraft:poison", "duration": 100}]` |
| `minecraft:lock` | Lock state for containers | `{"key": ""}` |
| `minecraft:fireworks` | Firework burst properties | `{"flight_duration": 1}` |
| `minecraft:instrument` | Goat horn instrument type | `{"sound_event": "minecraft:item.goat_horn.sound.0"}` |
| `minecraft:recipes` | Unlocked recipes (knowledge book) | `["minecraft:crafting_table"]` |
| `minecraft:lodestone_tracker` | Lodestone position tracker | `{}` |
| `minecraft:creative_slot_lock` | Creative mode slot lock flag | `{}` |
| `minecraft:intangible_projectile` | Projectile intangibility flag | `{}` |
| `minecraft:item_model` | Custom item model override | `"minecraft:custom_models/special"` |
| `minecraft:item_name` | Item name component override | `{"text": "Name"}` |
| `minecraft:block_entity_data` | Block entity NBT data | `{...}` |
| `minecraft:custom_data` | Custom NBT data storage | `{...}` |
| `minecraft:entity_data` | Entity NBT data (for example, egg variants) | `{...}` |
| `minecraft:repair_cost` | Repair cost in an anvil | `5` |
| `minecraft:damage_resistant` | Damage resistance flag | `{}` |
| `minecraft:death_protection` | Protection on death | `{}` |
| `minecraft:tooltip_style` | Custom tooltip style | `"minecraft:style"` |

You can also use data components from other mods. Use the `minecraft:` namespace for vanilla components or `modid:` for modded ones. Stack size uses the `minecraft:max_stack_size` component.

### 5. Mod compatibility

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

- `/stattweaks reload`: reloads the JSON file, applies changes on the server, and synchronizes them with connected clients. Requires permission level 2.

## Additional notes

- Tag values (`#`) are applied additively.
- Tools modified with `efficiency` update their mining rules for the corresponding blocks.
- If an item already exists, some changes (such as max durability) apply visually, while the item's current damage can stay proportional.

## License

Code: MIT. See [`LICENSE`](LICENSE).
