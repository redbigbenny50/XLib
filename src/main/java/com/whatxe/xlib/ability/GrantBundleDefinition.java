package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record GrantBundleDefinition(
        ResourceLocation id,
        Set<ResourceLocation> abilities,
        Set<ResourceLocation> passives,
        Set<ResourceLocation> grantedItems,
        Set<ResourceLocation> recipePermissions,
        Set<ResourceLocation> blockedAbilities,
        Set<ResourceLocation> statePolicies,
        Set<ResourceLocation> stateFlags
) {
    public GrantBundleDefinition {
        Objects.requireNonNull(id, "id");
        abilities = Set.copyOf(abilities);
        passives = Set.copyOf(passives);
        grantedItems = Set.copyOf(grantedItems);
        recipePermissions = Set.copyOf(recipePermissions);
        blockedAbilities = Set.copyOf(blockedAbilities);
        statePolicies = Set.copyOf(statePolicies);
        stateFlags = Set.copyOf(stateFlags);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public boolean isEmpty() {
        return this.abilities.isEmpty()
                && this.passives.isEmpty()
                && this.grantedItems.isEmpty()
                && this.recipePermissions.isEmpty()
                && this.blockedAbilities.isEmpty()
                && this.statePolicies.isEmpty()
                && this.stateFlags.isEmpty();
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> abilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> passives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedAbilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> statePolicies = new LinkedHashSet<>();
        private final Set<ResourceLocation> stateFlags = new LinkedHashSet<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder grantAbility(ResourceLocation abilityId) {
            this.abilities.add(Objects.requireNonNull(abilityId, "abilityId"));
            return this;
        }

        public Builder grantAbilities(Collection<ResourceLocation> abilityIds) {
            abilityIds.stream().filter(Objects::nonNull).forEach(this.abilities::add);
            return this;
        }

        public Builder grantPassive(ResourceLocation passiveId) {
            this.passives.add(Objects.requireNonNull(passiveId, "passiveId"));
            return this;
        }

        public Builder grantPassives(Collection<ResourceLocation> passiveIds) {
            passiveIds.stream().filter(Objects::nonNull).forEach(this.passives::add);
            return this;
        }

        public Builder grantGrantedItem(ResourceLocation grantedItemId) {
            this.grantedItems.add(Objects.requireNonNull(grantedItemId, "grantedItemId"));
            return this;
        }

        public Builder grantGrantedItems(Collection<ResourceLocation> grantedItemIds) {
            grantedItemIds.stream().filter(Objects::nonNull).forEach(this.grantedItems::add);
            return this;
        }

        public Builder grantRecipePermission(ResourceLocation recipeId) {
            this.recipePermissions.add(Objects.requireNonNull(recipeId, "recipeId"));
            return this;
        }

        public Builder grantRecipePermissions(Collection<ResourceLocation> recipeIds) {
            recipeIds.stream().filter(Objects::nonNull).forEach(this.recipePermissions::add);
            return this;
        }

        public Builder blockAbility(ResourceLocation abilityId) {
            this.blockedAbilities.add(Objects.requireNonNull(abilityId, "abilityId"));
            return this;
        }

        public Builder blockAbilities(Collection<ResourceLocation> abilityIds) {
            abilityIds.stream().filter(Objects::nonNull).forEach(this.blockedAbilities::add);
            return this;
        }

        public Builder statePolicy(ResourceLocation statePolicyId) {
            this.statePolicies.add(Objects.requireNonNull(statePolicyId, "statePolicyId"));
            return this;
        }

        public Builder statePolicies(Collection<ResourceLocation> statePolicyIds) {
            statePolicyIds.stream().filter(Objects::nonNull).forEach(this.statePolicies::add);
            return this;
        }

        public Builder stateFlag(ResourceLocation stateFlagId) {
            this.stateFlags.add(Objects.requireNonNull(stateFlagId, "stateFlagId"));
            return this;
        }

        public Builder stateFlags(Collection<ResourceLocation> stateFlagIds) {
            stateFlagIds.stream().filter(Objects::nonNull).forEach(this.stateFlags::add);
            return this;
        }

        public GrantBundleDefinition build() {
            return new GrantBundleDefinition(
                    this.id,
                    this.abilities,
                    this.passives,
                    this.grantedItems,
                    this.recipePermissions,
                    this.blockedAbilities,
                    this.statePolicies,
                    this.stateFlags
            );
        }
    }
}
