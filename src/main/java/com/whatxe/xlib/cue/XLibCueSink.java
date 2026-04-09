package com.whatxe.xlib.cue;

import com.whatxe.xlib.ability.AbilityData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface XLibCueSink {
    void onCue(@Nullable ServerPlayer player, AbilityData data, XLibRuntimeCue cue);
}
