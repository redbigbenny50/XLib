package com.whatxe.xlib.combat;

import java.util.Objects;

public record CombatTargetingProfile(
        CombatTargetingMode mode,
        double range,
        double radius,
        double angleDegrees,
        double verticalRange,
        boolean requireLineOfSight,
        int maxTargets
) {
    public static Builder builder() {
        return new Builder();
    }

    public static CombatTargetingProfile melee(double range) {
        return builder().mode(CombatTargetingMode.DIRECT).range(range).build();
    }

    public static CombatTargetingProfile cone(double range, double angleDegrees) {
        return builder().mode(CombatTargetingMode.CONE).range(range).angleDegrees(angleDegrees).build();
    }

    public static CombatTargetingProfile radius(double range, double radius) {
        return builder().mode(CombatTargetingMode.RADIUS).range(range).radius(radius).build();
    }

    public static final class Builder {
        private CombatTargetingMode mode = CombatTargetingMode.DIRECT;
        private double range = 4.0D;
        private double radius = 1.25D;
        private double angleDegrees = 65.0D;
        private double verticalRange = 2.5D;
        private boolean requireLineOfSight = true;
        private int maxTargets = 1;

        public Builder mode(CombatTargetingMode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        public Builder range(double range) {
            this.range = range;
            return this;
        }

        public Builder radius(double radius) {
            this.radius = radius;
            return this;
        }

        public Builder angleDegrees(double angleDegrees) {
            this.angleDegrees = angleDegrees;
            return this;
        }

        public Builder verticalRange(double verticalRange) {
            this.verticalRange = verticalRange;
            return this;
        }

        public Builder requireLineOfSight(boolean requireLineOfSight) {
            this.requireLineOfSight = requireLineOfSight;
            return this;
        }

        public Builder maxTargets(int maxTargets) {
            this.maxTargets = maxTargets;
            return this;
        }

        public CombatTargetingProfile build() {
            if (!(this.range > 0.0D)) {
                throw new IllegalStateException("range must be positive");
            }
            if (this.radius < 0.0D) {
                throw new IllegalStateException("radius cannot be negative");
            }
            if (this.angleDegrees <= 0.0D || this.angleDegrees > 180.0D) {
                throw new IllegalStateException("angleDegrees must be between 0 and 180");
            }
            if (this.verticalRange < 0.0D) {
                throw new IllegalStateException("verticalRange cannot be negative");
            }
            if (this.maxTargets <= 0) {
                throw new IllegalStateException("maxTargets must be positive");
            }
            return new CombatTargetingProfile(
                    this.mode,
                    this.range,
                    this.radius,
                    this.angleDegrees,
                    this.verticalRange,
                    this.requireLineOfSight,
                    this.maxTargets
            );
        }
    }
}
