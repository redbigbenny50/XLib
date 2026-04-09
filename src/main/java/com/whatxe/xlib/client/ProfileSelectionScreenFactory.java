package com.whatxe.xlib.client;

import net.minecraft.client.gui.screens.Screen;

@FunctionalInterface
public interface ProfileSelectionScreenFactory {
    Screen create(ProfileSelectionScreenContext context);
}
