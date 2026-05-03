package com.whatxe.xlib.ability;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record StateControlStatus(
        Set<ResourceLocation> lockingPolicies,
        Set<ResourceLocation> silencingPolicies,
        Set<ResourceLocation> suppressingPolicies
) {
    public StateControlStatus {
        lockingPolicies = Set.copyOf(lockingPolicies);
        silencingPolicies = Set.copyOf(silencingPolicies);
        suppressingPolicies = Set.copyOf(suppressingPolicies);
    }

    public boolean locked() {
        return !this.lockingPolicies.isEmpty();
    }

    public boolean silenced() {
        return !this.silencingPolicies.isEmpty();
    }

    public boolean suppressed() {
        return !this.suppressingPolicies.isEmpty();
    }

    public boolean activationBlocked() {
        return silenced() || suppressed();
    }

    public boolean assignmentBlocked() {
        return locked();
    }
}
