package com.item_management;

import com.item_management.service.BlockedItemsManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemReplacementService {
    private ItemReplacementService() {}

    public static boolean sanitizePlayerInventory(Player player) {
        boolean changed = false;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            ItemStack replaced = replaceStack(current);
            if (!ItemStack.matches(current, replaced)) {
                player.getInventory().setItem(slot, replaced);
                changed = true;
            }
        }

        ItemStack carried = player.containerMenu.getCarried();
        ItemStack replacedCarried = replaceStack(carried);
        if (!ItemStack.matches(carried, replacedCarried)) {
            player.containerMenu.setCarried(replacedCarried);
            changed = true;
        }

        if (changed) {
            player.containerMenu.broadcastChanges();
        }

        return changed;
    }

    public static boolean sanitizeContainer(AbstractContainerMenu menu) {
        boolean changed = false;

        for (Slot slot : menu.slots) {
            ItemStack current = slot.getItem();
            ItemStack replaced = replaceStack(current);
            if (!ItemStack.matches(current, replaced)) {
                slot.set(replaced);
                slot.setChanged();
                changed = true;
            }
        }

        ItemStack carried = menu.getCarried();
        ItemStack replacedCarried = replaceStack(carried);
        if (!ItemStack.matches(carried, replacedCarried)) {
            menu.setCarried(replacedCarried);
            changed = true;
        }

        if (changed) {
            menu.broadcastChanges();
        }

        return changed;
    }

    public static boolean sanitizeItemEntity(ItemEntity itemEntity) {
        ItemStack current = itemEntity.getItem();
        ItemStack replaced = replaceStack(current);
        if (ItemStack.matches(current, replaced)) {
            return false;
        }

        if (replaced.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(replaced);
        }

        return true;
    }

    public static ItemStack replaceStack(ItemStack stack) {
        if (!Config.isItemReplacementEnabled() || stack.isEmpty()) {
            return stack;
        }

        Item item = stack.getItem();
        Item replacement = BlockedItemsManager.getReplacement(item).orElse(null);
        if (replacement == null || replacement == item) {
            return stack;
        }

        if (replacement == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(replacement, stack.getCount());
    }

    public static void sanitizeLoadedServerState(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            level.getEntities().getAll().forEach(entity -> {
                if (entity instanceof ItemEntity itemEntity) {
                    sanitizeItemEntity(itemEntity);
                }
            });
        }

        server.getPlayerList().getPlayers().forEach(ItemReplacementService::sanitizePlayerInventory);
    }
}
