package com.whatxe.xlib.capability;

public record MovementPolicy(
        boolean canSprint,
        boolean canSneak,
        boolean canJump,
        boolean canFly
) {
    public static final MovementPolicy FULL = new MovementPolicy(true, true, true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canSprint = true;
        private boolean canSneak = true;
        private boolean canJump = true;
        private boolean canFly = true;

        private Builder() {}

        public Builder canSprint(boolean value) { this.canSprint = value; return this; }
        public Builder canSneak(boolean value) { this.canSneak = value; return this; }
        public Builder canJump(boolean value) { this.canJump = value; return this; }
        public Builder canFly(boolean value) { this.canFly = value; return this; }

        public MovementPolicy build() {
            return new MovementPolicy(canSprint, canSneak, canJump, canFly);
        }
    }

    MovementPolicy mergeRestrictive(MovementPolicy other) {
        return new MovementPolicy(
                this.canSprint && other.canSprint,
                this.canSneak && other.canSneak,
                this.canJump && other.canJump,
                this.canFly && other.canFly
        );
    }
}
