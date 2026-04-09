package com.whatxe.xlib.menu;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public record MenuAccessDecision(MenuAccessState state, Optional<Component> reason) {
    public MenuAccessDecision {
        state = Objects.requireNonNull(state, "state");
        reason = reason == null ? Optional.empty() : reason;
    }

    public static MenuAccessDecision available() {
        return new MenuAccessDecision(MenuAccessState.AVAILABLE, Optional.empty());
    }

    public static MenuAccessDecision locked(Component reason) {
        return new MenuAccessDecision(MenuAccessState.LOCKED, Optional.ofNullable(reason));
    }

    public static MenuAccessDecision hidden(Component reason) {
        return new MenuAccessDecision(MenuAccessState.HIDDEN, Optional.ofNullable(reason));
    }

    public boolean isAvailable() {
        return this.state == MenuAccessState.AVAILABLE;
    }

    public boolean isLocked() {
        return this.state == MenuAccessState.LOCKED;
    }

    public boolean isHidden() {
        return this.state == MenuAccessState.HIDDEN;
    }
}
