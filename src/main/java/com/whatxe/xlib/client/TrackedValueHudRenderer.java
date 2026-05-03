package com.whatxe.xlib.client;

import com.whatxe.xlib.value.TrackedValueDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface TrackedValueHudRenderer {
    void render(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            Player player,
            TrackedValueDefinition definition,
            int currentValue,
            int maxValue,
            int x,
            int y,
            int width,
            int height
    );
}
