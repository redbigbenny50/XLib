package com.whatxe.xlib.client;

import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public record ProgressionMenuScreenContext(@Nullable Screen previousScreen, ProgressionMenuSessionState sessionState) {
    public ProgressionMenuScreenContext {
        Objects.requireNonNull(sessionState, "sessionState");
    }

    public static ProgressionMenuScreenContext defaultContext() {
        return fromCurrentState(null);
    }

    public static ProgressionMenuScreenContext fromCurrentState(@Nullable Screen previousScreen) {
        return new ProgressionMenuScreenContext(previousScreen, ProgressionMenuSessionStateApi.state());
    }
}
