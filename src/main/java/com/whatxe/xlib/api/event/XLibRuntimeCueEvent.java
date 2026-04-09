package com.whatxe.xlib.api.event;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.cue.XLibRuntimeCue;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

public final class XLibRuntimeCueEvent extends Event {
    private final @Nullable ServerPlayer player;
    private final AbilityData data;
    private final XLibRuntimeCue cue;

    public XLibRuntimeCueEvent(@Nullable ServerPlayer player, AbilityData data, XLibRuntimeCue cue) {
        this.player = player;
        this.data = data;
        this.cue = cue;
    }

    public @Nullable ServerPlayer player() {
        return this.player;
    }

    public AbilityData data() {
        return this.data;
    }

    public XLibRuntimeCue cue() {
        return this.cue;
    }
}
