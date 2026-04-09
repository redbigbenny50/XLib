package com.whatxe.xlib.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class XLibProfileResetEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation groupId;
    private final boolean admin;
    private final String reason;
    private final int resetCount;
    private final boolean onboardingReopened;

    public XLibProfileResetEvent(
            ServerPlayer player,
            ResourceLocation groupId,
            boolean admin,
            String reason,
            int resetCount,
            boolean onboardingReopened
    ) {
        this.player = player;
        this.groupId = groupId;
        this.admin = admin;
        this.reason = reason;
        this.resetCount = resetCount;
        this.onboardingReopened = onboardingReopened;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public ResourceLocation groupId() {
        return this.groupId;
    }

    public boolean admin() {
        return this.admin;
    }

    public String reason() {
        return this.reason;
    }

    public int resetCount() {
        return this.resetCount;
    }

    public boolean onboardingReopened() {
        return this.onboardingReopened;
    }
}
