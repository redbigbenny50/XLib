package com.whatxe.xlib.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.whatxe.xlib.integration.recipeviewer.RecipeViewerClientRuntime;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

public final class AbilityClientEvents {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbilityClientState.tick(minecraft);
        RecipeViewerClientRuntime.syncClient(minecraft);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (isCombatBarToggleKey(event)) {
            return;
        }

        if (!AbilityClientState.isCombatBarActive(minecraft)) {
            return;
        }

        AbilityControlInputHandler.handleKeyPress(minecraft, event.getKey(), event.getModifiers());
    }

    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || !AbilityClientState.isCombatBarActive(minecraft)) {
            return;
        }
        if (AbilityControlInputHandler.handleMouseButton(minecraft, event.getButton(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (AbilityClientState.isCombatBarActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderLayerPre(RenderGuiLayerEvent.Pre event) {
        if (AbilityClientState.isCombatBarActive() && event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            event.setCanceled(true);
        }
    }

    private static boolean isCombatBarToggleKey(InputEvent.Key event) {
        return ModKeyMappings.TOGGLE_COMBAT_BAR.getKey().getType() == InputConstants.Type.KEYSYM
                && ModKeyMappings.TOGGLE_COMBAT_BAR.getKey().getValue() == event.getKey();
    }
}

