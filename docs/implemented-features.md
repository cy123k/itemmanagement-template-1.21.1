# Implemented Features

This document summarizes what has already been built in the mod.

## 1. Runtime Item Blocking And Replacement

The mod supports world-level rules of the form:

- `source_item -> minecraft:air`
- `source_item -> target_item`

Runtime rule expansion is managed by:

- [src/main/java/com/item_management/service/BlockedItemsManager.java](../src/main/java/com/item_management/service/BlockedItemsManager.java)

Rules are ultimately applied through:

- [src/main/java/com/item_management/ItemReplacementService.java](../src/main/java/com/item_management/ItemReplacementService.java)

## 2. World-Persistent Rule Storage

Current world rules are saved in:

- [src/main/java/com/item_management/data/BlockedItemsSavedData.java](../src/main/java/com/item_management/data/BlockedItemsSavedData.java)

Stored rule entries support:

- single-item rules
- grouped rules generated from wildcard input
- replacement target ids
- expanded or collapsed GUI state

## 3. In-Game GUI Editor

The in-game rule editor is implemented in:

- [src/main/java/com/item_management/client/gui/BlockedItemsScreen.java](../src/main/java/com/item_management/client/gui/BlockedItemsScreen.java)

Current capabilities:

- open from a client keybind
- search item ids
- wildcard add such as `*_log`
- grouped display for wildcard-added items
- remove full rules
- remove individual items from a group
- edit replacement target after clicking a button
- `Tab` autocomplete for search and replacement input
- export current world rules

## 4. Networking

Client and server synchronization is handled by:

- [src/main/java/com/item_management/network/ModNetwork.java](../src/main/java/com/item_management/network/ModNetwork.java)

Implemented payloads:

- request current rules
- sync current rules to client
- update saved rules from GUI
- export current world rules as a template

## 5. Drop, Loot, And Container Filtering

Filtering currently covers:

- item entities joining the world
- player item tosses
- living entity drops
- loot table output through a global loot modifier
- opened containers
- optionally player inventories

Relevant files:

- [src/main/java/com/item_management/ItemReplacementEvents.java](../src/main/java/com/item_management/ItemReplacementEvents.java)
- [src/main/java/com/item_management/loot/BlockedItemsLootModifier.java](../src/main/java/com/item_management/loot/BlockedItemsLootModifier.java)
- [src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json](../src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json)

## 6. Creative Tab Hiding

Items that are truly blocked by `-> minecraft:air` are removed from creative tabs and search tabs.

This is wired from:

- [src/main/java/com/item_management/Itemmanagement.java](../src/main/java/com/item_management/Itemmanagement.java)

## 7. World Initialization From Pack Defaults

The mod now supports pack-level default world rules defined in config and applied when a world is initialized for the first time.

Relevant pieces:

- [src/main/java/com/item_management/Config.java](../src/main/java/com/item_management/Config.java)
- [src/main/java/com/item_management/service/WorldRuleInitializationService.java](../src/main/java/com/item_management/service/WorldRuleInitializationService.java)

Config-side defaults:

- `defaultWorldRulesEnabled`
- `defaultWorldItemReplacements`

## 8. Create-World Integration Through GameRule

A custom `GameRule` controls whether a newly initialized world should consume pack default rules:

- `itemmanagementUsePackDefaultRules`

Registration:

- [src/main/java/com/item_management/ModGameRules.java](../src/main/java/com/item_management/ModGameRules.java)

This is intended to appear in the create-world flow under game rules, so the player can choose before world initialization happens.

## 9. Export Current World Rules

Current saved world rules can be exported as a TOML template file for reuse as modpack defaults.

Service:

- [src/main/java/com/item_management/service/DefaultRuleExportService.java](../src/main/java/com/item_management/service/DefaultRuleExportService.java)

Current output file:

- `itemmanagement-exported-default-world-rules.toml`

The exported file is designed to be copied back into config as a starting point for:

- `defaultWorldItemReplacements`

## 10. Current Known Boundaries

- Replacement currently rebuilds `ItemStack` with only the new item id and count, so complex item data is not preserved.
- Machine-internal inventories from some mods may still need dedicated compatibility work.
- World rule export currently writes expanded item mappings, not compact wildcard expressions.
- The create-world integration is currently based on a `GameRule` switch, not a custom rule editor embedded directly into the world creation UI.
