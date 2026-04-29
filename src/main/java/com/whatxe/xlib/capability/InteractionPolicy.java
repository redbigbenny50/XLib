package com.whatxe.xlib.capability;

public record InteractionPolicy(
        boolean canInteractWithBlocks,
        boolean canInteractWithEntities,
        boolean canAttackPlayers,
        boolean canAttackMobs
) {
    public static final InteractionPolicy FULL = new InteractionPolicy(true, true, true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canInteractWithBlocks = true;
        private boolean canInteractWithEntities = true;
        private boolean canAttackPlayers = true;
        private boolean canAttackMobs = true;

        private Builder() {}

        public Builder canInteractWithBlocks(boolean value) { this.canInteractWithBlocks = value; return this; }
        public Builder canInteractWithEntities(boolean value) { this.canInteractWithEntities = value; return this; }
        public Builder canAttackPlayers(boolean value) { this.canAttackPlayers = value; return this; }
        public Builder canAttackMobs(boolean value) { this.canAttackMobs = value; return this; }

        public InteractionPolicy build() {
            return new InteractionPolicy(canInteractWithBlocks, canInteractWithEntities, canAttackPlayers, canAttackMobs);
        }
    }

    InteractionPolicy mergeRestrictive(InteractionPolicy other) {
        return new InteractionPolicy(
                this.canInteractWithBlocks && other.canInteractWithBlocks,
                this.canInteractWithEntities && other.canInteractWithEntities,
                this.canAttackPlayers && other.canAttackPlayers,
                this.canAttackMobs && other.canAttackMobs
        );
    }
}
