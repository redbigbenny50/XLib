package com.whatxe.xlib.progression;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class UpgradeRewardBundle {
    private final Set<ResourceLocation> abilities;
    private final Set<ResourceLocation> passives;
    private final Set<ResourceLocation> grantedItems;
    private final Set<ResourceLocation> recipePermissions;

    private UpgradeRewardBundle(
            Set<ResourceLocation> abilities,
            Set<ResourceLocation> passives,
            Set<ResourceLocation> grantedItems,
            Set<ResourceLocation> recipePermissions
    ) {
        this.abilities = Set.copyOf(abilities);
        this.passives = Set.copyOf(passives);
        this.grantedItems = Set.copyOf(grantedItems);
        this.recipePermissions = Set.copyOf(recipePermissions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<ResourceLocation> abilities() {
        return this.abilities;
    }

    public Set<ResourceLocation> passives() {
        return this.passives;
    }

    public Set<ResourceLocation> grantedItems() {
        return this.grantedItems;
    }

    public Set<ResourceLocation> recipePermissions() {
        return this.recipePermissions;
    }

    public static final class Builder {
        private final Set<ResourceLocation> abilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> passives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> recipePermissions = new LinkedHashSet<>();

        private Builder() {}

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

        public UpgradeRewardBundle build() {
            return new UpgradeRewardBundle(this.abilities, this.passives, this.grantedItems, this.recipePermissions);
        }
    }
}
