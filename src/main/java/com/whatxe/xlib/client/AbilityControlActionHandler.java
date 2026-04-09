package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityControlAction;
import com.whatxe.xlib.ability.AbilityData;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface AbilityControlActionHandler {
    boolean handle(Minecraft minecraft, AbilityData data, AbilityControlAction action);
}
