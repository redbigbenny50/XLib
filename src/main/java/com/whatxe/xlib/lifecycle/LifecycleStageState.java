package com.whatxe.xlib.lifecycle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record LifecycleStageState(
        ResourceLocation currentStageId,
        ResourceLocation sourceId,
        long enteredGameTime,
        int elapsedTicks,
        Optional<PendingLifecycleTransition> pendingTransition,
        LifecycleStageStatus status
) {
    private static final Codec<LifecycleStageStatus> STATUS_CODEC =
            Codec.STRING.xmap(LifecycleStageStatus::valueOf, LifecycleStageStatus::name);

    private static final Codec<LifecycleStageTrigger> TRIGGER_CODEC =
            Codec.STRING.xmap(LifecycleStageTrigger::valueOf, LifecycleStageTrigger::name);

    private static final Codec<PendingLifecycleTransition> PENDING_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("target_stage_id").forGetter(PendingLifecycleTransition::targetStageId),
                    TRIGGER_CODEC.fieldOf("trigger").forGetter(PendingLifecycleTransition::trigger),
                    Codec.LONG.fieldOf("requested_game_time").forGetter(PendingLifecycleTransition::requestedGameTime)
            ).apply(instance, PendingLifecycleTransition::new)
    );

    public static final Codec<LifecycleStageState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("current_stage_id").forGetter(LifecycleStageState::currentStageId),
                    ResourceLocation.CODEC.fieldOf("source_id").forGetter(LifecycleStageState::sourceId),
                    Codec.LONG.fieldOf("entered_game_time").forGetter(LifecycleStageState::enteredGameTime),
                    Codec.INT.optionalFieldOf("elapsed_ticks", 0).forGetter(LifecycleStageState::elapsedTicks),
                    PENDING_CODEC.optionalFieldOf("pending_transition").forGetter(LifecycleStageState::pendingTransition),
                    STATUS_CODEC.optionalFieldOf("status", LifecycleStageStatus.ACTIVE).forGetter(LifecycleStageState::status)
            ).apply(instance, LifecycleStageState::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LifecycleStageState> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public LifecycleStageState withElapsed(int ticks) {
        return new LifecycleStageState(currentStageId, sourceId, enteredGameTime, ticks, pendingTransition, status);
    }

    public LifecycleStageState withPending(PendingLifecycleTransition pending) {
        return new LifecycleStageState(currentStageId, sourceId, enteredGameTime, elapsedTicks,
                Optional.ofNullable(pending), LifecycleStageStatus.PENDING_TRANSITION);
    }

    public LifecycleStageState clearPending() {
        return new LifecycleStageState(currentStageId, sourceId, enteredGameTime, elapsedTicks,
                Optional.empty(), LifecycleStageStatus.ACTIVE);
    }
}
