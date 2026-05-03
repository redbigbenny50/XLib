package com.whatxe.xlib.client;

public record AbilityResourceHudLayout(
        AbilityResourceHudAnchor anchor,
        AbilityResourceHudOrientation orientation,
        int width,
        int height,
        int spacing,
        int priority,
        int offsetX,
        int offsetY,
        boolean showName,
        boolean showValue
) {
    public static AbilityResourceHudLayout defaultLayout() {
        return wideAboveHotbar().build();
    }

    public static Builder wideAboveHotbar() {
        return builder()
                .anchor(AbilityResourceHudAnchor.ABOVE_HOTBAR_RIGHT)
                .orientation(AbilityResourceHudOrientation.HORIZONTAL)
                .width(104)
                .height(14)
                .spacing(3);
    }

    public static Builder compactSidebar() {
        return builder()
                .anchor(AbilityResourceHudAnchor.RIGHT_OF_HOTBAR)
                .orientation(AbilityResourceHudOrientation.VERTICAL)
                .width(18)
                .height(54)
                .spacing(4);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AbilityResourceHudAnchor anchor = AbilityResourceHudAnchor.ABOVE_HOTBAR_RIGHT;
        private AbilityResourceHudOrientation orientation = AbilityResourceHudOrientation.HORIZONTAL;
        private int width = 104;
        private int height = 14;
        private int spacing = 3;
        private int priority;
        private int offsetX;
        private int offsetY;
        private boolean showName = true;
        private boolean showValue = true;

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

        public Builder offsetX(int offsetX) {
            this.offsetX = offsetX;
            return this;
        }

        public Builder offsetY(int offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        public Builder showName(boolean showName) {
            this.showName = showName;
            return this;
        }

        public Builder showValue(boolean showValue) {
            this.showValue = showValue;
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
                    this.priority,
                    this.offsetX,
                    this.offsetY,
                    this.showName,
                    this.showValue
            );
        }
    }
}

