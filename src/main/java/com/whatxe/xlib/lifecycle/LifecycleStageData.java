package com.whatxe.xlib.lifecycle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LifecycleStageData(
        Optional<LifecycleStageState> activeStage
) {
    public static final Codec<LifecycleStageData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    LifecycleStageState.CODEC.optionalFieldOf("active_stage")
                            .forGetter(LifecycleStageData::activeStage)
            ).apply(instance, LifecycleStageData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LifecycleStageData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static LifecycleStageData empty() {
        return new LifecycleStageData(Optional.empty());
    }

    public boolean hasStage() {
        return activeStage.isPresent();
    }

    public LifecycleStageData withStage(LifecycleStageState state) {
        return new LifecycleStageData(Optional.of(state));
    }

    public LifecycleStageData withoutStage() {
        return empty();
    }

    public LifecycleStageData updateStage(LifecycleStageState state) {
        if (activeStage.isEmpty()) return this;
        return withStage(state);
    }
}
