package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ContextGrantCondition {
    boolean matches(ServerPlayer player, AbilityData currentData);

    default ContextGrantCondition and(ContextGrantCondition other) {
        Objects.requireNonNull(other, "other");
        return (player, currentData) -> this.matches(player, currentData) && other.matches(player, currentData);
    }

    default ContextGrantCondition or(ContextGrantCondition other) {
        Objects.requireNonNull(other, "other");
        return (player, currentData) -> this.matches(player, currentData) || other.matches(player, currentData);
    }

    default ContextGrantCondition negate() {
        return (player, currentData) -> !this.matches(player, currentData);
    }
}
