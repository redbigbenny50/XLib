package com.whatxe.xlib.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record BodyTransitionData(
        Optional<BodyTransitionState> activeTransition
) {
    public static final Codec<BodyTransitionData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BodyTransitionState.CODEC.optionalFieldOf("active_transition")
                            .forGetter(BodyTransitionData::activeTransition)
            ).apply(instance, BodyTransitionData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BodyTransitionData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static BodyTransitionData empty() {
        return new BodyTransitionData(Optional.empty());
    }

    public boolean hasTransition() {
        return activeTransition.isPresent();
    }

    public BodyTransitionData withTransition(BodyTransitionState state) {
        return new BodyTransitionData(Optional.of(state));
    }

    public BodyTransitionData withoutTransition() {
        return empty();
    }

    public BodyTransitionData updateTransition(BodyTransitionState state) {
        if (activeTransition.isEmpty()) return this;
        return withTransition(state);
    }
}
