package com.whatxe.xlib.event;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.GrantedItemRuntime;
import com.whatxe.xlib.ability.AbilityRuntime;
import com.whatxe.xlib.ability.PassiveRuntime;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.body.BodyTransitionApi;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.form.VisualFormApi;
import com.whatxe.xlib.lifecycle.LifecycleStageApi;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.value.TrackedValueApi;
import com.whatxe.xlib.value.DataDrivenTrackedValueRuleApi;
import com.whatxe.xlib.value.TrackedValueData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
            ModAttachments.setProfiles(player, player.getData(ModAttachments.PLAYER_PROFILE_SELECTIONS));
            ModAttachments.setCapabilityPolicy(player, player.getData(ModAttachments.PLAYER_CAPABILITY_POLICY));
            ModAttachments.setTrackedValues(player, player.getData(ModAttachments.PLAYER_TRACKED_VALUES));
            player.setData(ModAttachments.PLAYER_LIFECYCLE_STAGE, LifecycleStageApi.sanitize(player.getData(ModAttachments.PLAYER_LIFECYCLE_STAGE)));
            player.setData(ModAttachments.PLAYER_VISUAL_FORM, VisualFormApi.sanitize(player.getData(ModAttachments.PLAYER_VISUAL_FORM)));
            player.setData(ModAttachments.PLAYER_BODY_TRANSITION, BodyTransitionApi.sanitize(player.getData(ModAttachments.PLAYER_BODY_TRANSITION)));
            RecipePermissionApi.installCraftingGuards(player);
            GrantedItemRuntime.installStorageGuards(player);
            GrantedItemRuntime.reconcile(player);
            RecipePermissionApi.syncServerState(player);
            UpgradeApi.syncServerState(player);
            ProfileApi.rebuild(player);
            if (!ProfileApi.get(player).firstLoginHandled()) {
                ProfileApi.evaluateOnboarding(player, com.whatxe.xlib.ability.ProfileOnboardingTrigger.FIRST_LOGIN, "first_login");
            }
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
        ModAttachments.set(player, player.getData(ModAttachments.PLAYER_ABILITY_DATA));
        ModAttachments.setProfiles(player, player.getData(ModAttachments.PLAYER_PROFILE_SELECTIONS));
        ModAttachments.setCapabilityPolicy(player, player.getData(ModAttachments.PLAYER_CAPABILITY_POLICY));
        ModAttachments.setTrackedValues(player, player.getData(ModAttachments.PLAYER_TRACKED_VALUES));
        player.setData(ModAttachments.PLAYER_LIFECYCLE_STAGE, LifecycleStageApi.sanitize(player.getData(ModAttachments.PLAYER_LIFECYCLE_STAGE)));
        player.setData(ModAttachments.PLAYER_VISUAL_FORM, VisualFormApi.sanitize(player.getData(ModAttachments.PLAYER_VISUAL_FORM)));
        ProfileApi.rebuild(player);
        ProfileApi.evaluateOnboarding(player, com.whatxe.xlib.ability.ProfileOnboardingTrigger.RESPAWN, "respawn");
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
            ProfileApi.evaluateOnboarding(player, com.whatxe.xlib.ability.ProfileOnboardingTrigger.ADVANCEMENT, "advancement");
            DataDrivenTrackedValueRuleApi.dispatchAdvancementEarned(player, event.getAdvancement().id());
        }
    }

    @SubscribeEvent
    public static void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RecipePermissionApi.syncServerState(player);
            ProfileApi.evaluateOnboarding(player, com.whatxe.xlib.ability.ProfileOnboardingTrigger.ADVANCEMENT, "advancement");
            DataDrivenTrackedValueRuleApi.dispatchAdvancementProgress(player, event.getAdvancement().id());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            ProfileApi.evaluateOnboarding(player, com.whatxe.xlib.ability.ProfileOnboardingTrigger.ITEM_USE, "item_use");
            DataDrivenTrackedValueRuleApi.dispatchItemUsed(player, event.getItemStack());
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
        TrackedValueData currentTrackedValues = ModAttachments.getTrackedValues(player);
        TrackedValueData updatedTrackedValues = TrackedValueApi.tick(currentTrackedValues);
        if (!updatedTrackedValues.equals(currentTrackedValues)) {
            ModAttachments.setTrackedValues(player, updatedTrackedValues);
        }
        DataDrivenTrackedValueRuleApi.dispatchTick(player);
        TrackedValueApi.tickFoodReplacementSurvival(player);
        TrackedValueApi.applyVanillaFoodSuppression(player);

        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
            RecipePermissionApi.syncServerState(player);
        }
    }
}

