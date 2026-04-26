package com.item_management.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.item_management.Config;
import com.item_management.Itemmanagement;
import com.item_management.data.BlockedItemsSavedData.BlockedEntryData;
import com.item_management.data.BlockedItemsSavedData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.neoforged.fml.event.config.ModConfigEvent;

public final class BlockedItemsManager {
    private static final ResourceLocation AIR_ID = ResourceLocation.parse("minecraft:air");

    @Nullable
    private static MinecraftServer currentServer;
    private static Map<ResourceLocation, ResourceLocation> activeReplacementIds = Map.of();
    private static Map<Item, Item> activeReplacementMap = Map.of();
    private static Set<Item> activeBlockedItems = Set.of();

    private BlockedItemsManager() {}

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            refreshRuntimeRules();
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            refreshRuntimeRules();
        }
    }

    public static void loadServerRules(MinecraftServer server) {
        currentServer = server;

        BlockedItemsSavedData savedData = BlockedItemsSavedData.get(server);
        Map<ResourceLocation, ResourceLocation> merged = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, ResourceLocation> entry : Config.getDefaultReplacementIds().entrySet()) {
            if (!AIR_ID.equals(entry.getValue()) || !savedData.hasCustomRulesSet()) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }

        if (savedData.hasCustomRulesSet()) {
            for (BlockedEntryData entry : savedData.getDisplayEntries()) {
                for (ResourceLocation itemId : entry.itemIds()) {
                    merged.put(itemId, entry.replacementId());
                }
            }
        }

        applyRules(merged);
        Itemmanagement.LOGGER.info(
                "Loaded {} runtime replacement rule(s), including {} world-level rule source item(s); custom blocked list present: {}",
                activeReplacementMap.size(),
                savedData.getDisplayEntries().stream().mapToInt(entry -> entry.itemIds().size()).sum(),
                savedData.hasCustomRulesSet());
    }

    public static void unloadServerRules() {
        currentServer = null;
        applyRules(Config.getDefaultReplacementIds());
    }

    public static void setWorldBlockedIds(MinecraftServer server, Collection<ResourceLocation> blockedIds) {
        BlockedItemsSavedData savedData = BlockedItemsSavedData.get(server);
        savedData.setEntries(blockedIds.stream()
                .map(id -> new BlockedEntryData(false, id.toString(), java.util.List.of(id), AIR_ID, false))
                .toList());
        loadServerRules(server);
    }

    public static void setWorldEntries(MinecraftServer server, Collection<BlockedEntryData> entries) {
        BlockedItemsSavedData savedData = BlockedItemsSavedData.get(server);
        savedData.setEntries(entries);
        loadServerRules(server);
    }

    public static Set<ResourceLocation> getActiveBlockedIds() {
        return activeReplacementIds.entrySet().stream()
                .filter(entry -> AIR_ID.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Optional<Item> getReplacement(Item item) {
        return Optional.ofNullable(activeReplacementMap.get(item));
    }

    public static Set<Item> getBlockedItems() {
        return activeBlockedItems;
    }

    public static List<BlockedEntryData> getSavedDisplayEntries(MinecraftServer server) {
        return BlockedItemsSavedData.get(server).getDisplayEntries();
    }

    private static void refreshRuntimeRules() {
        if (currentServer != null) {
            loadServerRules(currentServer);
        } else {
            applyRules(Config.getDefaultReplacementIds());
        }
    }

    private static void applyRules(Map<ResourceLocation, ResourceLocation> replacementIds) {
        Map<ResourceLocation, ResourceLocation> rebuiltIds = new LinkedHashMap<>();
        Map<Item, Item> rebuilt = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, ResourceLocation> entry : replacementIds.entrySet()) {
            if (!BuiltInRegistries.ITEM.containsKey(entry.getKey()) || !BuiltInRegistries.ITEM.containsKey(entry.getValue())) {
                continue;
            }

            rebuiltIds.put(entry.getKey(), entry.getValue());
            rebuilt.put(BuiltInRegistries.ITEM.get(entry.getKey()), BuiltInRegistries.ITEM.get(entry.getValue()));
        }

        activeReplacementIds = Map.copyOf(rebuiltIds);
        activeReplacementMap = Map.copyOf(rebuilt);
        activeBlockedItems = activeReplacementIds.entrySet().stream()
                .filter(entry -> AIR_ID.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(BuiltInRegistries.ITEM::get)
                .collect(Collectors.toUnmodifiableSet());
    }
}
