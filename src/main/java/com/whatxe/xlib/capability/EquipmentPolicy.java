package com.whatxe.xlib.capability;

public record EquipmentPolicy(
        boolean canEquipArmor,
        boolean canUnequipArmor,
        boolean canEquipHeldItems
) {
    public static final EquipmentPolicy FULL = new EquipmentPolicy(true, true, true);

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canEquipArmor = true;
        private boolean canUnequipArmor = true;
        private boolean canEquipHeldItems = true;

        private Builder() {}

        public Builder canEquipArmor(boolean value) { this.canEquipArmor = value; return this; }
        public Builder canUnequipArmor(boolean value) { this.canUnequipArmor = value; return this; }
        public Builder canEquipHeldItems(boolean value) { this.canEquipHeldItems = value; return this; }

        public EquipmentPolicy build() {
            return new EquipmentPolicy(canEquipArmor, canUnequipArmor, canEquipHeldItems);
        }
    }

    EquipmentPolicy mergeRestrictive(EquipmentPolicy other) {
        return new EquipmentPolicy(
                this.canEquipArmor && other.canEquipArmor,
                this.canUnequipArmor && other.canUnequipArmor,
                this.canEquipHeldItems && other.canEquipHeldItems
        );
    }
}
