package com.whatxe.xlib.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record AbilityMenuSessionState(
        int selectedSlot,
        @Nullable ResourceLocation editingModeId
) {
    public static final int DEFAULT_SELECTED_SLOT = 0;
    private static final AbilityMenuSessionState DEFAULT_STATE = new AbilityMenuSessionState(DEFAULT_SELECTED_SLOT, null);

    public static AbilityMenuSessionState defaultState() {
        return DEFAULT_STATE;
    }

    public static AbilityMenuSessionState of(int selectedSlot, @Nullable ResourceLocation editingModeId) {
        return new AbilityMenuSessionState(Math.max(0, selectedSlot), editingModeId);
    }
}
