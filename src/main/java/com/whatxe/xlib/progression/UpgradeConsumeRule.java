package com.whatxe.xlib.progression;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.GrantCondition;
import com.whatxe.xlib.ability.GrantConditions;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class UpgradeConsumeRule {
    private final ResourceLocation id;
    private final Set<ResourceLocation> itemIds;
    private final Set<TagKey<Item>> itemTags;
    private final boolean foodOnly;
    private final List<GrantCondition> conditions;
    private final Map<ResourceLocation, Integer> pointRewards;
    private final Map<ResourceLocation, Integer> counterRewards;

    private UpgradeConsumeRule(
            ResourceLocation id,
            Set<ResourceLocation> itemIds,
            Set<TagKey<Item>> itemTags,
            boolean foodOnly,
            List<GrantCondition> conditions,
            Map<ResourceLocation, Integer> pointRewards,
            Map<ResourceLocation, Integer> counterRewards
    ) {
        this.id = id;
        this.itemIds = Set.copyOf(itemIds);
        this.itemTags = Set.copyOf(itemTags);
        this.foodOnly = foodOnly;
        this.conditions = List.copyOf(conditions);
        this.pointRewards = Map.copyOf(pointRewards);
        this.counterRewards = Map.copyOf(counterRewards);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Map<ResourceLocation, Integer> pointRewards() {
        return this.pointRewards;
    }

    public Map<ResourceLocation, Integer> counterRewards() {
        return this.counterRewards;
    }

    public boolean matches(Player player, AbilityData data, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.foodOnly && !stack.has(DataComponents.FOOD)) {
            return false;
        }

        boolean itemMatches = this.itemIds.isEmpty() && this.itemTags.isEmpty();
        if (!itemMatches) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            itemMatches = this.itemIds.contains(itemId) || this.itemTags.stream().anyMatch(stack::is);
        }
        if (!itemMatches) {
            return false;
        }

        return GrantConditions.allMatch(player, data, stack, this.conditions);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        private final Set<TagKey<Item>> itemTags = new LinkedHashSet<>();
        private boolean foodOnly;
        private final List<GrantCondition> conditions = new java.util.ArrayList<>();
        private final Map<ResourceLocation, Integer> pointRewards = new LinkedHashMap<>();
        private final Map<ResourceLocation, Integer> counterRewards = new LinkedHashMap<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder item(ItemLike itemLike) {
            this.itemIds.add(BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(itemLike, "itemLike").asItem()));
            return this;
        }

        public Builder itemId(ResourceLocation itemId) {
            this.itemIds.add(Objects.requireNonNull(itemId, "itemId"));
            return this;
        }

        public Builder itemTag(TagKey<Item> itemTag) {
            this.itemTags.add(Objects.requireNonNull(itemTag, "itemTag"));
            return this;
        }

        public Builder itemTag(ResourceLocation tagId) {
            return itemTag(TagKey.create(Registries.ITEM, Objects.requireNonNull(tagId, "tagId")));
        }

        public Builder foodOnly() {
            this.foodOnly = true;
            return this;
        }

        public Builder condition(GrantCondition condition) {
            this.conditions.add(Objects.requireNonNull(condition, "condition"));
            return this;
        }

        public Builder requirement(AbilityRequirement requirement) {
            return condition(GrantConditions.fromRequirement(requirement));
        }

        public Builder conditions(Collection<GrantCondition> conditions) {
            conditions.stream().filter(Objects::nonNull).forEach(this.conditions::add);
            return this;
        }

        public Builder requirements(Collection<AbilityRequirement> requirements) {
            requirements.stream().filter(Objects::nonNull).forEach(this::requirement);
            return this;
        }

        public Builder awardPoints(ResourceLocation pointTypeId, int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Awarded points must be positive");
            }
            this.pointRewards.put(Objects.requireNonNull(pointTypeId, "pointTypeId"), amount);
            return this;
        }

        public Builder incrementCounter(ResourceLocation counterId, int amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Counter increments must be positive");
            }
            this.counterRewards.put(Objects.requireNonNull(counterId, "counterId"), amount);
            return this;
        }

        public UpgradeConsumeRule build() {
            return new UpgradeConsumeRule(
                    this.id,
                    this.itemIds,
                    this.itemTags,
                    this.foodOnly,
                    this.conditions,
                    this.pointRewards,
                    this.counterRewards
            );
        }
    }
}
