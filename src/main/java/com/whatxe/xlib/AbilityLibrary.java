package com.whatxe.xlib;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.ComboChainApi;
import com.whatxe.xlib.ability.ContextGrantApi;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.RecipePermissionApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.combat.CombatMarkApi;
import com.whatxe.xlib.command.AbilityGrantCommands;
import com.whatxe.xlib.dev.XLibDevContent;
import com.whatxe.xlib.network.ModPayloads;
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
        ComboChainApi.bootstrap();
        ContextGrantApi.bootstrap();
        CombatMarkApi.bootstrap();
        PassiveApi.bootstrap();
        GrantedItemApi.bootstrap();
        ModeApi.bootstrap();
        RecipePermissionApi.bootstrap();
        UpgradeApi.bootstrap();
        XLibDevContent.registerIfNeeded();
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

