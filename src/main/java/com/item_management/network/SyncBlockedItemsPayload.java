package com.item_management.network;

import java.util.ArrayList;
import java.util.List;

import com.item_management.Itemmanagement;
import com.item_management.data.BlockedItemsSavedData.BlockedEntryData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncBlockedItemsPayload(List<BlockedEntryPayload> entries) implements CustomPacketPayload {
    public static final Type<SyncBlockedItemsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemmanagement.MODID, "sync_blocked_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockedEntryPayload> ENTRY_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.cast(),
            BlockedEntryPayload::grouped,
            ByteBufCodecs.STRING_UTF8.cast(),
            BlockedEntryPayload::label,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(512)).cast(),
            BlockedEntryPayload::itemIds,
            ByteBufCodecs.STRING_UTF8.cast(),
            BlockedEntryPayload::replacementId,
            ByteBufCodecs.BOOL.cast(),
            BlockedEntryPayload::expanded,
            BlockedEntryPayload::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBlockedItemsPayload> STREAM_CODEC = StreamCodec.composite(
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(512)).cast(),
            SyncBlockedItemsPayload::entries,
            SyncBlockedItemsPayload::new);

    public SyncBlockedItemsPayload(List<BlockedEntryPayload> entries) {
        this.entries = List.copyOf(entries);
    }

    @Override
    public Type<SyncBlockedItemsPayload> type() {
        return TYPE;
    }

    public static SyncBlockedItemsPayload fromSavedEntries(List<BlockedEntryData> entries) {
        return new SyncBlockedItemsPayload(entries.stream().map(BlockedEntryPayload::fromSavedEntry).toList());
    }

    public List<BlockedEntryData> toSavedEntries() {
        List<BlockedEntryData> savedEntries = new ArrayList<>();
        for (BlockedEntryPayload entry : entries) {
            savedEntries.add(entry.toSavedEntry());
        }
        return savedEntries;
    }

    public record BlockedEntryPayload(boolean grouped, String label, List<String> itemIds, String replacementId, boolean expanded) {
        public BlockedEntryPayload {
            itemIds = List.copyOf(itemIds);
        }

        public static BlockedEntryPayload fromSavedEntry(BlockedEntryData entry) {
            return new BlockedEntryPayload(
                    entry.grouped(),
                    entry.label(),
                    entry.itemIds().stream().map(ResourceLocation::toString).toList(),
                    entry.replacementId().toString(),
                    entry.expanded());
        }

        public BlockedEntryData toSavedEntry() {
            return new BlockedEntryData(
                    grouped,
                    label,
                    itemIds.stream().map(ResourceLocation::parse).toList(),
                    ResourceLocation.parse(replacementId),
                    expanded);
        }
    }
}
