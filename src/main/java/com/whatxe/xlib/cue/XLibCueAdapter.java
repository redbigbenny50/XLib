package com.whatxe.xlib.cue;

import com.whatxe.xlib.ability.AbilityData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public interface XLibCueAdapter {
    default boolean supports(XLibRuntimeCue cue) {
        return true;
    }

    void onCue(@Nullable ServerPlayer player, AbilityData data, XLibRuntimeCue cue, XLibCueSurface surface);
}
