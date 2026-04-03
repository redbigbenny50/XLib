package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record AbilitySound(
        ResourceLocation soundId,
        float volume,
        float pitch
) {
    public AbilitySound {
        soundId = Objects.requireNonNull(soundId, "soundId");
    }

    public static AbilitySound of(ResourceLocation soundId) {
        return new AbilitySound(soundId, 1.0F, 1.0F);
    }
}

