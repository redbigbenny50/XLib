package com.whatxe.xlib.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ProfileSelectionScreenContext(@Nullable ResourceLocation pendingGroupId) {
    public static ProfileSelectionScreenContext defaultContext() {
        return new ProfileSelectionScreenContext(null);
    }
}
