package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record ProfileDefinition(
        ResourceLocation id,
        ResourceLocation groupId,
        Component displayName,
        Component description,
        AbilityIcon icon,
        Set<ResourceLocation> incompatibleProfiles,
        Set<ResourceLocation> grantBundles,
        Set<ResourceLocation> identities,
        Set<ResourceLocation> abilities,
        Set<ResourceLocation> modes,
        Set<ResourceLocation> passives,
        Set<ResourceLocation> grantedItems,
        Set<ResourceLocation> recipePermissions,
        Set<ResourceLocation> stateFlags,
        Set<ResourceLocation> unlockedArtifacts,
        Set<ResourceLocation> startingNodes
) {
    public ProfileDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(groupId, "groupId");
        displayName = displayName == null ? Component.literal(id.toString()) : displayName;
        description = description == null ? Component.empty() : description;
        Objects.requireNonNull(icon, "icon");
        incompatibleProfiles = Set.copyOf(incompatibleProfiles);
        grantBundles = Set.copyOf(grantBundles);
        identities = Set.copyOf(identities);
        abilities = Set.copyOf(abilities);
        modes = Set.copyOf(modes);
        passives = Set.copyOf(passives);
        grantedItems = Set.copyOf(grantedItems);
        recipePermissions = Set.copyOf(recipePermissions);
        stateFlags = Set.copyOf(stateFlags);
        unlockedArtifacts = Set.copyOf(unlockedArtifacts);
        startingNodes = Set.copyOf(startingNodes);
    }

    public static Builder builder(ResourceLocation id, ResourceLocation groupId, AbilityIcon icon) {
        return new Builder(id, groupId, icon);
    }

    public boolean incompatibleWith(ResourceLocation otherProfileId) {
        return this.incompatibleProfiles.contains(otherProfileId);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation groupId;
        private final AbilityIcon icon;
        private Component displayName;
        private Component description = Component.empty();
        private final Set<ResourceLocation> incompatibleProfiles = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantBundles = new LinkedHashSet<>();
        private final Set<ResourceLocation> identities = new LinkedHashSet<>();
        private final Set<ResourceLocation> abilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> modes = new LinkedHashSet<>();
        private final Set<ResourceLocation> passives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();
        private final Set<ResourceLocation> stateFlags = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockedArtifacts = new LinkedHashSet<>();
        private final Set<ResourceLocation> startingNodes = new LinkedHashSet<>();

        private Builder(ResourceLocation id, ResourceLocation groupId, AbilityIcon icon) {
            this.id = Objects.requireNonNull(id, "id");
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            this.icon = Objects.requireNonNull(icon, "icon");
        }

        public Builder displayName(Component displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder description(Component description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder incompatibleWith(ResourceLocation profileId) {
            this.incompatibleProfiles.add(Objects.requireNonNull(profileId, "profileId"));
            return this;
        }

        public Builder incompatibleWith(Collection<ResourceLocation> profileIds) {
            profileIds.stream().filter(Objects::nonNull).forEach(this.incompatibleProfiles::add);
            return this;
        }

        public Builder grantBundle(ResourceLocation bundleId) {
            this.grantBundles.add(Objects.requireNonNull(bundleId, "bundleId"));
            return this;
        }

        public Builder grantIdentity(ResourceLocation identityId) {
            this.identities.add(Objects.requireNonNull(identityId, "identityId"));
            return this;
        }

        public Builder grantAbility(ResourceLocation abilityId) {
            this.abilities.add(Objects.requireNonNull(abilityId, "abilityId"));
            return this;
        }

        public Builder grantMode(ResourceLocation modeId) {
            this.modes.add(Objects.requireNonNull(modeId, "modeId"));
            return this;
        }

        public Builder grantPassive(ResourceLocation passiveId) {
            this.passives.add(Objects.requireNonNull(passiveId, "passiveId"));
            return this;
        }

        public Builder grantItem(ResourceLocation grantedItemId) {
            this.grantedItems.add(Objects.requireNonNull(grantedItemId, "grantedItemId"));
            return this;
        }

        public Builder grantRecipePermission(ResourceLocation recipeId) {
            this.recipePermissions.add(Objects.requireNonNull(recipeId, "recipeId"));
            return this;
        }

        public Builder stateFlag(ResourceLocation stateFlagId) {
            this.stateFlags.add(Objects.requireNonNull(stateFlagId, "stateFlagId"));
            return this;
        }

        public Builder unlockArtifact(ResourceLocation artifactId) {
            this.unlockedArtifacts.add(Objects.requireNonNull(artifactId, "artifactId"));
            return this;
        }

        public Builder unlockStartingNode(ResourceLocation nodeId) {
            this.startingNodes.add(Objects.requireNonNull(nodeId, "nodeId"));
            return this;
        }

        public ProfileDefinition build() {
            return new ProfileDefinition(
                    this.id,
                    this.groupId,
                    this.displayName,
                    this.description,
                    this.icon,
                    this.incompatibleProfiles,
                    this.grantBundles,
                    this.identities,
                    this.abilities,
                    this.modes,
                    this.passives,
                    this.grantedItems,
                    this.recipePermissions,
                    this.stateFlags,
                    this.unlockedArtifacts,
                    this.startingNodes
            );
        }
    }
}
