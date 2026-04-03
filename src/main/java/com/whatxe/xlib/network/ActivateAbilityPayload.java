package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateAbilityPayload(int slot) implements CustomPacketPayload {
    public static final Type<ActivateAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "activate_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ActivateAbilityPayload::slot, ActivateAbilityPayload::new);

    @Override
    public Type<ActivateAbilityPayload> type() {
        return TYPE;
    }
}

