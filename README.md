# StatTweaks

> **Power over your inventory.** Dynamically modify item attributes for Vanilla and Modded content.

**StatTweaks** is a mod focused on dynamically modifying item attributes via a simple configuration file. Fully compatible with content from other mods, it allows you to easily adjust parameters such as damage, speed, durability, stack size, and reach.

Ideal for modpack creators looking for advanced balance tuning without writing code.

## 📊 Modifiable Attributes
Below is the list of keys you can use in the config file, along with reference values to help you balance your items.

| Config Key | Description                               | Reference / Typical Range |
| :--- |:------------------------------------------| :--- |
| `damage` | The attack damage dealt to an entity.     | **Vanilla Hand:** 1.0 <br> **Diamond Sword:** 7.0 |
| `speed` | Attack speed (attacks per second).        | **Slow (Axe):** ~0.8 - 1.0 <br> **Fast (Sword):** 1.6 <br> **Instant:** 4.0+ |
| `reach` | Interaction distance for blocks/entities. | **Default:** ~4.5 - 5.0 |
| `knockback` | Force used to push enemies away.          | **Default:** 0.0 <br> **Strong:** 1.0 - 3.0 |
| `armor` | Defense points (1 point = half shield).   | **Iron Chest:** 6.0 <br> **Diamond Chest:** 8.0 |
| `toughness` | Armor Toughness (high-damage resistance). | **Diamond:** 2.0 <br> **Netherite:** 3.0 |
| `knockback_resistance`| Probability to resist being pushed back.  | **Range:** 0.0 (0%) to 1.0 (100%) <br> **Netherite:** 0.1 |
| `movement_speed` | Speed modifier when wearing the item.     | **Warning:** Sensitive value! <br> **Default Player Speed:** ~0.1 |
| `durability` | Max uses before breaking.                 | **Iron:** 250 <br> **Diamond:** 1561 |
| `efficiency` | Mining speed for tools.                   | **Iron:** 6.0 <br> **Gold:** 12.0 |
| `stack_size` | Max items per slot.                       | **Min:** 1 <br> **Max:** 64 |

> **⚠️ IMPORTANT NOTE:**
> While some attribute changes may update instantly, **modifying core properties like `stack_size`, `durability`, or `efficiency` usually requires a complete Game Restart** to take effect correctly.

## 🔮 Future Updates
We are actively working on StatTweaks!
* **More Attributes:** We plan to add support for additional attributes and mechanics in future versions.
* Stay tuned for updates to expand your customization options.

## ⚙️ Configuration
The configuration uses a simple Key-Value JSON format located at `config/CPT_StatTweaks_Config.json`.

You simply refer to the item by its **Registry Name** (e.g., `minecraft:diamond_sword`) and list the stats you want to change.

**Example Configuration:**
```json
{
  "minecraft:diamond_sword": {
    "damage": 10.0,
    "reach": 5.0,
    "movement_speed": 0.6
  },
  "minecraft:ender_pearl": {
    "stack_size": 64
  },
  "minecraft:iron_pickaxe": {
    "efficiency": 12.0,
    "durability": 1000
  },
  "alexsmobs:moose_chestplate": {
    "armor": 8.0,
    "knockback_resistance": 0.5
  }
}