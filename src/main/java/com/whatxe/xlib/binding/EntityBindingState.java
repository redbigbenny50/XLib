package com.whatxe.xlib.binding;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record EntityBindingState(
        UUID bindingInstanceId,
        ResourceLocation bindingId,
        UUID primaryEntityId,
        UUID secondaryEntityId,
        ResourceLocation sourceId,
        long startedGameTime,
        Optional<Integer> remainingTicks,
        EntityBindingStatus status,
        CompoundTag payload,
        int revision
) {
    static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<EntityBindingStatus> STATUS_CODEC =
            Codec.STRING.xmap(EntityBindingStatus::valueOf, EntityBindingStatus::name);

    static final Codec<CompoundTag> COMPOUND_TAG_CODEC = Codec.STRING.comapFlatMap(
            snbt -> {
                try {
                    return DataResult.success(TagParser.parseTag(snbt));
                } catch (CommandSyntaxException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            CompoundTag::toString
    );

    public static final Codec<EntityBindingState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("binding_instance_id").forGetter(EntityBindingState::bindingInstanceId),
                    ResourceLocation.CODEC.fieldOf("binding_id").forGetter(EntityBindingState::bindingId),
                    UUID_CODEC.fieldOf("primary_entity_id").forGetter(EntityBindingState::primaryEntityId),
                    UUID_CODEC.fieldOf("secondary_entity_id").forGetter(EntityBindingState::secondaryEntityId),
                    ResourceLocation.CODEC.fieldOf("source_id").forGetter(EntityBindingState::sourceId),
                    Codec.LONG.fieldOf("started_game_time").forGetter(EntityBindingState::startedGameTime),
                    Codec.INT.optionalFieldOf("remaining_ticks").forGetter(EntityBindingState::remainingTicks),
                    STATUS_CODEC.optionalFieldOf("status", EntityBindingStatus.ACTIVE).forGetter(EntityBindingState::status),
                    COMPOUND_TAG_CODEC.optionalFieldOf("payload", new CompoundTag()).forGetter(EntityBindingState::payload),
                    Codec.INT.optionalFieldOf("revision", 0).forGetter(EntityBindingState::revision)
            ).apply(instance, EntityBindingState::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityBindingState> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public EntityBindingState withRemainingTicks(int ticks) {
        return new EntityBindingState(bindingInstanceId, bindingId, primaryEntityId, secondaryEntityId,
                sourceId, startedGameTime, Optional.of(ticks), status, payload, revision + 1);
    }

    public EntityBindingState withStatus(EntityBindingStatus newStatus) {
        return new EntityBindingState(bindingInstanceId, bindingId, primaryEntityId, secondaryEntityId,
                sourceId, startedGameTime, remainingTicks, newStatus, payload, revision + 1);
    }

    public EntityBindingState withPayload(CompoundTag newPayload) {
        return new EntityBindingState(bindingInstanceId, bindingId, primaryEntityId, secondaryEntityId,
                sourceId, startedGameTime, remainingTicks, status, newPayload, revision + 1);
    }
}
