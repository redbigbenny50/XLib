package com.whatxe.xlib;

import com.whatxe.xlib.client.AbilityClientEvents;
import com.whatxe.xlib.client.AbilityControlActionHandlerApi;
import com.whatxe.xlib.client.AbilityMenuScreenFactoryApi;
import com.whatxe.xlib.client.CombatBarOverlay;
import com.whatxe.xlib.client.CombatBarPreferences;
import com.whatxe.xlib.client.CombatHudRendererApi;
import com.whatxe.xlib.client.ModKeyMappings;
import com.whatxe.xlib.client.ProfileSelectionScreenFactoryApi;
import com.whatxe.xlib.client.ProgressionMenuScreenFactoryApi;
import com.whatxe.xlib.client.RegisterAbilityClientRenderersEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class AbilityLibraryClient {
    private AbilityLibraryClient() {}

    public static void bootstrap(IEventBus modEventBus) {
        modEventBus.addListener(ModKeyMappings::register);
        modEventBus.addListener(CombatBarOverlay::registerLayer);
        modEventBus.addListener(AbilityLibraryClient::onClientSetup);
        NeoForge.EVENT_BUS.register(new AbilityClientEvents());
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            AbilityMenuScreenFactoryApi.bootstrap();
            ProgressionMenuScreenFactoryApi.bootstrap();
            ProfileSelectionScreenFactoryApi.bootstrap();
            CombatHudRendererApi.bootstrap();
            AbilityControlActionHandlerApi.bootstrap();
            CombatBarPreferences.load();
            NeoForge.EVENT_BUS.post(new RegisterAbilityClientRenderersEvent());
        });
    }
}

