package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record AbilityControlAction(
        AbilityControlActionType type,
        @Nullable ResourceLocation containerId,
        int slotIndex,
        int pageIndex
) {
    public AbilityControlAction {
        Objects.requireNonNull(type, "type");
        if (slotIndex < -1) {
            throw new IllegalArgumentException("slotIndex cannot be less than -1");
        }
        if (pageIndex < -1) {
            throw new IllegalArgumentException("pageIndex cannot be less than -1");
        }
    }

    public static AbilityControlAction activateSlot(ResourceLocation containerId, int slotIndex) {
        return new AbilityControlAction(AbilityControlActionType.ACTIVATE_SLOT, Objects.requireNonNull(containerId, "containerId"), slotIndex, -1);
    }

    public static AbilityControlAction nextPage(ResourceLocation containerId) {
        return new AbilityControlAction(AbilityControlActionType.NEXT_PAGE, Objects.requireNonNull(containerId, "containerId"), -1, -1);
    }

    public static AbilityControlAction previousPage(ResourceLocation containerId) {
        return new AbilityControlAction(AbilityControlActionType.PREVIOUS_PAGE, Objects.requireNonNull(containerId, "containerId"), -1, -1);
    }

    public static AbilityControlAction setPage(ResourceLocation containerId, int pageIndex) {
        return new AbilityControlAction(AbilityControlActionType.SET_PAGE, Objects.requireNonNull(containerId, "containerId"), -1, pageIndex);
    }

    public static AbilityControlAction openSelector(@Nullable ResourceLocation containerId) {
        return new AbilityControlAction(AbilityControlActionType.OPEN_SELECTOR, containerId, -1, -1);
    }

    public static AbilityControlAction confirmSelector(@Nullable ResourceLocation containerId) {
        return new AbilityControlAction(AbilityControlActionType.CONFIRM_SELECTOR, containerId, -1, -1);
    }
}
