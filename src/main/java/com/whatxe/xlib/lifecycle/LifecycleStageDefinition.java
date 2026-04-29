package com.whatxe.xlib.lifecycle;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record LifecycleStageDefinition(
        ResourceLocation id,
        Optional<Integer> durationTicks,
        List<LifecycleStageTransition> autoTransitions,
        Set<ResourceLocation> manualTransitionTargets,
        List<ResourceLocation> projectedStateFlags,
        List<ResourceLocation> projectedGrantBundles,
        List<ResourceLocation> projectedIdentities,
        List<ResourceLocation> projectedCapabilityPolicies,
        Optional<ResourceLocation> projectedVisualForm
) {
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private Optional<Integer> durationTicks = Optional.empty();
        private final List<LifecycleStageTransition> autoTransitions = new java.util.ArrayList<>();
        private final Set<ResourceLocation> manualTransitionTargets = new LinkedHashSet<>();
        private final List<ResourceLocation> projectedStateFlags = new java.util.ArrayList<>();
        private final List<ResourceLocation> projectedGrantBundles = new java.util.ArrayList<>();
        private final List<ResourceLocation> projectedIdentities = new java.util.ArrayList<>();
        private final List<ResourceLocation> projectedCapabilityPolicies = new java.util.ArrayList<>();
        private Optional<ResourceLocation> projectedVisualForm = Optional.empty();

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder durationTicks(int ticks) { this.durationTicks = Optional.of(ticks); return this; }
        public Builder autoTransition(LifecycleStageTransition transition) { this.autoTransitions.add(transition); return this; }
        public Builder allowTransitionTo(ResourceLocation stageId) { this.manualTransitionTargets.add(stageId); return this; }
        public Builder projectStateFlag(ResourceLocation flagId) { this.projectedStateFlags.add(flagId); return this; }
        public Builder projectGrantBundle(ResourceLocation bundleId) { this.projectedGrantBundles.add(bundleId); return this; }
        public Builder projectIdentity(ResourceLocation identityId) { this.projectedIdentities.add(identityId); return this; }
        public Builder projectCapabilityPolicy(ResourceLocation policyId) { this.projectedCapabilityPolicies.add(policyId); return this; }
        public Builder projectVisualForm(ResourceLocation formId) { this.projectedVisualForm = Optional.of(formId); return this; }

        public LifecycleStageDefinition build() {
            return new LifecycleStageDefinition(
                    id, durationTicks,
                    List.copyOf(autoTransitions),
                    Collections.unmodifiableSet(new LinkedHashSet<>(manualTransitionTargets)),
                    List.copyOf(projectedStateFlags),
                    List.copyOf(projectedGrantBundles),
                    List.copyOf(projectedIdentities),
                    List.copyOf(projectedCapabilityPolicies),
                    projectedVisualForm
            );
        }
    }
}
