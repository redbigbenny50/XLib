package com.whatxe.xlib.network;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilitySlotReference;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssignAbilityPayload(AbilitySlotReference slotReference, Optional<ResourceLocation> abilityId, Optional<ResourceLocation> modeId)
        implements CustomPacketPayload {
    public static final Type<AssignAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "assign_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssignAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            AbilitySlotReference.STREAM_CODEC,
            AssignAbilityPayload::slotReference,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
            AssignAbilityPayload::abilityId,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
            AssignAbilityPayload::modeId,
            AssignAbilityPayload::new
    );

    public AssignAbilityPayload(int slot, Optional<ResourceLocation> abilityId, Optional<ResourceLocation> modeId) {
        this(AbilitySlotReference.primary(slot), abilityId, modeId);
    }

    @Override
    public Type<AssignAbilityPayload> type() {
        return TYPE;
    }
}

