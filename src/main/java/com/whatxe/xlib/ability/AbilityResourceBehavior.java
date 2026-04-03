package com.whatxe.xlib.ability;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;

public interface AbilityResourceBehavior {
    default AbilityData onTick(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource) {
        return data;
    }

    default AbilityData onKill(ServerPlayer player, LivingEntity target, AbilityData data, AbilityResourceDefinition resource) {
        return data;
    }

    default AbilityData onEat(ServerPlayer player, ItemStack stack, AbilityData data, AbilityResourceDefinition resource) {
        return data;
    }

    default AbilityData onIncomingDamage(
            ServerPlayer player,
            DamageSource source,
            float amount,
            AbilityData data,
            AbilityResourceDefinition resource
    ) {
        return data;
    }

    default AbilityData onBiome(
            ServerPlayer player,
            Holder<Biome> biome,
            AbilityData data,
            AbilityResourceDefinition resource
    ) {
        return data;
    }
}

