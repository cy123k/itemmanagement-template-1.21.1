package com.item_management.network;

import com.item_management.Itemmanagement;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestBlockedItemsPayload() implements CustomPacketPayload {
    public static final Type<RequestBlockedItemsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemmanagement.MODID, "request_blocked_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBlockedItemsPayload> STREAM_CODEC = StreamCodec.unit(new RequestBlockedItemsPayload());

    @Override
    public Type<RequestBlockedItemsPayload> type() {
        return TYPE;
    }
}
