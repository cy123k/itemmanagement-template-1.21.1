# Item Management

`Item Management` is a NeoForge mod for Minecraft `1.21.1` focused on item blocking and item replacement for modpacks and custom worlds.

## What It Does

- Block items by replacing them with `minecraft:air`
- Replace one item with another item
- Filter blocked or replaced items from:
  - world item entities
  - player tosses
  - living entity drops
  - loot table output
  - opened containers
  - optionally player inventories
- Hide truly blocked items from creative tabs
- Edit world rules in-game through a keybind-driven GUI
- Export the current world rules as a default-world template
- Initialize new worlds from pack-defined default rules
- Let world creation decide whether pack default rules should be applied through a custom `GameRule`

## Main User Flows

### In-Game Rule Editing

- Press the configured keybind to open the blocked/replacement rule screen
- Add rules from:
  - current main hand item
  - searched item ids
  - wildcard patterns such as `minecraft:*_log`
- Change a rule's replacement target
- Remove entire rules or individual entries inside grouped wildcard rules
- Save rules to the current world

### Modpack Author Workflow

- Define pack-level world defaults in config
- Create a world and decide whether to apply those defaults through the `itemmanagementUsePackDefaultRules` gamerule
- Tune rules in-game if needed
- Export the tuned world rules to a TOML template file

## Important Files

- Main mod entry: [src/main/java/com/item_management/Itemmanagement.java](src/main/java/com/item_management/Itemmanagement.java)
- Config: [src/main/java/com/item_management/Config.java](src/main/java/com/item_management/Config.java)
- Runtime rule manager: [src/main/java/com/item_management/service/BlockedItemsManager.java](src/main/java/com/item_management/service/BlockedItemsManager.java)
- World initialization: [src/main/java/com/item_management/service/WorldRuleInitializationService.java](src/main/java/com/item_management/service/WorldRuleInitializationService.java)
- Rule export: [src/main/java/com/item_management/service/DefaultRuleExportService.java](src/main/java/com/item_management/service/DefaultRuleExportService.java)
- GUI: [src/main/java/com/item_management/client/gui/BlockedItemsScreen.java](src/main/java/com/item_management/client/gui/BlockedItemsScreen.java)
- Network: [src/main/java/com/item_management/network/ModNetwork.java](src/main/java/com/item_management/network/ModNetwork.java)
- Loot filtering: [src/main/java/com/item_management/loot/BlockedItemsLootModifier.java](src/main/java/com/item_management/loot/BlockedItemsLootModifier.java)

## Build

```powershell
./gradlew compileJava
```

## Docs

- See [docs/implemented-features.md](docs/implemented-features.md) for a fuller overview of the systems currently implemented in the mod.
