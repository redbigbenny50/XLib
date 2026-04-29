package com.whatxe.xlib.client;

import java.util.Locale;

public enum HudOverlayAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    ABOVE_HOTBAR_LEFT,
    ABOVE_HOTBAR_CENTER,
    ABOVE_HOTBAR_RIGHT;

    public static HudOverlayAnchor parse(String rawValue) {
        return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
