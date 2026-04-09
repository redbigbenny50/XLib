package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ModeAbilityDefinition {
    private final AbilityDefinition ability;
    private final ModeDefinition mode;

    private ModeAbilityDefinition(AbilityDefinition ability, ModeDefinition mode) {
        this.ability = ability;
        this.mode = mode;
    }

    public static Builder builder(ResourceLocation id, Item iconItem) {
        return new Builder(id, AbilityIcon.ofItem(iconItem));
    }

    public static Builder builder(ResourceLocation id, AbilityIcon icon) {
        return new Builder(id, icon);
    }

    public ResourceLocation id() {
        return this.ability.id();
    }

    public AbilityDefinition ability() {
        return this.ability;
    }

    public ModeDefinition mode() {
        return this.mode;
    }

    public ModeAbilityDefinition register() {
        return ModeApi.registerModeAbility(this);
    }

    public static final class Builder {
        private final AbilityDefinition.Builder abilityBuilder;
        private final ModeDefinition.Builder modeBuilder;

        private Builder(ResourceLocation id, AbilityIcon icon) {
            this.abilityBuilder = AbilityDefinition.builder(id, icon).toggleAbility();
            this.modeBuilder = ModeDefinition.builder(id);
        }

        public Builder cooldownTicks(int cooldownTicks) {
            this.abilityBuilder.cooldownTicks(cooldownTicks);
            return this;
        }

        public Builder cooldownPolicy(AbilityCooldownPolicy cooldownPolicy) {
            this.abilityBuilder.cooldownPolicy(cooldownPolicy);
            return this;
        }

        public Builder durationTicks(int durationTicks) {
            this.abilityBuilder.durationTicks(durationTicks);
            return this;
        }

        public Builder charges(int maxCharges, int chargeRechargeTicks) {
            this.abilityBuilder.charges(maxCharges, chargeRechargeTicks);
            return this;
        }

        public Builder family(ResourceLocation familyId) {
            this.abilityBuilder.family(familyId);
            this.modeBuilder.family(familyId);
            return this;
        }

        public Builder group(ResourceLocation groupId) {
            this.abilityBuilder.group(groupId);
            this.modeBuilder.group(groupId);
            return this;
        }

        public Builder page(ResourceLocation pageId) {
            this.abilityBuilder.page(pageId);
            this.modeBuilder.page(pageId);
            return this;
        }

        public Builder tag(ResourceLocation tagId) {
            this.abilityBuilder.tag(tagId);
            this.modeBuilder.tag(tagId);
            return this;
        }

        public Builder tags(Collection<ResourceLocation> tagIds) {
            this.abilityBuilder.tags(tagIds);
            this.modeBuilder.tags(tagIds);
            return this;
        }

        public Builder assignRequirement(AbilityRequirement requirement) {
            this.abilityBuilder.assignRequirement(requirement);
            return this;
        }

        public Builder activateRequirement(AbilityRequirement requirement) {
            this.abilityBuilder.activateRequirement(requirement);
            return this;
        }

        public Builder stayActiveRequirement(AbilityRequirement requirement) {
            this.abilityBuilder.stayActiveRequirement(requirement);
            return this;
        }

        public Builder renderRequirement(AbilityRequirement requirement) {
            this.abilityBuilder.renderRequirement(requirement);
            return this;
        }

        public Builder requirement(AbilityRequirement requirement) {
            this.abilityBuilder.requirement(requirement);
            return this;
        }

        public Builder resourceCost(ResourceLocation resourceId, int amount) {
            this.abilityBuilder.resourceCost(resourceId, amount);
            return this;
        }

        public Builder sound(AbilitySoundTrigger trigger, ResourceLocation soundId) {
            this.abilityBuilder.sound(trigger, soundId);
            return this;
        }

        public Builder sound(AbilitySoundTrigger trigger, AbilitySound sound) {
            this.abilityBuilder.sound(trigger, sound);
            return this;
        }

        public Builder action(AbilityDefinition.AbilityAction action) {
            this.abilityBuilder.action(action);
            return this;
        }

        public Builder ticker(AbilityDefinition.AbilityTicker ticker) {
            this.abilityBuilder.ticker(ticker);
            return this;
        }

        public Builder ender(AbilityDefinition.AbilityEnder ender) {
            this.abilityBuilder.ender(ender);
            return this;
        }

        public Builder priority(int priority) {
            this.modeBuilder.priority(priority);
            return this;
        }

        public Builder stackable() {
            this.modeBuilder.stackable();
            return this;
        }

        public Builder overlayMode() {
            this.modeBuilder.overlayMode();
            return this;
        }

        public Builder cycleGroup(ResourceLocation groupId) {
            this.modeBuilder.cycleGroup(groupId);
            return this;
        }

        public Builder orderedCycle(ResourceLocation groupId, int cycleOrder) {
            this.modeBuilder.orderedCycle(groupId, cycleOrder);
            return this;
        }

        public Builder cycleOrder(int cycleOrder) {
            this.modeBuilder.cycleOrder(cycleOrder);
            return this;
        }

        public Builder resetCycleGroupOnActivate(ResourceLocation groupId) {
            this.modeBuilder.resetCycleGroupOnActivate(groupId);
            return this;
        }

        public Builder resetOwnCycleGroupOnActivate() {
            this.modeBuilder.resetOwnCycleGroupOnActivate();
            return this;
        }

        public Builder cooldownTickRateMultiplier(double multiplier) {
            this.modeBuilder.cooldownTickRateMultiplier(multiplier);
            return this;
        }

        public Builder healthCostPerTick(double amount) {
            this.modeBuilder.healthCostPerTick(amount);
            return this;
        }

        public Builder minimumHealth(double minimumHealth) {
            this.modeBuilder.minimumHealth(minimumHealth);
            return this;
        }

        public Builder resourceDeltaPerTick(ResourceLocation resourceId, double amount) {
            this.modeBuilder.resourceDeltaPerTick(resourceId, amount);
            return this;
        }

        public Builder exclusiveWith(ResourceLocation modeId) {
            this.modeBuilder.exclusiveWith(modeId);
            return this;
        }

        public Builder exclusiveWith(Collection<ResourceLocation> modeIds) {
            this.modeBuilder.exclusiveWith(modeIds);
            return this;
        }

        public Builder blockedByMode(ResourceLocation modeId) {
            this.modeBuilder.blockedByMode(modeId);
            return this;
        }

        public Builder blockedByModes(Collection<ResourceLocation> modeIds) {
            this.modeBuilder.blockedByModes(modeIds);
            return this;
        }

        public Builder transformsFrom(ResourceLocation modeId) {
            this.modeBuilder.transformsFrom(modeId);
            return this;
        }

        public Builder transformsFrom(Collection<ResourceLocation> modeIds) {
            this.modeBuilder.transformsFrom(modeIds);
            return this;
        }

        public Builder overlayAbility(int slot, ResourceLocation abilityId) {
            this.modeBuilder.overlayAbility(slot, abilityId);
            return this;
        }

        public Builder grantAbility(ResourceLocation abilityId) {
            this.modeBuilder.grantAbility(abilityId);
            return this;
        }

        public Builder grantAbilities(Collection<ResourceLocation> abilityIds) {
            this.modeBuilder.grantAbilities(abilityIds);
            return this;
        }

        public Builder grantPassive(ResourceLocation passiveId) {
            this.modeBuilder.grantPassive(passiveId);
            return this;
        }

        public Builder grantPassives(Collection<ResourceLocation> passiveIds) {
            this.modeBuilder.grantPassives(passiveIds);
            return this;
        }

        public Builder grantGrantedItem(ResourceLocation grantedItemId) {
            this.modeBuilder.grantGrantedItem(grantedItemId);
            return this;
        }

        public Builder grantGrantedItems(Collection<ResourceLocation> grantedItemIds) {
            this.modeBuilder.grantGrantedItems(grantedItemIds);
            return this;
        }

        public Builder grantRecipePermission(ResourceLocation recipeId) {
            this.modeBuilder.grantRecipePermission(recipeId);
            return this;
        }

        public Builder grantRecipePermissions(Collection<ResourceLocation> recipeIds) {
            this.modeBuilder.grantRecipePermissions(recipeIds);
            return this;
        }

        public Builder blockAbility(ResourceLocation abilityId) {
            this.modeBuilder.blockAbility(abilityId);
            return this;
        }

        public Builder blockAbilities(Collection<ResourceLocation> abilityIds) {
            this.modeBuilder.blockAbilities(abilityIds);
            return this;
        }

        public Builder statePolicy(ResourceLocation statePolicyId) {
            this.modeBuilder.statePolicy(statePolicyId);
            return this;
        }

        public Builder statePolicies(Collection<ResourceLocation> statePolicyIds) {
            this.modeBuilder.statePolicies(statePolicyIds);
            return this;
        }

        public Builder stateFlag(ResourceLocation stateFlagId) {
            this.modeBuilder.stateFlag(stateFlagId);
            return this;
        }

        public Builder stateFlags(Collection<ResourceLocation> stateFlagIds) {
            this.modeBuilder.stateFlags(stateFlagIds);
            return this;
        }

        public ModeAbilityDefinition build() {
            AbilityDefinition ability = this.abilityBuilder.build();
            ModeDefinition mode = this.modeBuilder.build();
            if (!Objects.equals(ability.id(), mode.abilityId())) {
                throw new IllegalStateException("ModeAbilityDefinition requires matching ability and mode ids");
            }
            if (!ability.toggleAbility()) {
                throw new IllegalStateException("ModeAbilityDefinition requires a toggle ability");
            }
            return new ModeAbilityDefinition(ability, mode);
        }

        public ModeAbilityDefinition register() {
            return build().register();
        }
    }
}
