package com.whatxe.xlib.ability;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record AbilityUseResult(AbilityData data, boolean consumed, @Nullable Component feedback) {
    public static AbilityUseResult success(AbilityData data) {
        return new AbilityUseResult(data, true, null);
    }

    public static AbilityUseResult success(AbilityData data, Component feedback) {
        return new AbilityUseResult(data, true, feedback);
    }

    public static AbilityUseResult fail(AbilityData currentData, Component feedback) {
        return new AbilityUseResult(currentData, false, feedback);
    }
}

