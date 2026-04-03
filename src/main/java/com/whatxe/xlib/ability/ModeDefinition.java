package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLib;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class ModeDefinition {
    private final ResourceLocation abilityId;
    private final int priority;
    private final boolean stackable;
    private final @Nullable ResourceLocation cycleGroupId;
    private final Set<ResourceLocation> resetCycleGroupsOnActivate;
    private final double cooldownTickRateMultiplier;
    private final Set<ResourceLocation> exclusiveModes;
    private final Set<ResourceLocation> blockedByModes;
    private final Set<ResourceLocation> transformsFrom;
    private final Map<Integer, ResourceLocation> overlayAbilities;
    private final Set<ResourceLocation> grantedAbilities;
    private final Set<ResourceLocation> grantedPassives;
    private final Set<ResourceLocation> grantedItems;
    private final Set<ResourceLocation> grantedRecipes;
    private final Set<ResourceLocation> blockedAbilities;

    private ModeDefinition(
            ResourceLocation abilityId,
            int priority,
            boolean stackable,
            @Nullable ResourceLocation cycleGroupId,
            Set<ResourceLocation> resetCycleGroupsOnActivate,
            double cooldownTickRateMultiplier,
            Set<ResourceLocation> exclusiveModes,
            Set<ResourceLocation> blockedByModes,
            Set<ResourceLocation> transformsFrom,
            Map<Integer, ResourceLocation> overlayAbilities,
            Set<ResourceLocation> grantedAbilities,
            Set<ResourceLocation> grantedPassives,
            Set<ResourceLocation> grantedItems,
            Set<ResourceLocation> grantedRecipes,
            Set<ResourceLocation> blockedAbilities
    ) {
        this.abilityId = abilityId;
        this.priority = priority;
        this.stackable = stackable;
        this.cycleGroupId = cycleGroupId;
        this.resetCycleGroupsOnActivate = Set.copyOf(resetCycleGroupsOnActivate);
        this.cooldownTickRateMultiplier = cooldownTickRateMultiplier;
        this.exclusiveModes = Set.copyOf(exclusiveModes);
        this.blockedByModes = Set.copyOf(blockedByModes);
        this.transformsFrom = Set.copyOf(transformsFrom);
        this.overlayAbilities = Map.copyOf(overlayAbilities);
        this.grantedAbilities = Set.copyOf(grantedAbilities);
        this.grantedPassives = Set.copyOf(grantedPassives);
        this.grantedItems = Set.copyOf(grantedItems);
        this.grantedRecipes = Set.copyOf(grantedRecipes);
        this.blockedAbilities = Set.copyOf(blockedAbilities);
    }

    public static Builder builder(ResourceLocation abilityId) {
        return new Builder(abilityId);
    }

    public ResourceLocation abilityId() {
        return this.abilityId;
    }

    public int priority() {
        return this.priority;
    }

    public boolean stackable() {
        return this.stackable;
    }

    public @Nullable ResourceLocation cycleGroupId() {
        return this.cycleGroupId;
    }

    public Set<ResourceLocation> resetCycleGroupsOnActivate() {
        return this.resetCycleGroupsOnActivate;
    }

    public double cooldownTickRateMultiplier() {
        return this.cooldownTickRateMultiplier;
    }

    public Set<ResourceLocation> exclusiveModes() {
        return this.exclusiveModes;
    }

    public Set<ResourceLocation> blockedByModes() {
        return this.blockedByModes;
    }

    public Set<ResourceLocation> transformsFrom() {
        return this.transformsFrom;
    }

    public Map<Integer, ResourceLocation> overlayAbilities() {
        return this.overlayAbilities;
    }

    public Set<ResourceLocation> grantedAbilities() {
        return this.grantedAbilities;
    }

    public Set<ResourceLocation> grantedPassives() {
        return this.grantedPassives;
    }

    public Set<ResourceLocation> grantedItems() {
        return this.grantedItems;
    }

    public Set<ResourceLocation> grantedRecipes() {
        return this.grantedRecipes;
    }

    public Set<ResourceLocation> blockedAbilities() {
        return this.blockedAbilities;
    }

    public ResourceLocation sourceId() {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                "mode/" + this.abilityId.getNamespace() + "/" + this.abilityId.getPath()
        );
    }

    public ContextGrantSnapshot snapshot() {
        return ContextGrantSnapshot.builder(sourceId())
                .grantAbilities(this.grantedAbilities)
                .grantPassives(this.grantedPassives)
                .grantGrantedItems(this.grantedItems)
                .grantRecipePermissions(this.grantedRecipes)
                .blockAbilities(this.blockedAbilities)
                .build();
    }

    public static final class Builder {
        private final ResourceLocation abilityId;
        private int priority;
        private boolean stackable;
        private @Nullable ResourceLocation cycleGroupId;
        private final Set<ResourceLocation> resetCycleGroupsOnActivate = new LinkedHashSet<>();
        private double cooldownTickRateMultiplier = 1.0D;
        private final Set<ResourceLocation> exclusiveModes = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedByModes = new LinkedHashSet<>();
        private final Set<ResourceLocation> transformsFrom = new LinkedHashSet<>();
        private final Map<Integer, ResourceLocation> overlayAbilities = new LinkedHashMap<>();
        private final Set<ResourceLocation> grantedAbilities = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedPassives = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedItems = new LinkedHashSet<>();
        private final Set<ResourceLocation> grantedRecipes = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedAbilities = new LinkedHashSet<>();

        private Builder(ResourceLocation abilityId) {
            this.abilityId = Objects.requireNonNull(abilityId, "abilityId");
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder stackable() {
            this.stackable = true;
            return this;
        }

        public Builder overlayMode() {
            return stackable();
        }

        public Builder cycleGroup(ResourceLocation groupId) {
            this.cycleGroupId = Objects.requireNonNull(groupId, "groupId");
            return this;
        }

        public Builder resetCycleGroupOnActivate(ResourceLocation groupId) {
            this.resetCycleGroupsOnActivate.add(Objects.requireNonNull(groupId, "groupId"));
            return this;
        }

        public Builder resetOwnCycleGroupOnActivate() {
            if (this.cycleGroupId == null) {
                throw new IllegalStateException("cycleGroup must be set before resetOwnCycleGroupOnActivate()");
            }
            return resetCycleGroupOnActivate(this.cycleGroupId);
        }

        public Builder cooldownTickRateMultiplier(double multiplier) {
            if (!(multiplier > 0.0D)) {
                throw new IllegalArgumentException("cooldownTickRateMultiplier must be positive");
            }
            this.cooldownTickRateMultiplier = multiplier;
            return this;
        }

        public Builder exclusiveWith(ResourceLocation modeId) {
            this.exclusiveModes.add(Objects.requireNonNull(modeId, "modeId"));
            return this;
        }

        public Builder exclusiveWith(Collection<ResourceLocation> modeIds) {
            modeIds.stream().filter(Objects::nonNull).forEach(this.exclusiveModes::add);
            return this;
        }

        public Builder blockedByMode(ResourceLocation modeId) {
            this.blockedByModes.add(Objects.requireNonNull(modeId, "modeId"));
            return this;
        }

        public Builder blockedByModes(Collection<ResourceLocation> modeIds) {
            modeIds.stream().filter(Objects::nonNull).forEach(this.blockedByModes::add);
            return this;
        }

        public Builder transformsFrom(ResourceLocation modeId) {
            this.transformsFrom.add(Objects.requireNonNull(modeId, "modeId"));
            return this;
        }

        public Builder transformsFrom(Collection<ResourceLocation> modeIds) {
            modeIds.stream().filter(Objects::nonNull).forEach(this.transformsFrom::add);
            return this;
        }

        public Builder overlayAbility(int slot, ResourceLocation abilityId) {
            if (slot < 0 || slot >= AbilityData.SLOT_COUNT) {
                throw new IllegalArgumentException("Invalid overlay slot: " + slot);
            }
            this.overlayAbilities.put(slot, Objects.requireNonNull(abilityId, "abilityId"));
            return this;
        }

        public Builder grantAbility(ResourceLocation abilityId) {
            this.grantedAbilities.add(Objects.requireNonNull(abilityId, "abilityId"));
            return this;
        }

        public Builder grantAbilities(Collection<ResourceLocation> abilityIds) {
            abilityIds.stream().filter(Objects::nonNull).forEach(this.grantedAbilities::add);
            return this;
        }

        public Builder grantPassive(ResourceLocation passiveId) {
            this.grantedPassives.add(Objects.requireNonNull(passiveId, "passiveId"));
            return this;
        }

        public Builder grantPassives(Collection<ResourceLocation> passiveIds) {
            passiveIds.stream().filter(Objects::nonNull).forEach(this.grantedPassives::add);
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
            this.grantedRecipes.add(Objects.requireNonNull(recipeId, "recipeId"));
            return this;
        }

        public Builder grantRecipePermissions(Collection<ResourceLocation> recipeIds) {
            recipeIds.stream().filter(Objects::nonNull).forEach(this.grantedRecipes::add);
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

        public ModeDefinition build() {
            return new ModeDefinition(
                    this.abilityId,
                    this.priority,
                    this.stackable,
                    this.cycleGroupId,
                    this.resetCycleGroupsOnActivate,
                    this.cooldownTickRateMultiplier,
                    this.exclusiveModes,
                    this.blockedByModes,
                    this.transformsFrom,
                    this.overlayAbilities,
                    this.grantedAbilities,
                    this.grantedPassives,
                    this.grantedItems,
                    this.grantedRecipes,
                    this.blockedAbilities
            );
        }
    }
}
