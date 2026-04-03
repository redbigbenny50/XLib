package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface AbilityUnlockItem {
    ResourceLocation unlockSourceId(ItemStack stack, Player player);

    Collection<ResourceLocation> unlockedAbilities(ItemStack stack, Player player);

    default Collection<GrantCondition> unlockConditions(ItemStack stack, Player player) {
        return List.of();
    }

    default Collection<ResourceLocation> unlockedPassives(ItemStack stack, Player player) {
        return List.of();
    }

    default Collection<ResourceLocation> unlockedGrantedItems(ItemStack stack, Player player) {
        return List.of();
    }

    default Collection<ResourceLocation> unlockedRecipePermissions(ItemStack stack, Player player) {
        return List.of();
    }
}

