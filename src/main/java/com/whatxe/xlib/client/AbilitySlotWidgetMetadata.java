package com.whatxe.xlib.client;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public record AbilitySlotWidgetMetadata(
        AbilitySlotWidgetRole role,
        @Nullable Component shortLabel,
        @Nullable Component categoryLabel,
        @Nullable Component roleHint,
        boolean softLocked
) {
    private static final AbilitySlotWidgetMetadata DEFAULT = new AbilitySlotWidgetMetadata(
            AbilitySlotWidgetRole.STANDARD,
            null,
            null,
            null,
            false
    );

    public AbilitySlotWidgetMetadata {
        role = Objects.requireNonNull(role, "role");
        shortLabel = copy(shortLabel);
        categoryLabel = copy(categoryLabel);
        roleHint = copy(roleHint);
    }

    public static AbilitySlotWidgetMetadata defaultMetadata() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static @Nullable Component copy(@Nullable Component value) {
        return value == null ? null : value.copy();
    }

    public static final class Builder {
        private AbilitySlotWidgetRole role = AbilitySlotWidgetRole.STANDARD;
        private @Nullable Component shortLabel;
        private @Nullable Component categoryLabel;
        private @Nullable Component roleHint;
        private boolean softLocked;

        private Builder() {}

        public Builder role(AbilitySlotWidgetRole role) {
            this.role = Objects.requireNonNull(role, "role");
            return this;
        }

        public Builder shortLabel(Component shortLabel) {
            this.shortLabel = Objects.requireNonNull(shortLabel, "shortLabel");
            return this;
        }

        public Builder categoryLabel(Component categoryLabel) {
            this.categoryLabel = Objects.requireNonNull(categoryLabel, "categoryLabel");
            return this;
        }

        public Builder roleHint(Component roleHint) {
            this.roleHint = Objects.requireNonNull(roleHint, "roleHint");
            return this;
        }

        public Builder softLocked(boolean softLocked) {
            this.softLocked = softLocked;
            return this;
        }

        public AbilitySlotWidgetMetadata build() {
            return new AbilitySlotWidgetMetadata(this.role, this.shortLabel, this.categoryLabel, this.roleHint, this.softLocked);
        }
    }
}
