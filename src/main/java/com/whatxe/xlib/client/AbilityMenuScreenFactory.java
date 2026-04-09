package com.whatxe.xlib.client;

import net.minecraft.client.gui.screens.Screen;

@FunctionalInterface
public interface AbilityMenuScreenFactory {
    Screen create(AbilityMenuScreenContext context);
}
