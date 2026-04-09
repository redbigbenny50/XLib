package com.whatxe.xlib.client;

import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public record AbilityMenuScreenContext(@Nullable Screen previousScreen, AbilityMenuSessionState sessionState) {
    public AbilityMenuScreenContext {
        Objects.requireNonNull(sessionState, "sessionState");
    }

    public static AbilityMenuScreenContext defaultContext() {
        return fromCurrentState(null);
    }

    public static AbilityMenuScreenContext fromCurrentState(@Nullable Screen previousScreen) {
        return new AbilityMenuScreenContext(previousScreen, AbilityMenuSessionStateApi.state());
    }
}
