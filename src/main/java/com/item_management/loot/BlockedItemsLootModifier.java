package com.item_management.loot;

import com.item_management.Config;
import com.item_management.Itemmanagement;
import com.item_management.ItemReplacementService;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class BlockedItemsLootModifier extends LootModifier {
    public static final MapCodec<BlockedItemsLootModifier> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance ->
                    codecStart(instance).apply(instance, BlockedItemsLootModifier::new));

    public BlockedItemsLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ObjectArrayList<ItemStack> filteredLoot = new ObjectArrayList<>();
        for (ItemStack stack : generatedLoot) {
            ItemStack replaced = ItemReplacementService.replaceStack(stack);
            logFilteredLoot(stack, replaced);
            if (!replaced.isEmpty()) {
                filteredLoot.add(replaced);
            }
        }

        return filteredLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.FILTER_BLOCKED_ITEMS.get();
    }

    private void logFilteredLoot(ItemStack original, ItemStack replaced) {
        if (!Config.shouldLogFilteredLoot() || ItemStack.matches(original, replaced)) {
            return;
        }

        String originalId = BuiltInRegistries.ITEM.getKey(original.getItem()).toString();
        if (replaced.isEmpty() || replaced.getItem() == Items.AIR) {
            Itemmanagement.LOGGER.info("Filtered loot stack: {} x{} -> removed", originalId, original.getCount());
            return;
        }

        String replacementId = BuiltInRegistries.ITEM.getKey(replaced.getItem()).toString();
        Itemmanagement.LOGGER.info("Filtered loot stack: {} x{} -> {} x{}", originalId, original.getCount(), replacementId, replaced.getCount());
    }
}
