package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record SupportPackageDefinition(
        ResourceLocation id,
        Set<ResourceLocation> grantBundles,
        Set<ResourceLocation> requiredRelationships,
        boolean allowSelf
) {
    public SupportPackageDefinition {
        Objects.requireNonNull(id, "id");
        grantBundles = Set.copyOf(grantBundles);
        requiredRelationships = Set.copyOf(requiredRelationships);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> grantBundles = new LinkedHashSet<>();
        private final Set<ResourceLocation> requiredRelationships = new LinkedHashSet<>();
        private boolean allowSelf;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder grantBundle(ResourceLocation bundleId) {
            this.grantBundles.add(Objects.requireNonNull(bundleId, "bundleId"));
            return this;
        }

        public Builder grantBundles(Collection<ResourceLocation> bundleIds) {
            bundleIds.stream().filter(Objects::nonNull).forEach(this.grantBundles::add);
            return this;
        }

        public Builder relationship(ResourceLocation relationshipId) {
            this.requiredRelationships.add(Objects.requireNonNull(relationshipId, "relationshipId"));
            return this;
        }

        public Builder relationships(Collection<ResourceLocation> relationshipIds) {
            relationshipIds.stream().filter(Objects::nonNull).forEach(this.requiredRelationships::add);
            return this;
        }

        public Builder allowSelf() {
            this.allowSelf = true;
            return this;
        }

        public SupportPackageDefinition build() {
            return new SupportPackageDefinition(this.id, this.grantBundles, this.requiredRelationships, this.allowSelf);
        }
    }
}
