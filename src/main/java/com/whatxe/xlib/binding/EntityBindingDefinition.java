package com.whatxe.xlib.binding;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record EntityBindingDefinition(
        ResourceLocation id,
        EntityBindingKind kind,
        String primaryRole,
        String secondaryRole,
        Optional<Integer> durationTicks,
        EntityBindingStackingPolicy stackingPolicy,
        EntityBindingSymmetry symmetry,
        Set<EntityBindingBreakCondition> breakConditions,
        EntityBindingCompletionMode completionMode,
        EntityBindingTickPolicy tickPolicy
) {
    public static Builder builder(ResourceLocation id, EntityBindingKind kind) {
        return new Builder(id, kind);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final EntityBindingKind kind;
        private String primaryRole = "primary";
        private String secondaryRole = "secondary";
        private Optional<Integer> durationTicks = Optional.empty();
        private EntityBindingStackingPolicy stackingPolicy = EntityBindingStackingPolicy.SINGLE;
        private EntityBindingSymmetry symmetry = EntityBindingSymmetry.DIRECTED;
        private final Set<EntityBindingBreakCondition> breakConditions = new LinkedHashSet<>();
        private EntityBindingCompletionMode completionMode = EntityBindingCompletionMode.INSTANT;
        private EntityBindingTickPolicy tickPolicy = EntityBindingTickPolicy.NONE;

        private Builder(ResourceLocation id, EntityBindingKind kind) {
            this.id = id;
            this.kind = kind;
        }

        public Builder primaryRole(String role) { this.primaryRole = role; return this; }
        public Builder secondaryRole(String role) { this.secondaryRole = role; return this; }
        public Builder durationTicks(int ticks) { this.durationTicks = Optional.of(ticks); return this; }
        public Builder stackingPolicy(EntityBindingStackingPolicy policy) { this.stackingPolicy = policy; return this; }
        public Builder symmetry(EntityBindingSymmetry symmetry) { this.symmetry = symmetry; return this; }
        public Builder breakOn(EntityBindingBreakCondition condition) { this.breakConditions.add(condition); return this; }
        public Builder completionMode(EntityBindingCompletionMode mode) { this.completionMode = mode; return this; }
        public Builder tickPolicy(EntityBindingTickPolicy policy) { this.tickPolicy = policy; return this; }

        public EntityBindingDefinition build() {
            EntityBindingTickPolicy effective = tickPolicy;
            if (effective == EntityBindingTickPolicy.NONE && durationTicks.isPresent()) {
                effective = EntityBindingTickPolicy.TICK_DURATION;
            }
            return new EntityBindingDefinition(
                    id, kind, primaryRole, secondaryRole,
                    durationTicks, stackingPolicy, symmetry,
                    Collections.unmodifiableSet(new LinkedHashSet<>(breakConditions)),
                    completionMode, effective
            );
        }
    }
}
