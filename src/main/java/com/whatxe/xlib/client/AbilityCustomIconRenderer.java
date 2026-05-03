package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface AbilityCustomIconRenderer {
    void render(GuiGraphics guiGraphics, Minecraft minecraft, AbilityIcon icon, int x, int y, int width, int height);
}

