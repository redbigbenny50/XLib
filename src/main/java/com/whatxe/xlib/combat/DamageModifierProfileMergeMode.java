package com.whatxe.xlib.combat;

public enum DamageModifierProfileMergeMode {
    /**
     * Default behavior — multiply all matching profile multipliers together.
     * e.g. 1.5 * 0.8 = 1.2
     */
    MULTIPLICATIVE,

    /**
     * Sum all multipliers across active profiles.
     * e.g. 1.5 + 0.8 = 2.3
     */
    ADDITIVE,

    /**
     * Only the profile with the highest {@code priority} value is used;
     * all lower-priority profiles are ignored for this damage source.
     */
    OVERRIDE
}
