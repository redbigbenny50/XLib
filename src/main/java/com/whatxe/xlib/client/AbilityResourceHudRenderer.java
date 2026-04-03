package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface AbilityResourceHudRenderer {
    void render(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            Player player,
            AbilityData data,
            AbilityResourceDefinition resource,
            AbilityResourceHudLayout layout,
            int x,
            int y
    );
}

