package com.whatxe.xlib;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.binding.EntityBindingApi;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.form.VisualFormApi;
import com.whatxe.xlib.lifecycle.LifecycleStageApi;
import com.whatxe.xlib.ability.AbilityDetectorApi;
import com.whatxe.xlib.ability.ArtifactApi;
import com.whatxe.xlib.ability.AbilityLoadoutFeatureApi;
import com.whatxe.xlib.ability.AbilityControlProfileApi;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import com.whatxe.xlib.ability.AbilitySlotMigrationApi;
import com.whatxe.xlib.ability.ComboChainApi;
import com.whatxe.xlib.ability.ContextGrantApi;
import com.whatxe.xlib.ability.SupportPackageApi;
import com.whatxe.xlib.ability.GrantBundleApi;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ReactiveTriggerApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.ability.StateFlagApi;
import com.whatxe.xlib.ability.StatePolicyApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.client.AbilityContainerLayoutApi;
import com.whatxe.xlib.combat.CombatMarkApi;
import com.whatxe.xlib.combat.CombatReactionApi;
import com.whatxe.xlib.cue.XLibCueRouteProfileApi;
import com.whatxe.xlib.command.AbilityGrantCommands;
import com.whatxe.xlib.network.ModPayloads;
import com.whatxe.xlib.presentation.AbilityMenuPresentationApi;
import com.whatxe.xlib.presentation.CombatHudPresentationApi;
import com.whatxe.xlib.presentation.ProgressionMenuPresentationApi;
import com.whatxe.xlib.progression.UpgradeApi;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class AbilityLibrary {
    private AbilityLibrary() {}

    public static void bootstrap(IEventBus modEventBus) {
        AbilityApi.bootstrap();
        EntityBindingApi.bootstrap();
        LifecycleStageApi.bootstrap();
        VisualFormApi.bootstrap();
        CapabilityPolicyApi.bootstrap();
        AbilityDetectorApi.bootstrap();
        AbilityLoadoutFeatureApi.bootstrap();
        AbilityControlProfileApi.bootstrap();
        AbilitySlotContainerApi.bootstrap();
        AbilitySlotMigrationApi.bootstrap();
        AbilityContainerLayoutApi.bootstrap();
        ReactiveTriggerApi.bootstrap();
        ComboChainApi.bootstrap();
        ContextGrantApi.bootstrap();
        SupportPackageApi.bootstrap();
        ArtifactApi.bootstrap();
        GrantBundleApi.bootstrap();
        IdentityApi.bootstrap();
        ProfileApi.bootstrap();
        CombatMarkApi.bootstrap();
        CombatReactionApi.bootstrap();
        XLibCueRouteProfileApi.bootstrap();
        AbilityMenuPresentationApi.bootstrap();
        ProgressionMenuPresentationApi.bootstrap();
        CombatHudPresentationApi.bootstrap();
        PassiveApi.bootstrap();
        GrantedItemApi.bootstrap();
        ModeApi.bootstrap();
        RecipePermissionApi.bootstrap();
        StatePolicyApi.bootstrap();
        StateFlagApi.bootstrap();
        UpgradeApi.bootstrap();
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(AbilityLibrary::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(AbilityGrantCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(AbilityLibrary::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(AbilityLibrary::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(AbilityLibrary::onDatapackSync);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        XLibRegistryGuard.freeze("common setup");
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        XLibRegistryGuard.freeze("server startup");
    }

    private static void onAddReloadListener(AddReloadListenerEvent event) {
        RecipePermissionApi.onAddReloadListener(event);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        RecipePermissionApi.onDatapackSync(event);
    }
}

