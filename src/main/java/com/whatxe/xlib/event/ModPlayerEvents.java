package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.GrantedItemRuntime;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.ability.PassiveRuntime;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = XLib.MODID)
public final class ModPlayerEvents {
    private ModPlayerEvents() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModAttachments.set(player, player.getData(ModAttachments.PLAYER_ABILITY_DATA));
            ModAttachments.setProgression(player, player.getData(ModAttachments.PLAYER_UPGRADE_PROGRESS));
            RecipePermissionApi.installCraftingGuards(player);
            GrantedItemRuntime.installStorageGuards(player);
            GrantedItemRuntime.reconcile(player);
            RecipePermissionApi.syncServerState(player);
            UpgradeApi.syncServerState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        RecipePermissionApi.installCraftingGuards(player);
        GrantedItemRuntime.installStorageGuards(player);
        GrantedItemRuntime.restoreMissingUndroppableItems(player);
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RecipePermissionApi.guardCraftingMenu(player, event.getContainer());
            GrantedItemRuntime.guardStorageMenu(player, event.getContainer());
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RecipePermissionApi.syncServerState(player);
        }
    }

    @SubscribeEvent
    public static void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RecipePermissionApi.syncServerState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = AbilityRuntime.tick(player, currentData);
        updatedData = PassiveRuntime.tick(player, updatedData);
        updatedData = GrantedItemRuntime.tick(player, updatedData);
        RecipePermissionApi.enforceOpenCraftingResult(player, updatedData);

        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
            RecipePermissionApi.syncServerState(player);
        }
    }
}

