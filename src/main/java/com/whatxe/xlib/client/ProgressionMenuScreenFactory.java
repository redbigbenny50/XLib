package com.whatxe.xlib.client;

import net.minecraft.client.gui.screens.Screen;

@FunctionalInterface
public interface ProgressionMenuScreenFactory {
    Screen create(ProgressionMenuScreenContext context);
}
