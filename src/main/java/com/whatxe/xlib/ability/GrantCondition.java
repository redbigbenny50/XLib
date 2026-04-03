package com.whatxe.xlib.ability;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface GrantCondition {
    boolean test(Player player, AbilityData data, ItemStack stack);
}

