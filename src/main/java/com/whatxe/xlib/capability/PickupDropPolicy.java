package com.whatxe.xlib.capability;

public record PickupDropPolicy(
        boolean canPickupItems,
        boolean canDropItems
) {
    public static final PickupDropPolicy FULL = new PickupDropPolicy(true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canPickupItems = true;
        private boolean canDropItems = true;

        private Builder() {}

        public Builder canPickupItems(boolean value) { this.canPickupItems = value; return this; }
        public Builder canDropItems(boolean value) { this.canDropItems = value; return this; }

        public PickupDropPolicy build() {
            return new PickupDropPolicy(canPickupItems, canDropItems);
        }
    }

    PickupDropPolicy mergeRestrictive(PickupDropPolicy other) {
        return new PickupDropPolicy(
                this.canPickupItems && other.canPickupItems,
                this.canDropItems && other.canDropItems
        );
    }
}
