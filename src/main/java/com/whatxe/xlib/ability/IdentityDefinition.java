package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record IdentityDefinition(
        ResourceLocation id,
        Set<ResourceLocation> inheritedIdentities,
        Set<ResourceLocation> grantBundles
) {
    public IdentityDefinition {
        Objects.requireNonNull(id, "id");
        inheritedIdentities = Set.copyOf(inheritedIdentities);
        grantBundles = Set.copyOf(grantBundles);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> inheritedIdentities = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantBundles = new LinkedHashSet<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder inherits(ResourceLocation identityId) {
            this.inheritedIdentities.add(Objects.requireNonNull(identityId, "identityId"));
            return this;
        }

        public Builder inherits(Collection<ResourceLocation> identityIds) {
            identityIds.stream().filter(Objects::nonNull).forEach(this.inheritedIdentities::add);
            return this;
        }

        public Builder grantBundle(ResourceLocation bundleId) {
            this.grantBundles.add(Objects.requireNonNull(bundleId, "bundleId"));
            return this;
        }

        public Builder grantBundles(Collection<ResourceLocation> bundleIds) {
            bundleIds.stream().filter(Objects::nonNull).forEach(this.grantBundles::add);
            return this;
        }

        public IdentityDefinition build() {
            return new IdentityDefinition(this.id, this.inheritedIdentities, this.grantBundles);
        }
    }
}
