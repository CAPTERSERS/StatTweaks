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

#### Special Attributes (for `attributes` section):
- `stattweaks:durability`: Maximum durability of the item.
- `stattweaks:efficiency`: Mining speed for tools (updates tool mining rules).

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
    "minecraft:fire_resistant": {},
    "minecraft:max_stack_size": 32
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

### 3. Available Attributes (for Items and Entities)

| Attribute ID | What it does | Example Value |
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

**Note:** You can also use attributes from other mods by using their ResourceLocation (e.g., `modid:custom_attribute`).

### 4. Available Data Components (for Items)

| Component ID | What it does | Example Value |
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
| `minecraft:entity_data` | Entity NBT data (e.g. egg variants) | `{...}` |
| `minecraft:repair_cost` | Repair cost in anvil | `5` |
| `minecraft:damage_resistant` | Damage resistance flag | `{}` |
| `minecraft:death_protection` | Protection on death | `{}` |
| `minecraft:tooltip_style` | Custom tooltip style | `"minecraft:style"` |

**Note:** You can also use data components from other mods. Use `minecraft:` namespace for vanilla components or `modid:` for modded ones.

### 5. Mod Compatibility 🧩
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
