package com.whatxe.xlib.capability;

public record InventoryPolicy(
        boolean canOpenInventory,
        boolean canMoveItems,
        boolean canUseHotbar,
        boolean canUseOffhand
) {
    public static final InventoryPolicy FULL = new InventoryPolicy(true, true, true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canOpenInventory = true;
        private boolean canMoveItems = true;
        private boolean canUseHotbar = true;
        private boolean canUseOffhand = true;

        private Builder() {}

        public Builder canOpenInventory(boolean value) { this.canOpenInventory = value; return this; }
        public Builder canMoveItems(boolean value) { this.canMoveItems = value; return this; }
        public Builder canUseHotbar(boolean value) { this.canUseHotbar = value; return this; }
        public Builder canUseOffhand(boolean value) { this.canUseOffhand = value; return this; }

        public InventoryPolicy build() {
            return new InventoryPolicy(canOpenInventory, canMoveItems, canUseHotbar, canUseOffhand);
        }
    }

    InventoryPolicy mergeRestrictive(InventoryPolicy other) {
        return new InventoryPolicy(
                this.canOpenInventory && other.canOpenInventory,
                this.canMoveItems && other.canMoveItems,
                this.canUseHotbar && other.canUseHotbar,
                this.canUseOffhand && other.canUseOffhand
        );
    }
}
