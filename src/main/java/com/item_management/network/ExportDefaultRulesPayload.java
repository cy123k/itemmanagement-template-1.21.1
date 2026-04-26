package com.item_management.network;

import com.item_management.Itemmanagement;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExportDefaultRulesPayload() implements CustomPacketPayload {
    public static final Type<ExportDefaultRulesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Itemmanagement.MODID, "export_default_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportDefaultRulesPayload> STREAM_CODEC = StreamCodec.unit(new ExportDefaultRulesPayload());

    @Override
    public Type<ExportDefaultRulesPayload> type() {
        return TYPE;
    }
}
