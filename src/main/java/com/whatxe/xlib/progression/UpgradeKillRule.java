package com.whatxe.xlib.progression;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class UpgradeKillRule {
    private final ResourceLocation id;
    private final Set<ResourceLocation> targetEntityIds;
    private final Set<TagKey<EntityType<?>>> targetEntityTags;
    private final ResourceLocation requiredAbilityId;
    private final Map<ResourceLocation, Integer> pointRewards;
    private final Map<ResourceLocation, Integer> counterRewards;

    private UpgradeKillRule(
            ResourceLocation id,
            Set<ResourceLocation> targetEntityIds,
            Set<TagKey<EntityType<?>>> targetEntityTags,
            ResourceLocation requiredAbilityId,
            Map<ResourceLocation, Integer> pointRewards,
            Map<ResourceLocation, Integer> counterRewards
    ) {
        this.id = id;
        this.targetEntityIds = Set.copyOf(targetEntityIds);
        this.targetEntityTags = Set.copyOf(targetEntityTags);
        this.requiredAbilityId = requiredAbilityId;
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

    public boolean matches(ServerPlayer player, LivingEntity target, Optional<ResourceLocation> attributedAbilityId) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        boolean entityMatches = this.targetEntityIds.isEmpty() && this.targetEntityTags.isEmpty();
        if (!entityMatches) {
            entityMatches = this.targetEntityIds.contains(entityTypeId)
                    || this.targetEntityTags.stream().anyMatch(target.getType()::is);
        }
        if (!entityMatches) {
            return false;
        }
        return this.requiredAbilityId == null || attributedAbilityId.filter(this.requiredAbilityId::equals).isPresent();
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> targetEntityIds = new LinkedHashSet<>();
        private final Set<TagKey<EntityType<?>>> targetEntityTags = new LinkedHashSet<>();
        private ResourceLocation requiredAbilityId;
        private final Map<ResourceLocation, Integer> pointRewards = new LinkedHashMap<>();
        private final Map<ResourceLocation, Integer> counterRewards = new LinkedHashMap<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder target(EntityType<?> entityType) {
            this.targetEntityIds.add(BuiltInRegistries.ENTITY_TYPE.getKey(Objects.requireNonNull(entityType, "entityType")));
            return this;
        }

        public Builder target(ResourceLocation entityTypeId) {
            this.targetEntityIds.add(Objects.requireNonNull(entityTypeId, "entityTypeId"));
            return this;
        }

        public Builder targets(Collection<ResourceLocation> entityTypeIds) {
            entityTypeIds.stream().filter(Objects::nonNull).forEach(this.targetEntityIds::add);
            return this;
        }

        public Builder targetTag(TagKey<EntityType<?>> tagKey) {
            this.targetEntityTags.add(Objects.requireNonNull(tagKey, "tagKey"));
            return this;
        }

        public Builder targetTag(ResourceLocation tagId) {
            return targetTag(TagKey.create(Registries.ENTITY_TYPE, Objects.requireNonNull(tagId, "tagId")));
        }

        public Builder requiredAbility(ResourceLocation abilityId) {
            this.requiredAbilityId = Objects.requireNonNull(abilityId, "abilityId");
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

        public UpgradeKillRule build() {
            return new UpgradeKillRule(
                    this.id,
                    this.targetEntityIds,
                    this.targetEntityTags,
                    this.requiredAbilityId,
                    this.pointRewards,
                    this.counterRewards
            );
        }
    }
}
