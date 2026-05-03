package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface AbilityResourceItem {
    default Collection<GrantCondition> resourceConditions(ItemStack stack, Player player) {
        return List.of();
    }

    Map<ResourceLocation, Integer> resourceChanges(ItemStack stack, Player player);
}

