package com.whatxe.xlib.ability;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

public final class AbilityResourceRuntime {
    private AbilityResourceRuntime() {}

    public static AbilityData tick(ServerPlayer player, AbilityData currentData) {
        AbilityData updatedData = currentData;
        Holder<Biome> currentBiome = player.serverLevel().getBiome(player.blockPosition());
        for (AbilityResourceDefinition resource : AbilityApi.allResources()) {
            int currentAmount = updatedData.resourceAmount(resource.id());
            if (currentAmount < resource.maxAmount()) {
                updatedData = tickRegeneration(updatedData, resource);
            } else if (updatedData.resourceRegenDelay(resource.id()) > 0) {
                updatedData = updatedData.withResourceRegenDelay(resource.id(), 0);
            }

            updatedData = resource.tick(player, updatedData);
            for (AbilityResourceBehavior behavior : resource.behaviors()) {
                updatedData = behavior.onBiome(player, currentBiome, updatedData, resource);
            }
        }
        return updatedData;
    }

    public static AbilityData onKill(ServerPlayer player, AbilityData currentData, LivingEntity target) {
        AbilityData updatedData = currentData;
        for (AbilityResourceDefinition resource : AbilityApi.allResources()) {
            for (AbilityResourceBehavior behavior : resource.behaviors()) {
                updatedData = behavior.onKill(player, target, updatedData, resource);
            }
        }
        return updatedData;
    }

    public static AbilityData onEat(ServerPlayer player, AbilityData currentData, ItemStack stack) {
        AbilityData updatedData = currentData;
        for (AbilityResourceDefinition resource : AbilityApi.allResources()) {
            for (AbilityResourceBehavior behavior : resource.behaviors()) {
                updatedData = behavior.onEat(player, stack, updatedData, resource);
            }
        }
        return updatedData;
    }

    public static AbilityData onIncomingDamage(
            ServerPlayer player,
            AbilityData currentData,
            DamageSource source,
            MutableDamageAmount damageAmount
    ) {
        AbilityData updatedData = currentData;
        for (AbilityResourceDefinition resource : AbilityApi.allResources()) {
            if (damageAmount.amount() <= 0.0F) {
                break;
            }

            if (resource.shieldStyle()) {
                int availableShield = updatedData.resourceAmount(resource.id());
                if (availableShield > 0) {
                    int absorbedDamage = Math.min(availableShield, (int)Math.ceil(damageAmount.amount()));
                    if (absorbedDamage > 0) {
                        updatedData = AbilityResourceApi.addAmount(updatedData, resource.id(), -absorbedDamage);
                        updatedData = updatedData.withResourceDecayDelay(resource.id(), 20);
                        damageAmount.setAmount(Math.max(0.0F, damageAmount.amount() - absorbedDamage));
                    }
                }
            }

            for (AbilityResourceBehavior behavior : resource.behaviors()) {
                updatedData = behavior.onIncomingDamage(player, source, damageAmount.amount(), updatedData, resource);
            }
        }
        return updatedData;
    }

    private static AbilityData tickRegeneration(AbilityData data, AbilityResourceDefinition resource) {
        if (resource.regenAmount() <= 0) {
            return data;
        }

        if (resource.regenIntervalTicks() <= 1) {
            return AbilityResourceApi.withAmount(data, resource.id(), data.resourceAmount(resource.id()) + resource.regenAmount());
        }

        int regenDelay = data.resourceRegenDelay(resource.id());
        if (regenDelay > 1) {
            return data.withResourceRegenDelay(resource.id(), regenDelay - 1);
        }

        AbilityData updatedData = AbilityResourceApi.withAmount(data, resource.id(), data.resourceAmount(resource.id()) + resource.regenAmount());
        int nextAmount = updatedData.resourceAmount(resource.id());
        return updatedData.withResourceRegenDelay(
                resource.id(),
                nextAmount < resource.maxAmount() ? resource.regenIntervalTicks() : 0
        );
    }

    public static final class MutableDamageAmount {
        private float amount;

        public MutableDamageAmount(float amount) {
            this.amount = amount;
        }

        public float amount() {
            return this.amount;
        }

        public void setAmount(float amount) {
            this.amount = amount;
        }
    }
}

