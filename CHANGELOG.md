# Changelog

All notable changes to StatTweaks for Minecraft 1.21.9 are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-11

### Added

- JSON configuration for item and entity attributes, loaded through `STBalanceManager`, with support for item tags and specific registry IDs.
- Item properties for damage, speed, reach, durability, and stack size, including a `StatAccessor` mixin into `DataComponentMap`.
- Attribute and component overrides for items and entities, additive tags (`#`), and `base` or `relative` tooltip modes.
- Unique modifier IDs so armor and toughness do not stack incorrectly.
- Tool efficiency that updates mining rules for the matching blocks.
- Proportional health scaling when an entity's max health changes.
- Server-to-client configuration sync through custom network packets, on Fabric and NeoForge.
- Config loading that accepts null, empty, or partial JSON.
- Mod icon.
- JSON coverage for armor stats, tool efficiency, and entity attributes.
- Attribute and component lookup that falls back when vanilla names change, including `generic.attack_damage`.

### Changed

- Ported the mod to Minecraft 1.21.9 (Architectury API 18.0.5, Fabric API 0.134.0+1.21.9, NeoForge 21.9.9-beta).
- `ItemStackMixin` follows the 1.21.9 `TooltipDisplay` signature.
- `/stattweaks reload` does not apply the JSON on this Minecraft version. Rejoin the world to apply changes.

### Fixed

- Compilation errors and crashes from the move to Minecraft 1.21.9.
- Armor and toughness stacking, by using a unique modifier ID per item.
