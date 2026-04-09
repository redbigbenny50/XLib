package com.whatxe.xlib.ability;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface AbilityLoadoutFeaturePolicy {
    AbilityLoadoutFeatureDecision evaluate(@Nullable Player player, AbilityData data);
}
