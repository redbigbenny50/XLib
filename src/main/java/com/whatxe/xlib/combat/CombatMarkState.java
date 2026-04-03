package com.whatxe.xlib.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record CombatMarkState(int durationTicks, int stacks, double value, @Nullable ResourceLocation sourceId) {
    public static final Codec<CombatMarkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("duration_ticks", 0).forGetter(CombatMarkState::durationTicks),
            Codec.INT.optionalFieldOf("stacks", 1).forGetter(CombatMarkState::stacks),
            Codec.DOUBLE.optionalFieldOf("value", 0.0D).forGetter(CombatMarkState::value),
            ResourceLocation.CODEC.optionalFieldOf("source_id").forGetter(state -> java.util.Optional.ofNullable(state.sourceId()))
    ).apply(instance, (durationTicks, stacks, value, sourceId) -> new CombatMarkState(durationTicks, stacks, value, sourceId.orElse(null))));

    public CombatMarkState {
        durationTicks = Math.max(0, durationTicks);
        stacks = Math.max(1, stacks);
    }

    public CombatMarkState ticked() {
        return new CombatMarkState(Math.max(0, this.durationTicks - 1), this.stacks, this.value, this.sourceId);
    }

    public CombatMarkState withDuration(int nextDurationTicks) {
        return new CombatMarkState(nextDurationTicks, this.stacks, this.value, this.sourceId);
    }

    public CombatMarkState withStacks(int nextStacks) {
        return new CombatMarkState(this.durationTicks, nextStacks, this.value, this.sourceId);
    }

    public CombatMarkState withValue(double nextValue) {
        return new CombatMarkState(this.durationTicks, this.stacks, nextValue, this.sourceId);
    }

    public CombatMarkState withSource(@Nullable ResourceLocation nextSourceId) {
        return new CombatMarkState(this.durationTicks, this.stacks, this.value, nextSourceId);
    }
}
