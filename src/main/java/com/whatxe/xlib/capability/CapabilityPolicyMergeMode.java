package com.whatxe.xlib.capability;

public enum CapabilityPolicyMergeMode {
    /** A false value in any active policy wins — the most restrictive policy governs. */
    RESTRICTIVE,
    /** A true value in any active policy wins — allows if any policy permits. */
    PERMISSIVE
}
