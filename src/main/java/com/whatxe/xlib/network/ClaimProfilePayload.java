package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClaimProfilePayload(ResourceLocation groupId, ResourceLocation profileId) implements CustomPacketPayload {
    public static final Type<ClaimProfilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "claim_profile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimProfilePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ClaimProfilePayload::groupId,
            ResourceLocation.STREAM_CODEC,
            ClaimProfilePayload::profileId,
            ClaimProfilePayload::new
    );

    @Override
    public Type<ClaimProfilePayload> type() {
        return TYPE;
    }
}
