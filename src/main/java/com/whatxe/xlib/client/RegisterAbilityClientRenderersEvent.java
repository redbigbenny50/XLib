package com.whatxe.xlib.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

public class RegisterAbilityClientRenderersEvent extends Event {
    public void registerResourceHud(ResourceLocation resourceId, AbilityResourceHudRenderer renderer) {
        AbilityResourceHudRegistry.register(resourceId, renderer);
    }

    public void registerResourceHud(ResourceLocation resourceId, AbilityResourceHudLayout layout) {
        AbilityResourceHudRegistry.register(resourceId, layout);
    }

    public void registerResourceHud(
            ResourceLocation resourceId,
            AbilityResourceHudRenderer renderer,
            AbilityResourceHudLayout layout
    ) {
        AbilityResourceHudRegistry.register(resourceId, renderer, layout);
    }

    public void registerCustomIconRenderer(ResourceLocation rendererId, AbilityCustomIconRenderer renderer) {
        AbilityCustomIconRegistry.register(rendererId, renderer);
    }
}

