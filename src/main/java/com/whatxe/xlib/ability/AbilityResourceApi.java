package com.whatxe.xlib.ability;

import com.whatxe.xlib.attachment.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class AbilityResourceApi {
    private AbilityResourceApi() {}

    public static int get(Player player, ResourceLocation resourceId) {
        return ModAttachments.get(player).resourceAmount(resourceId);
    }

    public static void set(Player player, ResourceLocation resourceId, int amount) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = withAmount(currentData, resourceId, amount);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    public static void add(Player player, ResourceLocation resourceId, int delta) {
        set(player, resourceId, get(player, resourceId) + delta);
    }

    public static AbilityData withAmount(AbilityData data, ResourceLocation resourceId, int amount) {
        AbilityResourceDefinition resource = AbilityApi.findResource(resourceId).orElse(null);
        if (resource == null) {
            return data;
        }

        int clampedAmount = Math.max(0, Math.min(resource.totalCapacity(), amount));
        return data.withResourceAmount(resourceId, clampedAmount);
    }

    public static AbilityData addAmount(AbilityData data, ResourceLocation resourceId, int delta) {
        return withAmount(data, resourceId, data.resourceAmount(resourceId) + delta);
    }
}

