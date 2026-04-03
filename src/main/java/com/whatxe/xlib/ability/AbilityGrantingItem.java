package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface AbilityGrantingItem {
    ResourceLocation grantSourceId(ItemStack stack, Player player);

    Collection<ResourceLocation> grantedAbilities(ItemStack stack, Player player);

    default Collection<GrantCondition> grantConditions(ItemStack stack, Player player) {
        return List.of();
    }

    default Collection<ResourceLocation> grantedPassives(ItemStack stack, Player player) {
        return List.of();
    }

    default Collection<ResourceLocation> grantedItems(ItemStack stack, Player player) {
        return List.of();
    }

    default Collection<ResourceLocation> grantedRecipePermissions(ItemStack stack, Player player) {
        return List.of();
    }
}

