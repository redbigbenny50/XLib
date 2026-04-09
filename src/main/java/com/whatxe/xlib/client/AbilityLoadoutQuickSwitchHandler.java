package com.whatxe.xlib.client;

import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface AbilityLoadoutQuickSwitchHandler {
    boolean handle(Minecraft minecraft);
}
