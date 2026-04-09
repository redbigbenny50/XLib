package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class AbilitySequenceDefinition {
    @FunctionalInterface
    public interface SequenceStartAction {
        AbilityUseResult start(@Nullable ServerPlayer player, AbilityData data, AbilitySequenceState firstStage);
    }

    private static final SequenceStartAction DEFAULT_START = (player, data, state) -> AbilityUseResult.success(data);

    private final List<AbilitySequenceStage> stages;
    private final int totalDurationTicks;
    private final SequenceStartAction startAction;

    private AbilitySequenceDefinition(List<AbilitySequenceStage> stages, SequenceStartAction startAction) {
        this.stages = List.copyOf(stages);
        this.totalDurationTicks = this.stages.stream().mapToInt(AbilitySequenceStage::durationTicks).sum();
        this.startAction = startAction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<AbilitySequenceStage> stages() {
        return this.stages;
    }

    public int totalDurationTicks() {
        return this.totalDurationTicks;
    }

    AbilityUseResult activate(ResourceLocation abilityId, @Nullable ServerPlayer player, AbilityData data) {
        AbilitySequenceState firstState = stateForElapsed(abilityId, 0);
        AbilityUseResult startResult = this.startAction.start(player, data, firstState);
        if (!startResult.consumed()) {
            return startResult;
        }
        AbilityData updatedData = this.stages.getFirst().onEnter().apply(player, startResult.data(), firstState);
        return AbilityUseResult.success(updatedData, startResult.feedback());
    }

    AbilityData tick(ResourceLocation abilityId, @Nullable ServerPlayer player, AbilityData data) {
        int remainingTicks = Math.max(1, data.activeDurationFor(abilityId));
        int elapsedTicks = Math.max(0, this.totalDurationTicks - remainingTicks);
        AbilitySequenceState currentState = stateForElapsed(abilityId, elapsedTicks);
        AbilityData updatedData = data;

        if (elapsedTicks > 0) {
            AbilitySequenceState previousState = stateForElapsed(abilityId, elapsedTicks - 1);
            if (previousState.stageIndex() != currentState.stageIndex()) {
                updatedData = this.stages.get(previousState.stageIndex()).onComplete().apply(player, updatedData, previousState);
                updatedData = this.stages.get(currentState.stageIndex()).onEnter().apply(player, updatedData, currentState);
            }
        }

        return this.stages.get(currentState.stageIndex()).onTick().apply(player, updatedData, currentState);
    }

    AbilityUseResult end(ResourceLocation abilityId, @Nullable ServerPlayer player, AbilityData data, AbilityEndReason reason) {
        int remainingTicks = Math.max(1, data.activeDurationFor(abilityId));
        int elapsedTicks = Math.max(0, Math.min(this.totalDurationTicks - 1, this.totalDurationTicks - remainingTicks));
        AbilitySequenceState currentState = stateForElapsed(abilityId, elapsedTicks);
        AbilityData updatedData = this.stages.get(currentState.stageIndex()).onEnd().apply(player, data, currentState, reason);
        return AbilityUseResult.success(updatedData);
    }

    private AbilitySequenceState stateForElapsed(ResourceLocation abilityId, int elapsedTicks) {
        int boundedElapsed = Math.max(0, Math.min(elapsedTicks, this.totalDurationTicks - 1));
        int traversed = 0;
        for (int index = 0; index < this.stages.size(); index++) {
            AbilitySequenceStage stage = this.stages.get(index);
            int stageStart = traversed;
            int stageEndExclusive = traversed + stage.durationTicks();
            if (boundedElapsed < stageEndExclusive) {
                int stageElapsed = boundedElapsed - stageStart;
                int totalRemaining = this.totalDurationTicks - boundedElapsed;
                return new AbilitySequenceState(
                        abilityId,
                        index,
                        this.stages.size(),
                        stageElapsed,
                        Math.max(1, stage.durationTicks() - stageElapsed),
                        stage.durationTicks(),
                        boundedElapsed,
                        totalRemaining,
                        this.totalDurationTicks
                );
            }
            traversed = stageEndExclusive;
        }

        AbilitySequenceStage lastStage = this.stages.getLast();
        return new AbilitySequenceState(
                abilityId,
                this.stages.size() - 1,
                this.stages.size(),
                Math.max(0, lastStage.durationTicks() - 1),
                1,
                lastStage.durationTicks(),
                this.totalDurationTicks - 1,
                1,
                this.totalDurationTicks
        );
    }

    public static final class Builder {
        private final List<AbilitySequenceStage> stages = new ArrayList<>();
        private SequenceStartAction startAction = DEFAULT_START;

        private Builder() {}

        public Builder startAction(SequenceStartAction startAction) {
            this.startAction = Objects.requireNonNull(startAction, "startAction");
            return this;
        }

        public Builder stage(AbilitySequenceStage stage) {
            this.stages.add(Objects.requireNonNull(stage, "stage"));
            return this;
        }

        public Builder stages(Collection<AbilitySequenceStage> stages) {
            stages.stream().filter(Objects::nonNull).forEach(this.stages::add);
            return this;
        }

        public AbilitySequenceDefinition build() {
            if (this.stages.isEmpty()) {
                throw new IllegalStateException("Sequences require at least one stage");
            }
            return new AbilitySequenceDefinition(this.stages, this.startAction);
        }
    }
}
