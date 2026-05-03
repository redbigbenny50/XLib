package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum ProfileSelectionOrigin {
    PLAYER,
    ADMIN,
    DELEGATED,
    TEMPORARY;

    public static final Codec<ProfileSelectionOrigin> CODEC = Codec.STRING.xmap(
            value -> ProfileSelectionOrigin.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT)
    );

    public boolean isAdminSet() {
        return this == ADMIN;
    }

    public boolean isDelegated() {
        return this == DELEGATED;
    }

    public boolean isTemporary() {
        return this == TEMPORARY;
    }
}
