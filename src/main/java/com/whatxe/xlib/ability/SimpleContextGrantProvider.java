package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class SimpleContextGrantProvider implements ContextGrantProvider {
    private final ResourceLocation id;
    private final ContextGrantCondition condition;
    private final ContextGrantSnapshot snapshot;

    private SimpleContextGrantProvider(
            ResourceLocation id,
            ContextGrantCondition condition,
            ContextGrantSnapshot snapshot
    ) {
        this.id = id;
        this.condition = condition;
        this.snapshot = snapshot;
    }

    public static Builder builder(ResourceLocation id, ResourceLocation sourceId) {
        return new Builder(id, sourceId);
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Collection<ContextGrantSnapshot> collect(ServerPlayer player, AbilityData currentData) {
        if (!this.condition.matches(player, currentData) || this.snapshot.isEmpty()) {
            return List.of();
        }
        return List.of(this.snapshot);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation sourceId;
        private ContextGrantCondition condition = ContextGrantConditions.always();
        private final ContextGrantSnapshot.Builder snapshot;

        private Builder(ResourceLocation id, ResourceLocation sourceId) {
            this.id = Objects.requireNonNull(id, "id");
            this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
            this.snapshot = ContextGrantSnapshot.builder(sourceId);
        }

        public Builder when(ContextGrantCondition condition) {
            this.condition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Builder when(AbilityRequirement requirement) {
            return when(ContextGrantConditions.fromRequirement(requirement));
        }

        public Builder grantAbility(ResourceLocation abilityId) {
            this.snapshot.grantAbility(abilityId);
            return this;
        }

        public Builder grantAbilities(Collection<ResourceLocation> abilityIds) {
            this.snapshot.grantAbilities(abilityIds);
            return this;
        }

        public Builder grantPassive(ResourceLocation passiveId) {
            this.snapshot.grantPassive(passiveId);
            return this;
        }

        public Builder grantPassives(Collection<ResourceLocation> passiveIds) {
            this.snapshot.grantPassives(passiveIds);
            return this;
        }

        public Builder grantGrantedItem(ResourceLocation grantedItemId) {
            this.snapshot.grantGrantedItem(grantedItemId);
            return this;
        }

        public Builder grantGrantedItems(Collection<ResourceLocation> grantedItemIds) {
            this.snapshot.grantGrantedItems(grantedItemIds);
            return this;
        }

        public Builder grantRecipePermission(ResourceLocation recipeId) {
            this.snapshot.grantRecipePermission(recipeId);
            return this;
        }

        public Builder grantRecipePermissions(Collection<ResourceLocation> recipeIds) {
            this.snapshot.grantRecipePermissions(recipeIds);
            return this;
        }

        public Builder blockAbility(ResourceLocation abilityId) {
            this.snapshot.blockAbility(abilityId);
            return this;
        }

        public Builder blockAbilities(Collection<ResourceLocation> abilityIds) {
            this.snapshot.blockAbilities(abilityIds);
            return this;
        }

        public Builder grantStatePolicy(ResourceLocation statePolicyId) {
            this.snapshot.grantStatePolicy(statePolicyId);
            return this;
        }

        public Builder grantStatePolicies(Collection<ResourceLocation> statePolicyIds) {
            this.snapshot.grantStatePolicies(statePolicyIds);
            return this;
        }

        public Builder grantStateFlag(ResourceLocation stateFlagId) {
            this.snapshot.grantStateFlag(stateFlagId);
            return this;
        }

        public Builder grantStateFlags(Collection<ResourceLocation> stateFlagIds) {
            this.snapshot.grantStateFlags(stateFlagIds);
            return this;
        }

        public SimpleContextGrantProvider build() {
            return new SimpleContextGrantProvider(this.id, this.condition, this.snapshot.build());
        }
    }
}
