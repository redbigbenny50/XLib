package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import java.util.Locale;

public enum ProfileOnboardingTrigger {
    FIRST_LOGIN,
    RESPAWN,
    ADVANCEMENT,
    ITEM_USE,
    COMMAND;

    public static final Codec<ProfileOnboardingTrigger> CODEC = Codec.STRING.xmap(
            value -> ProfileOnboardingTrigger.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT)
    );
}
