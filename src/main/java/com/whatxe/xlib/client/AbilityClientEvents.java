package com.whatxe.xlib.client;

import com.whatxe.xlib.integration.recipeviewer.RecipeViewerClientRuntime;
import com.whatxe.xlib.network.ActivateAbilityPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
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

        if (isAltKey(event.getKey())) {
            AbilityClientState.toggleCombatBar(minecraft);
            return;
        }

        if (!AbilityClientState.isCombatBarActive(minecraft)) {
            return;
        }

        int slot = slotFromKey(event.getKey());
        if (slot >= 0) {
            PacketDistributor.sendToServer(new ActivateAbilityPayload(slot));
            AbilityClientState.flashSlot(slot);
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

    private static boolean isAltKey(int key) {
        return key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static int slotFromKey(int key) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            return key - GLFW.GLFW_KEY_1;
        }
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_9) {
            return key - GLFW.GLFW_KEY_KP_1;
        }
        return -1;
    }
}

