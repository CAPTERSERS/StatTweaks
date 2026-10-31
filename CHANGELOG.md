# Changelog

All notable changes to StatTweaks for Minecraft 1.21.1 are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-11

### Added

- JSON configuration for item and entity attributes, loaded through `STBalanceManager`, with support for item tags and specific registry IDs.
- Item properties for damage, speed, reach, durability, and stack size, including a `StatAccessor` mixin into `DataComponentMap`.
- Backup and restore so configuration can be reloaded with `/stattweaks reload`.
- Attribute and component overrides for items and entities, additive tags (`#`), and `base` or `relative` tooltip modes.
- Unique modifier IDs so armor and toughness do not stack incorrectly.
- Tool efficiency that updates mining rules for the matching blocks.
- Proportional health scaling when an entity's max health changes.
- Server-to-client configuration sync, with the command available on Fabric and NeoForge.
- Config loading that accepts null, empty, or partial JSON.
- Mod icon.
- README tables for the attributes and data components on this branch. Stack size is documented as the `minecraft:max_stack_size` component.

### Changed

- README component table no longer lists fire resistance as a separate documented row.

### Fixed

- Armor and toughness stacking, by using a unique modifier ID per item.
