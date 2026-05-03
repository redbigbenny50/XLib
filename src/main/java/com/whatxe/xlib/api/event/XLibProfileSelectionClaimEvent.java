package com.whatxe.xlib.api.event;

import com.whatxe.xlib.ability.ProfileSelectionOrigin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public class XLibProfileSelectionClaimEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation groupId;
    private final ResourceLocation profileId;
    private final ProfileSelectionOrigin origin;
    private final boolean locked;
    private final String reason;
    private final boolean pendingClaim;

    public XLibProfileSelectionClaimEvent(
            ServerPlayer player,
            ResourceLocation groupId,
            ResourceLocation profileId,
            ProfileSelectionOrigin origin,
            boolean locked,
            String reason,
            boolean pendingClaim
    ) {
        this.player = player;
        this.groupId = groupId;
        this.profileId = profileId;
        this.origin = origin;
        this.locked = locked;
        this.reason = reason;
        this.pendingClaim = pendingClaim;
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

    public ProfileSelectionOrigin origin() {
        return this.origin;
    }

    public boolean locked() {
        return this.locked;
    }

    public String reason() {
        return this.reason;
    }

    public boolean pendingClaim() {
        return this.pendingClaim;
    }
}
