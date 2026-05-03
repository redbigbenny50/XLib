package com.whatxe.xlib.ability;

import java.util.Objects;

public record AbilityLoadoutFeature(boolean exposesQuickSwitchKeybind, AbilityLoadoutFeaturePolicy policy) {
    public AbilityLoadoutFeature {
        Objects.requireNonNull(policy, "policy");
    }

    public static AbilityLoadoutFeature managementOnly(AbilityLoadoutFeaturePolicy policy) {
        return new AbilityLoadoutFeature(false, policy);
    }

    public static AbilityLoadoutFeature managementAndQuickSwitch(AbilityLoadoutFeaturePolicy policy) {
        return new AbilityLoadoutFeature(true, policy);
    }

    public static AbilityLoadoutFeature alwaysEnabled() {
        return managementOnly((player, data) -> AbilityLoadoutFeatureDecision.managementOnly());
    }

    public static AbilityLoadoutFeature alwaysEnabledWithQuickSwitch() {
        return managementAndQuickSwitch((player, data) -> AbilityLoadoutFeatureDecision.managementAndQuickSwitch());
    }
}
