package com.whatxe.xlib.ability;

import java.util.Objects;

public record AbilityControlBinding(AbilityControlTrigger trigger, AbilityControlAction action) {
    public AbilityControlBinding {
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(action, "action");
    }
}
