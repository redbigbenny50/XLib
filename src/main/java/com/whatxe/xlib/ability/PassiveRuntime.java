package com.whatxe.xlib.ability;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PassiveRuntime {
    private PassiveRuntime() {}

    public static AbilityData tick(ServerPlayer player, AbilityData currentData) {
        AbilityData updatedData = currentData;
        for (ResourceLocation passiveId : Set.copyOf(updatedData.grantedPassives())) {
            PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElse(null);
            if (passive == null) {
                updatedData = clearMissingPassive(updatedData, passiveId);
                continue;
            }

            if (passive.firstFailedActiveRequirement(player, updatedData).isPresent()) {
                continue;
            }

            updatedData = passive.tick(player, updatedData);
        }
        return updatedData;
    }

    public static AbilityData onHit(ServerPlayer player, AbilityData currentData, LivingEntity target) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onHit(player, updatedData, target));
    }

    public static AbilityData onKill(ServerPlayer player, AbilityData currentData, LivingEntity target) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onKill(player, updatedData, target));
    }

    public static AbilityData onHurt(ServerPlayer player, AbilityData currentData, DamageSource source, float amount) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onHurt(player, updatedData, source, amount));
    }

    public static AbilityData onJump(ServerPlayer player, AbilityData currentData) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onJump(player, updatedData));
    }

    public static AbilityData onEat(ServerPlayer player, AbilityData currentData, ItemStack stack) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onEat(player, updatedData, stack));
    }

    public static AbilityData onBlockBreak(ServerPlayer player, AbilityData currentData, BlockState state, BlockPos pos) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onBlockBreak(player, updatedData, state, pos));
    }

    public static AbilityData onArmorChange(
            ServerPlayer player,
            AbilityData currentData,
            EquipmentSlot slot,
            ItemStack from,
            ItemStack to
    ) {
        return forEachPassive(player, currentData, (passive, updatedData) -> passive.onArmorChange(player, updatedData, slot, from, to));
    }

    private static AbilityData forEachPassive(ServerPlayer player, AbilityData currentData, PassiveAction action) {
        AbilityData updatedData = currentData;
        for (ResourceLocation passiveId : Set.copyOf(updatedData.grantedPassives())) {
            PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElse(null);
            if (passive == null || passive.firstFailedActiveRequirement(player, updatedData).isPresent()) {
                continue;
            }
            updatedData = action.apply(passive, updatedData);
        }
        return updatedData;
    }

    private static AbilityData clearMissingPassive(AbilityData data, ResourceLocation passiveId) {
        AbilityData updatedData = data;
        for (ResourceLocation sourceId : Set.copyOf(data.passiveGrantSourcesFor(passiveId))) {
            updatedData = updatedData.withPassiveGrantSource(passiveId, sourceId, false);
        }
        return updatedData;
    }

    @FunctionalInterface
    private interface PassiveAction {
        AbilityData apply(PassiveDefinition passive, AbilityData currentData);
    }
}

