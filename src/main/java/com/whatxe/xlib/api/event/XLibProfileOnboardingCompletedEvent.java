package com.whatxe.xlib.api.event;

import com.whatxe.xlib.ability.ProfileOnboardingTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class XLibProfileOnboardingCompletedEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation groupId;
    private final ResourceLocation profileId;
    private final ProfileOnboardingTrigger trigger;
    private final String reason;

    public XLibProfileOnboardingCompletedEvent(
            ServerPlayer player,
            ResourceLocation groupId,
            ResourceLocation profileId,
            ProfileOnboardingTrigger trigger,
            String reason
    ) {
        this.player = player;
        this.groupId = groupId;
        this.profileId = profileId;
        this.trigger = trigger;
        this.reason = reason;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public ResourceLocation groupId() {
        return this.groupId;
    }

    public ResourceLocation profileId() {
        return this.profileId;
    }

    public ProfileOnboardingTrigger trigger() {
        return this.trigger;
    }

    public String reason() {
        return this.reason;
    }
}
