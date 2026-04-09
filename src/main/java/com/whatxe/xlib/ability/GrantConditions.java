package com.whatxe.xlib.ability;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class GrantConditions {
    private static final GrantCondition ALWAYS = (player, data, stack) -> true;
    private static final GrantCondition NEVER = (player, data, stack) -> false;

    private GrantConditions() {}

    public static GrantCondition always() {
        return ALWAYS;
    }

    public static GrantCondition never() {
        return NEVER;
    }

    public static GrantCondition all(GrantCondition... conditions) {
        return all(Arrays.asList(conditions));
    }

    public static GrantCondition all(Collection<GrantCondition> conditions) {
        List<GrantCondition> safeConditions = List.copyOf(conditions);
        return (player, data, stack) -> {
            for (GrantCondition condition : safeConditions) {
                if (!condition.test(player, data, stack)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static GrantCondition any(GrantCondition... conditions) {
        return any(Arrays.asList(conditions));
    }

    public static GrantCondition any(Collection<GrantCondition> conditions) {
        List<GrantCondition> safeConditions = List.copyOf(conditions);
        return (player, data, stack) -> {
            for (GrantCondition condition : safeConditions) {
                if (condition.test(player, data, stack)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static GrantCondition not(GrantCondition condition) {
        return Objects.requireNonNull(condition, "condition").negate();
    }

    public static GrantCondition fromRequirement(AbilityRequirement requirement) {
        AbilityRequirement resolvedRequirement = Objects.requireNonNull(requirement, "requirement");
        return (player, data, stack) -> resolvedRequirement.validate(player, data).isEmpty();
    }

    public static GrantCondition sprinting() {
        return (player, data, stack) -> player.isSprinting();
    }

    public static GrantCondition sneaking() {
        return (player, data, stack) -> player.isCrouching();
    }

    public static GrantCondition onGround() {
        return (player, data, stack) -> player.onGround();
    }

    public static GrantCondition inWater() {
        return (player, data, stack) -> player.isInWater();
    }

    public static GrantCondition holding(ItemLike itemLike) {
        Item item = Objects.requireNonNull(itemLike, "itemLike").asItem();
        return (player, data, stack) -> player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    public static GrantCondition wearing(ItemLike itemLike) {
        Item item = Objects.requireNonNull(itemLike, "itemLike").asItem();
        return (player, data, stack) -> player.getInventory().armor.stream().anyMatch(armor -> armor.is(item));
    }

    public static GrantCondition resourceAtLeast(ResourceLocation resourceId, int amount) {
        Objects.requireNonNull(resourceId, "resourceId");
        return (player, data, stack) -> data.resourceAmount(resourceId) >= amount;
    }

    public static GrantCondition resourceAtLeastExact(ResourceLocation resourceId, double amount) {
        Objects.requireNonNull(resourceId, "resourceId");
        return (player, data, stack) -> data.resourceAmountExact(resourceId) + 1.0E-9D >= amount;
    }

    public static boolean allMatch(
            Player player,
            AbilityData data,
            ItemStack stack,
            Collection<GrantCondition> conditions
    ) {
        for (GrantCondition condition : conditions) {
            if (!condition.test(player, data, stack)) {
                return false;
            }
        }
        return true;
    }
}

