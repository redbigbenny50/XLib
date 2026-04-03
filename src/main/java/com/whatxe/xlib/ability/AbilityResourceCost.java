package com.whatxe.xlib.ability;

import net.minecraft.resources.ResourceLocation;

public record AbilityResourceCost(ResourceLocation resourceId, int amount) {
    public AbilityResourceCost {
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}

