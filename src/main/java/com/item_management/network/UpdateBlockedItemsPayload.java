package com.item_management.network;

import java.util.List;

import com.item_management.Itemmanagement;
import com.item_management.network.SyncBlockedItemsPayload.BlockedEntryPayload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateBlockedItemsPayload(List<BlockedEntryPayload> entries) implements CustomPacketPayload {
    public static final Type<UpdateBlockedItemsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemmanagement.MODID, "update_blocked_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBlockedItemsPayload> STREAM_CODEC = StreamCodec.composite(
            SyncBlockedItemsPayload.ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(512)).cast(),
            UpdateBlockedItemsPayload::entries,
            UpdateBlockedItemsPayload::new);

    public UpdateBlockedItemsPayload(List<BlockedEntryPayload> entries) {
        this.entries = List.copyOf(entries);
    }

    @Override
    public Type<UpdateBlockedItemsPayload> type() {
        return TYPE;
    }
}
