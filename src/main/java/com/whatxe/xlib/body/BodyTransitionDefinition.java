package com.whatxe.xlib.body;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record BodyTransitionDefinition(
        ResourceLocation id,
        BodyTransitionKind kind,
        OriginBodyPolicy originBodyPolicy,
        BodyControlPolicy controlPolicy,
        Optional<ResourceLocation> temporaryCapabilityPolicyId,
        Optional<ResourceLocation> temporaryVisualFormId,
        boolean reversible
) {
    public static Builder builder(ResourceLocation id, BodyTransitionKind kind) {
        return new Builder(id, kind);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final BodyTransitionKind kind;
        private OriginBodyPolicy originBodyPolicy = OriginBodyPolicy.PRESERVE;
        private BodyControlPolicy controlPolicy = BodyControlPolicy.FULL;
        private Optional<ResourceLocation> temporaryCapabilityPolicyId = Optional.empty();
        private Optional<ResourceLocation> temporaryVisualFormId = Optional.empty();
        private boolean reversible = true;

        private Builder(ResourceLocation id, BodyTransitionKind kind) {
            this.id = id;
            this.kind = kind;
        }

        public Builder originBodyPolicy(OriginBodyPolicy policy) { this.originBodyPolicy = policy; return this; }
        public Builder controlPolicy(BodyControlPolicy policy) { this.controlPolicy = policy; return this; }
        public Builder temporaryCapabilityPolicy(ResourceLocation policyId) { this.temporaryCapabilityPolicyId = Optional.of(policyId); return this; }
        public Builder temporaryVisualForm(ResourceLocation formId) { this.temporaryVisualFormId = Optional.of(formId); return this; }
        public Builder reversible(boolean reversible) { this.reversible = reversible; return this; }

        public BodyTransitionDefinition build() {
            return new BodyTransitionDefinition(
                    id, kind, originBodyPolicy, controlPolicy,
                    temporaryCapabilityPolicyId, temporaryVisualFormId, reversible
            );
        }
    }
}
