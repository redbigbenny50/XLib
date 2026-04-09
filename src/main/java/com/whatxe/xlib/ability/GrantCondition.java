package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface GrantCondition {
    boolean test(Player player, AbilityData data, ItemStack stack);

    default GrantCondition and(GrantCondition other) {
        Objects.requireNonNull(other, "other");
        return (player, data, stack) -> this.test(player, data, stack) && other.test(player, data, stack);
    }

    default GrantCondition or(GrantCondition other) {
        Objects.requireNonNull(other, "other");
        return (player, data, stack) -> this.test(player, data, stack) || other.test(player, data, stack);
    }

    default GrantCondition negate() {
        return (player, data, stack) -> !this.test(player, data, stack);
    }
}

