package com.item_management.loot;

import com.item_management.Itemmanagement;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Itemmanagement.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<BlockedItemsLootModifier>> FILTER_BLOCKED_ITEMS =
            GLOBAL_LOOT_MODIFIERS.register("filter_blocked_items", () -> BlockedItemsLootModifier.CODEC);

    private ModLootModifiers() {}
}
