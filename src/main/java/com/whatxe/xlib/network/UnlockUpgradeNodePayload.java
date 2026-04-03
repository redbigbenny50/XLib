package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockUpgradeNodePayload(ResourceLocation nodeId) implements CustomPacketPayload {
    public static final Type<UnlockUpgradeNodePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "unlock_upgrade_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockUpgradeNodePayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, UnlockUpgradeNodePayload::nodeId, UnlockUpgradeNodePayload::new);

    @Override
    public Type<UnlockUpgradeNodePayload> type() {
        return TYPE;
    }
}
