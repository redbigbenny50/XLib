package com.whatxe.xlib.api.event;

import com.whatxe.xlib.cue.XLibRuntimeCue;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

public final class XLibClientRuntimeCueEvent extends Event {
    private final @Nullable Entity entity;
    private final XLibRuntimeCue cue;

    public XLibClientRuntimeCueEvent(@Nullable Entity entity, XLibRuntimeCue cue) {
        this.entity = entity;
        this.cue = cue;
    }

    public @Nullable Entity entity() {
        return this.entity;
    }

    public XLibRuntimeCue cue() {
        return this.cue;
    }
}
