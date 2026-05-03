package com.whatxe.xlib.ability;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public final class ContextGrantConditions {
    private static final ContextGrantCondition ALWAYS = (player, currentData) -> true;
    private static final ContextGrantCondition NEVER = (player, currentData) -> false;

    private ContextGrantConditions() {}

    public static ContextGrantCondition always() {
        return ALWAYS;
    }

    public static ContextGrantCondition never() {
        return NEVER;
    }

    public static ContextGrantCondition all(ContextGrantCondition... conditions) {
        return all(Arrays.asList(conditions));
    }

    public static ContextGrantCondition all(Collection<ContextGrantCondition> conditions) {
        Collection<ContextGrantCondition> safeConditions = List.copyOf(conditions);
        return (player, currentData) -> {
            for (ContextGrantCondition condition : safeConditions) {
                if (condition != null && !condition.matches(player, currentData)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static ContextGrantCondition any(ContextGrantCondition... conditions) {
        return any(Arrays.asList(conditions));
    }

    public static ContextGrantCondition any(Collection<ContextGrantCondition> conditions) {
        Collection<ContextGrantCondition> safeConditions = List.copyOf(conditions);
        return (player, currentData) -> {
            for (ContextGrantCondition condition : safeConditions) {
                if (condition != null && condition.matches(player, currentData)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static ContextGrantCondition not(ContextGrantCondition condition) {
        return Objects.requireNonNull(condition, "condition").negate();
    }

    public static ContextGrantCondition fromRequirement(AbilityRequirement requirement) {
        AbilityRequirement resolvedRequirement = Objects.requireNonNull(requirement, "requirement");
        return (player, currentData) -> resolvedRequirement.validate(player, currentData).isEmpty();
    }

    public static ContextGrantCondition dimension(ResourceLocation dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        return (player, currentData) -> player.level().dimension().location().equals(dimensionId);
    }

    public static ContextGrantCondition biome(ResourceLocation biomeId) {
        Objects.requireNonNull(biomeId, "biomeId");
        return (player, currentData) -> player.level()
                .getBiome(player.blockPosition())
                .unwrapKey()
                .map(ResourceKey::location)
                .filter(biomeId::equals)
                .isPresent();
    }

    public static ContextGrantCondition team(String teamName) {
        Objects.requireNonNull(teamName, "teamName");
        return (player, currentData) -> player.getTeam() != null && teamName.equals(player.getTeam().getName());
    }

    public static ContextGrantCondition modeActive(ResourceLocation modeAbilityId) {
        Objects.requireNonNull(modeAbilityId, "modeAbilityId");
        return (player, currentData) -> currentData.isModeActive(modeAbilityId);
    }

    public static ContextGrantCondition wearingAll(Collection<? extends ItemLike> requiredArmor) {
        List<Item> requiredItems = requiredArmor.stream()
                .filter(Objects::nonNull)
                .map(ItemLike::asItem)
                .toList();
        return (player, currentData) -> requiredItems.stream()
                .allMatch(requiredItem -> player.getInventory().armor.stream().anyMatch(stack -> stack.is(requiredItem)));
    }

    public static ContextGrantCondition hasStatusEffect(ResourceLocation effectId) {
        Objects.requireNonNull(effectId, "effectId");
        return (player, currentData) -> {
            for (MobEffectInstance activeEffect : player.getActiveEffects()) {
                ResourceLocation activeEffectId = BuiltInRegistries.MOB_EFFECT.getKey(activeEffect.getEffect().value());
                if (effectId.equals(activeEffectId)) {
                    return true;
                }
            }
            return false;
        };
    }
}
