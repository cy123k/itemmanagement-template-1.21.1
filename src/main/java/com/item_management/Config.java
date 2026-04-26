package com.item_management;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ITEM_REPLACEMENT_ENABLED = BUILDER
            .comment("Enable item replacement and blocking.")
            .define("itemReplacementEnabled", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_REPLACEMENTS = BUILDER
            .comment("Replacement rules in the form source_mod:item=target_mod:item. Use minecraft:air to delete the item.")
            .defineListAllowEmpty(
                    "itemReplacements",
                    List.of(),
                    () -> "",
                    Config::validateReplacementEntry);

    public static final ModConfigSpec.BooleanValue DEFAULT_WORLD_RULES_ENABLED = BUILDER
            .comment("Apply the pack's default item rules when a world is initialized for the first time.")
            .define("defaultWorldRulesEnabled", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DEFAULT_WORLD_ITEM_REPLACEMENTS = BUILDER
            .comment("Default replacement rules to write into a world when it is initialized for the first time.")
            .defineListAllowEmpty(
                    "defaultWorldItemReplacements",
                    List.of(),
                    () -> "",
                    Config::validateReplacementEntry);

    public static final ModConfigSpec.BooleanValue LOG_FILTERED_LOOT = BUILDER
            .comment("Log loot entries that were removed or replaced by item blocking rules.")
            .define("logFilteredLoot", false);

    public static final ModConfigSpec.BooleanValue ENABLE_PLAYER_INVENTORY_SCAN = BUILDER
            .comment("Enable scanning player inventories for blocked or replaced items. Default: false.")
            .define("enablePlayerInventoryScan", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static final ResourceLocation AIR_ID = ResourceLocation.parse("minecraft:air");
    private static Map<ResourceLocation, ResourceLocation> replacementIdMap = Map.of();
    private static Map<Item, Item> replacementMap = Map.of();
    private static Map<ResourceLocation, ResourceLocation> defaultWorldReplacementIdMap = Map.of();

    public static boolean isItemReplacementEnabled() {
        return ITEM_REPLACEMENT_ENABLED.getAsBoolean();
    }

    public static Optional<Item> getReplacement(Item item) {
        return Optional.ofNullable(replacementMap.get(item));
    }

    public static boolean shouldApplyDefaultWorldRules() {
        return DEFAULT_WORLD_RULES_ENABLED.getAsBoolean();
    }

    public static boolean shouldLogFilteredLoot() {
        return LOG_FILTERED_LOOT.getAsBoolean();
    }

    public static boolean isPlayerInventoryScanEnabled() {
        return ENABLE_PLAYER_INVENTORY_SCAN.getAsBoolean();
    }

    public static Map<ResourceLocation, ResourceLocation> getDefaultReplacementIds() {
        return replacementIdMap;
    }

    public static Map<ResourceLocation, ResourceLocation> getDefaultWorldReplacementIds() {
        return defaultWorldReplacementIdMap;
    }

    public static Set<ResourceLocation> getDefaultBlockedIds() {
        return replacementIdMap.entrySet().stream()
                .filter(entry -> AIR_ID.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<Item> getBlockedItems() {
        return getDefaultBlockedIds().stream()
                .map(BuiltInRegistries.ITEM::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean validateReplacementEntry(final Object obj) {
        if (!(obj instanceof String entry)) {
            return false;
        }

        String[] parts = entry.split("=", 2);
        if (parts.length != 2) {
            return false;
        }

        ResourceLocation sourceId = ResourceLocation.tryParse(parts[0].trim());
        ResourceLocation targetId = ResourceLocation.tryParse(parts[1].trim());
        if (sourceId == null || targetId == null) {
            return false;
        }

        return BuiltInRegistries.ITEM.containsKey(sourceId) && BuiltInRegistries.ITEM.containsKey(targetId);
    }

    private static void rebuildReplacementMaps() {
        replacementIdMap = parseReplacementIdMap(ITEM_REPLACEMENTS.get());
        replacementMap = buildReplacementItemMap(replacementIdMap);
        defaultWorldReplacementIdMap = parseReplacementIdMap(DEFAULT_WORLD_ITEM_REPLACEMENTS.get());

        Itemmanagement.LOGGER.info(
                "Loaded {} runtime replacement rule(s) and {} default world replacement rule(s)",
                replacementMap.size(),
                defaultWorldReplacementIdMap.size());
    }

    private static Map<ResourceLocation, ResourceLocation> parseReplacementIdMap(List<? extends String> entries) {
        Map<ResourceLocation, ResourceLocation> rebuiltIds = new LinkedHashMap<>();

        for (String entry : entries) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            ResourceLocation sourceId = ResourceLocation.tryParse(parts[0].trim());
            ResourceLocation targetId = ResourceLocation.tryParse(parts[1].trim());
            if (sourceId == null || targetId == null) {
                continue;
            }

            if (BuiltInRegistries.ITEM.containsKey(sourceId) && BuiltInRegistries.ITEM.containsKey(targetId)) {
                rebuiltIds.put(sourceId, targetId);
            }
        }

        return Map.copyOf(rebuiltIds);
    }

    private static Map<Item, Item> buildReplacementItemMap(Map<ResourceLocation, ResourceLocation> replacementIds) {
        Map<Item, Item> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : replacementIds.entrySet()) {
            rebuilt.put(BuiltInRegistries.ITEM.get(entry.getKey()), BuiltInRegistries.ITEM.get(entry.getValue()));
        }

        return Map.copyOf(rebuilt);
    }

    static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildReplacementMaps();
        }
    }

    static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            rebuildReplacementMaps();
        }
    }
}
