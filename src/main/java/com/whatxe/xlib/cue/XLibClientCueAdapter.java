package com.whatxe.xlib.cue;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface XLibClientCueAdapter {
    default boolean supports(XLibRuntimeCue cue) {
        return true;
    }

    void onCue(@Nullable Entity entity, XLibRuntimeCue cue, XLibCueSurface surface);
}
