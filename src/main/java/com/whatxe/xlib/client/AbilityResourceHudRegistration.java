package com.whatxe.xlib.client;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public record AbilityResourceHudRegistration(
        @Nullable AbilityResourceHudRenderer renderer,
        AbilityResourceHudLayout layout
) {
    public AbilityResourceHudRegistration {
        layout = Objects.requireNonNull(layout, "layout");
    }
}

