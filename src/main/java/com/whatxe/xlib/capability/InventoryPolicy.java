package com.whatxe.xlib.capability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record InventoryPolicy(
        boolean canOpenInventory,
        boolean canMoveItems,
        boolean canUseHotbar,
        boolean canUseOffhand,
        boolean canChangeSelectedHotbarSlot,
        Set<Integer> allowedHotbarSlots,
        Set<Integer> blockedHotbarSlots
) {
    public static final InventoryPolicy FULL = new InventoryPolicy(true, true, true, true, true, Set.of(), Set.of());

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canOpenInventory = true;
        private boolean canMoveItems = true;
        private boolean canUseHotbar = true;
        private boolean canUseOffhand = true;
        private boolean canChangeSelectedHotbarSlot = true;
        private final Set<Integer> allowedHotbarSlots = new LinkedHashSet<>();
        private final Set<Integer> blockedHotbarSlots = new LinkedHashSet<>();

        private Builder() {}

        public Builder canOpenInventory(boolean value) { this.canOpenInventory = value; return this; }
        public Builder canMoveItems(boolean value) { this.canMoveItems = value; return this; }
        public Builder canUseHotbar(boolean value) { this.canUseHotbar = value; return this; }
        public Builder canUseOffhand(boolean value) { this.canUseOffhand = value; return this; }
        public Builder canChangeSelectedHotbarSlot(boolean value) { this.canChangeSelectedHotbarSlot = value; return this; }
        public Builder allowHotbarSlot(int slot) { this.allowedHotbarSlots.add(validateHotbarSlot(slot)); return this; }
        public Builder blockHotbarSlot(int slot) { this.blockedHotbarSlots.add(validateHotbarSlot(slot)); return this; }

        public InventoryPolicy build() {
            return new InventoryPolicy(
                    canOpenInventory,
                    canMoveItems,
                    canUseHotbar,
                    canUseOffhand,
                    canChangeSelectedHotbarSlot,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedHotbarSlots)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedHotbarSlots))
            );
        }
    }

    InventoryPolicy mergeRestrictive(InventoryPolicy other) {
        return new InventoryPolicy(
                this.canOpenInventory && other.canOpenInventory,
                this.canMoveItems && other.canMoveItems,
                this.canUseHotbar && other.canUseHotbar,
                this.canUseOffhand && other.canUseOffhand,
                this.canChangeSelectedHotbarSlot && other.canChangeSelectedHotbarSlot,
                mergeUnion(this.allowedHotbarSlots, other.allowedHotbarSlots),
                mergeUnion(this.blockedHotbarSlots, other.blockedHotbarSlots)
        );
    }

    public boolean allowsSelectedHotbarSlot(int slot) {
        int resolvedSlot = validateHotbarSlot(slot);
        if (this.blockedHotbarSlots.contains(resolvedSlot)) {
            return false;
        }
        return this.allowedHotbarSlots.isEmpty() || this.allowedHotbarSlots.contains(resolvedSlot);
    }

    private static Set<Integer> mergeUnion(Set<Integer> left, Set<Integer> right) {
        LinkedHashSet<Integer> merged = new LinkedHashSet<>(left);
        merged.addAll(right);
        return Collections.unmodifiableSet(merged);
    }

    static int validateHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("Hotbar slot must be between 0 and 8");
        }
        return slot;
    }
}
