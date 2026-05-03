package com.whatxe.xlib.client;

import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
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

    public void registerCombatHudRenderer(ResourceLocation rendererId, CombatHudRenderer renderer) {
        CombatHudRendererApi.register(rendererId, renderer);
    }

    public void registerAbilityMenuScreen(ResourceLocation screenId, Supplier<Screen> factory) {
        AbilityMenuScreenFactoryApi.register(screenId, factory);
    }

    public void registerAbilityMenuScreen(ResourceLocation screenId, AbilityMenuScreenFactory factory) {
        AbilityMenuScreenFactoryApi.register(screenId, factory);
    }

    public void registerProgressionMenuScreen(ResourceLocation screenId, Supplier<Screen> factory) {
        ProgressionMenuScreenFactoryApi.register(screenId, factory);
    }

    public void registerProgressionMenuScreen(ResourceLocation screenId, ProgressionMenuScreenFactory factory) {
        ProgressionMenuScreenFactoryApi.register(screenId, factory);
    }

    public void registerProfileSelectionScreen(ResourceLocation screenId, Supplier<Screen> factory) {
        ProfileSelectionScreenFactoryApi.register(screenId, factory);
    }

    public void registerProfileSelectionScreen(ResourceLocation screenId, ProfileSelectionScreenFactory factory) {
        ProfileSelectionScreenFactoryApi.register(screenId, factory);
    }

    public void registerLoadoutQuickSwitchHandler(ResourceLocation handlerId, AbilityLoadoutQuickSwitchHandler handler) {
        AbilityLoadoutQuickSwitchApi.register(handlerId, handler);
    }

    public void registerControlKeyMapping(AbilityControlKeyMappingDefinition definition) {
        AbilityControlKeyMappingApi.register(definition);
    }

    public void registerControlActionHandler(
            com.whatxe.xlib.ability.AbilityControlActionType actionType,
            AbilityControlActionHandler handler
    ) {
        AbilityControlActionHandlerApi.register(actionType, handler);
    }

    public void registerAbilityContainerLayout(AbilityContainerLayoutDefinition definition) {
        AbilityContainerLayoutApi.register(definition);
    }
}

