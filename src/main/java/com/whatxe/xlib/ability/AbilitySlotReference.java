package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record AbilitySlotReference(ResourceLocation containerId, int pageIndex, int slotIndex) {
    public static final Codec<AbilitySlotReference> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("container_id").forGetter(AbilitySlotReference::containerId),
            Codec.INT.optionalFieldOf("page", 0).forGetter(AbilitySlotReference::pageIndex),
            Codec.INT.fieldOf("slot").forGetter(AbilitySlotReference::slotIndex)
    ).apply(instance, AbilitySlotReference::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilitySlotReference> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            AbilitySlotReference::containerId,
            ByteBufCodecs.VAR_INT,
            AbilitySlotReference::pageIndex,
            ByteBufCodecs.VAR_INT,
            AbilitySlotReference::slotIndex,
            AbilitySlotReference::new
    );

    public AbilitySlotReference {
        Objects.requireNonNull(containerId, "containerId");
        if (pageIndex < 0) {
            throw new IllegalArgumentException("pageIndex cannot be negative");
        }
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative");
        }
    }

    public static AbilitySlotReference primary(int slotIndex) {
        return new AbilitySlotReference(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, 0, slotIndex);
    }

    public boolean isPrimaryContainer() {
        return this.containerId.equals(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID);
    }
}
