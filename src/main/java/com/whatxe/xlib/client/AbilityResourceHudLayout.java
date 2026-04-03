package com.whatxe.xlib.client;

public record AbilityResourceHudLayout(
        AbilityResourceHudAnchor anchor,
        AbilityResourceHudOrientation orientation,
        int width,
        int height,
        int spacing,
        int priority
) {
    public static AbilityResourceHudLayout defaultLayout() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AbilityResourceHudAnchor anchor = AbilityResourceHudAnchor.RIGHT_OF_HOTBAR;
        private AbilityResourceHudOrientation orientation = AbilityResourceHudOrientation.VERTICAL;
        private int width = 18;
        private int height = 54;
        private int spacing = 4;
        private int priority;

        private Builder() {}

        public Builder anchor(AbilityResourceHudAnchor anchor) {
            this.anchor = anchor;
            return this;
        }

        public Builder orientation(AbilityResourceHudOrientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder spacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public AbilityResourceHudLayout build() {
            if (this.anchor == null) {
                throw new IllegalStateException("anchor must be provided");
            }
            if (this.orientation == null) {
                throw new IllegalStateException("orientation must be provided");
            }
            if (this.width <= 0) {
                throw new IllegalStateException("width must be positive");
            }
            if (this.height <= 0) {
                throw new IllegalStateException("height must be positive");
            }
            if (this.spacing < 0) {
                throw new IllegalStateException("spacing cannot be negative");
            }
            return new AbilityResourceHudLayout(
                    this.anchor,
                    this.orientation,
                    this.width,
                    this.height,
                    this.spacing,
                    this.priority
            );
        }
    }
}

