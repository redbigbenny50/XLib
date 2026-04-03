package com.whatxe.xlib.ability;

public enum AbilityEndReason {
    PLAYER_TOGGLED,
    DURATION_EXPIRED,
    REQUIREMENT_INVALIDATED,
    FORCE_ENDED,
    REPLACED_BY_TRANSFORM,
    REPLACED_BY_EXCLUSIVE;

    public boolean usesInterruptSound() {
        return this == REQUIREMENT_INVALIDATED || this == FORCE_ENDED;
    }
}

