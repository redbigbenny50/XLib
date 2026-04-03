package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record ContextGrantSnapshot(
        ResourceLocation sourceId,
        Set<ResourceLocation> abilities,
        Set<ResourceLocation> passives,
        Set<ResourceLocation> grantedItems,
        Set<ResourceLocation> recipePermissions,
        Set<ResourceLocation> blockedAbilities
) {
    public ContextGrantSnapshot {
        Objects.requireNonNull(sourceId, "sourceId");
        abilities = Set.copyOf(abilities);
        passives = Set.copyOf(passives);
        grantedItems = Set.copyOf(grantedItems);
        recipePermissions = Set.copyOf(recipePermissions);
        blockedAbilities = Set.copyOf(blockedAbilities);
    }

    public static ContextGrantSnapshot empty(ResourceLocation sourceId) {
        return new ContextGrantSnapshot(sourceId, Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }

    public static Builder builder(ResourceLocation sourceId) {
        return new Builder(sourceId);
    }

    public boolean isEmpty() {
        return this.abilities.isEmpty()
                && this.passives.isEmpty()
                && this.grantedItems.isEmpty()
                && this.recipePermissions.isEmpty()
                && this.blockedAbilities.isEmpty();
    }

    public ContextGrantSnapshot merge(ContextGrantSnapshot other) {
        if (!this.sourceId.equals(other.sourceId)) {
            throw new IllegalArgumentException("Cannot merge snapshots with different source ids");
        }

        return ContextGrantSnapshot.builder(this.sourceId)
                .grantAbilities(this.abilities)
                .grantAbilities(other.abilities)
                .grantPassives(this.passives)
                .grantPassives(other.passives)
                .grantGrantedItems(this.grantedItems)
                .grantGrantedItems(other.grantedItems)
                .grantRecipePermissions(this.recipePermissions)
                .grantRecipePermissions(other.recipePermissions)
                .blockAbilities(this.blockedAbilities)
                .blockAbilities(other.blockedAbilities)
                .build();
    }

    public static final class Builder {
        private final ResourceLocation sourceId;
        private final Set<ResourceLocation> abilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> passives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedAbilities = new LinkedHashSet<>();

        private Builder(ResourceLocation sourceId) {
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
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

        public ContextGrantSnapshot build() {
            return new ContextGrantSnapshot(
                    this.sourceId,
                    this.abilities,
                    this.passives,
                    this.grantedItems,
                    this.recipePermissions,
                    this.blockedAbilities
            );
        }
    }
}
