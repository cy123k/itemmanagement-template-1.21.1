package com.item_management;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Itemmanagement.MODID)
public final class ItemReplacementEvents {
    private static final int INVENTORY_SANITIZE_INTERVAL = 100;
    private static final int OPEN_CONTAINER_SANITIZE_INTERVAL = 20;

    private ItemReplacementEvents() {}

    @SubscribeEvent
    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sanitizePlayerState(event.getEntity(), false);
    }

    @SubscribeEvent
    static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sanitizePlayerState(event.getEntity(), false);
    }

    @SubscribeEvent
    static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sanitizePlayerState(event.getEntity(), false);
    }

    @SubscribeEvent
    static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        sanitizePlayerState(event.getEntity(), true);
    }

    @SubscribeEvent
    static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        sanitizePlayerState(event.getEntity(), true);
    }

    @SubscribeEvent
    static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        ItemReplacementService.sanitizeContainer(event.getContainer());
    }

    @SubscribeEvent
    static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer().level().isClientSide()) {
            return;
        }

        sanitizePlayerState(event.getPlayer(), true);
    }

    @SubscribeEvent
    static void onItemEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemReplacementService.sanitizeItemEntity(itemEntity);
            if (!itemEntity.isAlive()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer().level().isClientSide()) {
            return;
        }

        ItemEntity itemEntity = event.getEntity();
        ItemReplacementService.sanitizeItemEntity(itemEntity);
        if (!itemEntity.isAlive()) {
            event.setCanceled(true);
        }

        sanitizePlayerState(event.getPlayer(), true);
    }

    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        event.getDrops().removeIf(itemEntity -> {
            ItemReplacementService.sanitizeItemEntity(itemEntity);
            return !itemEntity.isAlive();
        });
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        Player player = event.getEntity();
        if (Config.isPlayerInventoryScanEnabled() && player.tickCount % INVENTORY_SANITIZE_INTERVAL == 0) {
            ItemReplacementService.sanitizePlayerInventory(player);
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (menu != player.inventoryMenu && player.tickCount % OPEN_CONTAINER_SANITIZE_INTERVAL == 0) {
            ItemReplacementService.sanitizeContainer(menu);
        }
    }

    private static void sanitizePlayerState(Player player, boolean includeInventory) {
        if (player.level().isClientSide()) {
            return;
        }

        if (includeInventory && Config.isPlayerInventoryScanEnabled()) {
            ItemReplacementService.sanitizePlayerInventory(player);
        }

        if (player.containerMenu != player.inventoryMenu) {
            ItemReplacementService.sanitizeContainer(player.containerMenu);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }
    }
}
