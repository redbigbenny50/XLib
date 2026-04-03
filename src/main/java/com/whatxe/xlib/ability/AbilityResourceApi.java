package com.whatxe.xlib.ability;

import com.whatxe.xlib.attachment.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class AbilityResourceApi {
    private AbilityResourceApi() {}

    public static int get(Player player, ResourceLocation resourceId) {
        return ModAttachments.get(player).resourceAmount(resourceId);
    }

    public static double getExact(Player player, ResourceLocation resourceId) {
        return ModAttachments.get(player).resourceAmountExact(resourceId);
    }

    public static void set(Player player, ResourceLocation resourceId, int amount) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = withAmount(currentData, resourceId, amount);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    public static void setExact(Player player, ResourceLocation resourceId, double amount) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = withAmountExact(currentData, resourceId, amount);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    public static void add(Player player, ResourceLocation resourceId, int delta) {
        setExact(player, resourceId, getExact(player, resourceId) + delta);
    }

    public static void addExact(Player player, ResourceLocation resourceId, double delta) {
        setExact(player, resourceId, getExact(player, resourceId) + delta);
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
        return withAmountExact(data, resourceId, data.resourceAmountExact(resourceId) + delta);
    }

    public static AbilityData withAmountExact(AbilityData data, ResourceLocation resourceId, double amount) {
        AbilityResourceDefinition resource = AbilityApi.findResource(resourceId).orElse(null);
        if (resource == null) {
            return data;
        }

        double clampedAmount = Math.max(0.0D, Math.min(resource.totalCapacity(), amount));
        return data.withResourceAmountExact(resourceId, clampedAmount);
    }

    public static AbilityData addAmountExact(AbilityData data, ResourceLocation resourceId, double delta) {
        return withAmountExact(data, resourceId, data.resourceAmountExact(resourceId) + delta);
    }
}

