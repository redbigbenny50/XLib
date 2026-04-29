package com.whatxe.xlib.form;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record VisualFormDefinition(
        ResourceLocation id,
        VisualFormKind kind,
        Optional<ResourceLocation> modelProfileId,
        Optional<ResourceLocation> cueRouteProfileId,
        Optional<ResourceLocation> hudProfileId,
        FirstPersonPolicy firstPersonPolicy,
        float renderScale,
        Optional<ResourceLocation> soundProfileId
) {
    public static Builder builder(ResourceLocation id, VisualFormKind kind) {
        return new Builder(id, kind);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final VisualFormKind kind;
        private Optional<ResourceLocation> modelProfileId = Optional.empty();
        private Optional<ResourceLocation> cueRouteProfileId = Optional.empty();
        private Optional<ResourceLocation> hudProfileId = Optional.empty();
        private FirstPersonPolicy firstPersonPolicy = FirstPersonPolicy.DEFAULT;
        private float renderScale = 1.0f;
        private Optional<ResourceLocation> soundProfileId = Optional.empty();

        private Builder(ResourceLocation id, VisualFormKind kind) {
            this.id = id;
            this.kind = kind;
        }

        public Builder modelProfile(ResourceLocation id) { this.modelProfileId = Optional.of(id); return this; }
        public Builder cueRouteProfile(ResourceLocation id) { this.cueRouteProfileId = Optional.of(id); return this; }
        public Builder hudProfile(ResourceLocation id) { this.hudProfileId = Optional.of(id); return this; }
        public Builder firstPersonPolicy(FirstPersonPolicy policy) { this.firstPersonPolicy = policy; return this; }
        public Builder renderScale(float scale) { this.renderScale = scale; return this; }
        public Builder soundProfile(ResourceLocation id) { this.soundProfileId = Optional.of(id); return this; }

        public VisualFormDefinition build() {
            return new VisualFormDefinition(
                    id, kind, modelProfileId, cueRouteProfileId,
                    hudProfileId, firstPersonPolicy, renderScale, soundProfileId
            );
        }
    }
}
