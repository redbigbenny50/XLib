package com.whatxe.xlib.client;

public enum AbilityResourceHudAnchor {
    ABOVE_HOTBAR_LEFT,
    ABOVE_HOTBAR_CENTER,
    ABOVE_HOTBAR_RIGHT,
    LEFT_OF_HOTBAR,
    RIGHT_OF_HOTBAR,
    TOP_LEFT,
    TOP_RIGHT;

    public boolean aboveHotbar() {
        return this == ABOVE_HOTBAR_LEFT || this == ABOVE_HOTBAR_CENTER || this == ABOVE_HOTBAR_RIGHT;
    }

    public boolean besideHotbar() {
        return this == LEFT_OF_HOTBAR || this == RIGHT_OF_HOTBAR;
    }
}

