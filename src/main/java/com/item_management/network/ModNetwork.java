package com.item_management.network;

import java.util.List;
import java.util.stream.Collectors;

import com.item_management.ItemReplacementService;
import com.item_management.service.BlockedItemsManager;
import com.item_management.client.ClientBlockedItemsViewState;
import com.item_management.data.BlockedItemsSavedData.BlockedEntryData;
import com.item_management.network.SyncBlockedItemsPayload.BlockedEntryPayload;
import com.item_management.service.DefaultRuleExportService;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetwork {
    private static final String NETWORK_VERSION = "1";
    private static final ResourceLocation AIR_ID = ResourceLocation.parse("minecraft:air");

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(RequestBlockedItemsPayload.TYPE, RequestBlockedItemsPayload.STREAM_CODEC, ModNetwork::handleRequestBlockedItems);
        registrar.playToServer(ExportDefaultRulesPayload.TYPE, ExportDefaultRulesPayload.STREAM_CODEC, ModNetwork::handleExportDefaultRules);
        registrar.playToClient(SyncBlockedItemsPayload.TYPE, SyncBlockedItemsPayload.STREAM_CODEC, ModNetwork::handleSyncBlockedItems);
        registrar.playToServer(UpdateBlockedItemsPayload.TYPE, UpdateBlockedItemsPayload.STREAM_CODEC, ModNetwork::handleUpdateBlockedItems);
    }

    public static void requestBlockedItems() {
        PacketDistributor.sendToServer(new RequestBlockedItemsPayload());
    }

    public static void exportDefaultRules() {
        PacketDistributor.sendToServer(new ExportDefaultRulesPayload());
    }

    public static void updateBlockedItems(List<BlockedEntryPayload> entries) {
        PacketDistributor.sendToServer(new UpdateBlockedItemsPayload(entries));
    }

    private static void handleRequestBlockedItems(RequestBlockedItemsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        PacketDistributor.sendToPlayer(player, SyncBlockedItemsPayload.fromSavedEntries(BlockedItemsManager.getSavedDisplayEntries(player.server)));
    }

    private static void handleSyncBlockedItems(SyncBlockedItemsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientBlockedItemsViewState.setEntries(payload.toSavedEntries()));
    }

    private static void handleExportDefaultRules(ExportDefaultRulesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("message.itemmanagement.no_permission"));
            return;
        }

        try {
            var exportPath = DefaultRuleExportService.exportWorldRules(server, BlockedItemsManager.getSavedDisplayEntries(server));
            player.sendSystemMessage(Component.translatable("message.itemmanagement.export_success", exportPath.getFileName().toString()));
        } catch (java.io.IOException exception) {
            player.sendSystemMessage(Component.translatable("message.itemmanagement.export_failed"));
        }
    }

    private static void handleUpdateBlockedItems(UpdateBlockedItemsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("message.itemmanagement.no_permission"));
            PacketDistributor.sendToPlayer(player, SyncBlockedItemsPayload.fromSavedEntries(BlockedItemsManager.getSavedDisplayEntries(server)));
            return;
        }

        List<BlockedEntryData> sanitizedEntries = payload.entries().stream()
                .map(BlockedEntryPayload::toSavedEntry)
                .map(entry -> {
                    List<ResourceLocation> validItems = entry.itemIds().stream()
                            .filter(id -> net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))
                            .distinct()
                            .toList();
                    ResourceLocation replacementId = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(entry.replacementId())
                            ? entry.replacementId()
                            : AIR_ID;
                    return new BlockedEntryData(entry.grouped(), entry.label(), validItems, replacementId, entry.expanded());
                })
                .filter(entry -> !entry.itemIds().isEmpty())
                .collect(Collectors.toList());

        BlockedItemsManager.setWorldEntries(server, sanitizedEntries);
        ItemReplacementService.sanitizeLoadedServerState(server);

        SyncBlockedItemsPayload syncPayload = SyncBlockedItemsPayload.fromSavedEntries(BlockedItemsManager.getSavedDisplayEntries(server));
        PacketDistributor.sendToAllPlayers(syncPayload);
    }
}
