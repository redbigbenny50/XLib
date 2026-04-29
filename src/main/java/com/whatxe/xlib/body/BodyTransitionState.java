package com.whatxe.xlib.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record BodyTransitionState(
        UUID controllerEntityId,
        UUID currentBodyEntityId,
        Optional<UUID> originBodyEntityId,
        ResourceLocation transitionId,
        ResourceLocation sourceId,
        long startedGameTime,
        BodyTransitionStatus status
) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<BodyTransitionStatus> STATUS_CODEC =
            Codec.STRING.xmap(BodyTransitionStatus::valueOf, BodyTransitionStatus::name);

    public static final Codec<BodyTransitionState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("controller_entity_id").forGetter(BodyTransitionState::controllerEntityId),
                    UUID_CODEC.fieldOf("current_body_entity_id").forGetter(BodyTransitionState::currentBodyEntityId),
                    UUID_CODEC.optionalFieldOf("origin_body_entity_id").forGetter(BodyTransitionState::originBodyEntityId),
                    ResourceLocation.CODEC.fieldOf("transition_id").forGetter(BodyTransitionState::transitionId),
                    ResourceLocation.CODEC.fieldOf("source_id").forGetter(BodyTransitionState::sourceId),
                    Codec.LONG.fieldOf("started_game_time").forGetter(BodyTransitionState::startedGameTime),
                    STATUS_CODEC.optionalFieldOf("status", BodyTransitionStatus.ACTIVE).forGetter(BodyTransitionState::status)
            ).apply(instance, BodyTransitionState::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyTransitionState> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public BodyTransitionState withStatus(BodyTransitionStatus newStatus) {
        return new BodyTransitionState(controllerEntityId, currentBodyEntityId, originBodyEntityId,
                transitionId, sourceId, startedGameTime, newStatus);
    }

    public BodyTransitionState withCurrentBody(UUID newBodyId) {
        return new BodyTransitionState(controllerEntityId, newBodyId, originBodyEntityId,
                transitionId, sourceId, startedGameTime, status);
    }
}
