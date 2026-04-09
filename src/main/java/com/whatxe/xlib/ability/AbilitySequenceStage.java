package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record AbilitySequenceStage(
        String key,
        int durationTicks,
        StageAction onEnter,
        StageAction onTick,
        StageAction onComplete,
        StageEndAction onEnd
) {
    @FunctionalInterface
    public interface StageAction {
        AbilityData apply(@Nullable ServerPlayer player, AbilityData data, AbilitySequenceState state);
    }

    @FunctionalInterface
    public interface StageEndAction {
        AbilityData apply(@Nullable ServerPlayer player, AbilityData data, AbilitySequenceState state, AbilityEndReason reason);
    }

    private static final StageAction NOOP_ACTION = (player, data, state) -> data;
    private static final StageEndAction NOOP_END_ACTION = (player, data, state, reason) -> data;

    public AbilitySequenceStage {
        Objects.requireNonNull(key, "key");
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("Stage duration must be positive");
        }
        Objects.requireNonNull(onEnter, "onEnter");
        Objects.requireNonNull(onTick, "onTick");
        Objects.requireNonNull(onComplete, "onComplete");
        Objects.requireNonNull(onEnd, "onEnd");
    }

    public static Builder builder(String key, int durationTicks) {
        return new Builder(key, durationTicks);
    }

    public static final class Builder {
        private final String key;
        private final int durationTicks;
        private StageAction onEnter = NOOP_ACTION;
        private StageAction onTick = NOOP_ACTION;
        private StageAction onComplete = NOOP_ACTION;
        private StageEndAction onEnd = NOOP_END_ACTION;

        private Builder(String key, int durationTicks) {
            this.key = Objects.requireNonNull(key, "key");
            this.durationTicks = durationTicks;
        }

        public Builder onEnter(StageAction onEnter) {
            this.onEnter = Objects.requireNonNull(onEnter, "onEnter");
            return this;
        }

        public Builder onTick(StageAction onTick) {
            this.onTick = Objects.requireNonNull(onTick, "onTick");
            return this;
        }

        public Builder onComplete(StageAction onComplete) {
            this.onComplete = Objects.requireNonNull(onComplete, "onComplete");
            return this;
        }

        public Builder onEnd(StageEndAction onEnd) {
            this.onEnd = Objects.requireNonNull(onEnd, "onEnd");
            return this;
        }

        public AbilitySequenceStage build() {
            return new AbilitySequenceStage(this.key, this.durationTicks, this.onEnter, this.onTick, this.onComplete, this.onEnd);
        }
    }
}
