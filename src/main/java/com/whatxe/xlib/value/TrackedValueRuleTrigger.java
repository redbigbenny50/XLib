package com.whatxe.xlib.value;

import java.util.Locale;

public enum TrackedValueRuleTrigger {
    TICK,
    DAMAGE_DEALT,
    DAMAGE_TAKEN,
    KILL,
    JUMP,
    ARMOR_CHANGED,
    ITEM_USED,
    ITEM_CONSUMED,
    BLOCK_BROKEN,
    ADVANCEMENT_EARNED,
    ADVANCEMENT_PROGRESS;

    public static TrackedValueRuleTrigger parse(String rawValue) {
        return valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
