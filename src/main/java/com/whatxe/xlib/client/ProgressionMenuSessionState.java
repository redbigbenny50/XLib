package com.whatxe.xlib.client;

import com.whatxe.xlib.presentation.ProgressionNodeLayoutMode;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ProgressionMenuSessionState(
        @Nullable ResourceLocation selectedTrackId,
        @Nullable ResourceLocation selectedNodeId,
        ProgressionNodeLayoutMode layoutMode
) {
    private static final ProgressionMenuSessionState DEFAULT_STATE =
            new ProgressionMenuSessionState(null, null, ProgressionNodeLayoutMode.TREE);

    public ProgressionMenuSessionState {
        Objects.requireNonNull(layoutMode, "layoutMode");
    }

    public static ProgressionMenuSessionState defaultState() {
        return DEFAULT_STATE;
    }
}
