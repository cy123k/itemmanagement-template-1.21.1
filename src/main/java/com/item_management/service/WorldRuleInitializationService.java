package com.item_management.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.item_management.Config;
import com.item_management.Itemmanagement;
import com.item_management.ModGameRules;
import com.item_management.data.BlockedItemsSavedData;
import com.item_management.data.BlockedItemsSavedData.BlockedEntryData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

public final class WorldRuleInitializationService {
    private static final ResourceLocation AIR_ID = ResourceLocation.parse("minecraft:air");

    private WorldRuleInitializationService() {}

    public static void initializeWorldRulesIfNeeded(MinecraftServer server) {
        BlockedItemsSavedData savedData = BlockedItemsSavedData.get(server);
        if (savedData.hasCustomRulesSet()) {
            return;
        }

        List<BlockedEntryData> initialEntries = new ArrayList<>();
        boolean usePackDefaultRules = server.getGameRules().getBoolean(ModGameRules.USE_PACK_DEFAULT_ITEM_RULES);
        boolean applyDefaultRules = Config.shouldApplyDefaultWorldRules() && usePackDefaultRules;
        if (applyDefaultRules) {
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : Config.getDefaultWorldReplacementIds().entrySet()) {
                initialEntries.add(new BlockedEntryData(
                        false,
                        entry.getKey().toString(),
                        List.of(entry.getKey()),
                        entry.getValue(),
                        false));
            }
        }

        savedData.setEntries(initialEntries);
        Itemmanagement.LOGGER.info(
                "Initialized world item rules with {} entry(s); config enabled: {}, gamerule enabled: {}",
                initialEntries.size(),
                Config.shouldApplyDefaultWorldRules(),
                usePackDefaultRules);
    }
}
