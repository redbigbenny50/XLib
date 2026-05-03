package com.whatxe.xlib.ability;

import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface ContextGrantProvider {
    ResourceLocation id();

    Collection<ContextGrantSnapshot> collect(ServerPlayer player, AbilityData currentData);
}
