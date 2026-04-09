package com.whatxe.xlib.ability;

public record AbilityLoadoutFeatureDecision(boolean managementEnabled, boolean quickSwitchEnabled) {
    private static final AbilityLoadoutFeatureDecision DISABLED = new AbilityLoadoutFeatureDecision(false, false);
    private static final AbilityLoadoutFeatureDecision MANAGEMENT_ONLY = new AbilityLoadoutFeatureDecision(true, false);
    private static final AbilityLoadoutFeatureDecision MANAGEMENT_AND_QUICK_SWITCH =
            new AbilityLoadoutFeatureDecision(true, true);

    public static AbilityLoadoutFeatureDecision disabled() {
        return DISABLED;
    }

    public static AbilityLoadoutFeatureDecision managementOnly() {
        return MANAGEMENT_ONLY;
    }

    public static AbilityLoadoutFeatureDecision managementAndQuickSwitch() {
        return MANAGEMENT_AND_QUICK_SWITCH;
    }

    public AbilityLoadoutFeatureDecision merge(AbilityLoadoutFeatureDecision other) {
        if (other == null) {
            return this;
        }
        return new AbilityLoadoutFeatureDecision(
                this.managementEnabled || other.managementEnabled,
                this.quickSwitchEnabled || other.quickSwitchEnabled
        );
    }
}
