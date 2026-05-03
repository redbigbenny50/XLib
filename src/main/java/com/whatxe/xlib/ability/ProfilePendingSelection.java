package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ProfilePendingSelection(
        ProfileOnboardingTrigger trigger,
        String reason
) {
    public static final Codec<ProfilePendingSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ProfileOnboardingTrigger.CODEC.optionalFieldOf("trigger", ProfileOnboardingTrigger.COMMAND)
                    .forGetter(ProfilePendingSelection::trigger),
            Codec.STRING.optionalFieldOf("reason", "").forGetter(ProfilePendingSelection::reason)
    ).apply(instance, ProfilePendingSelection::new));

    public ProfilePendingSelection {
        trigger = trigger == null ? ProfileOnboardingTrigger.COMMAND : trigger;
        reason = reason == null ? "" : reason.trim();
    }
}
