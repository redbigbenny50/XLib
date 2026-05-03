package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilitySlotReference;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateAbilityPayload(AbilitySlotReference slotReference) implements CustomPacketPayload {
    public static final Type<ActivateAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "activate_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> STREAM_CODEC =
            StreamCodec.composite(AbilitySlotReference.STREAM_CODEC, ActivateAbilityPayload::slotReference, ActivateAbilityPayload::new);

    public ActivateAbilityPayload(int slot) {
        this(AbilitySlotReference.primary(slot));
    }

    @Override
    public Type<ActivateAbilityPayload> type() {
        return TYPE;
    }
}

