package com.whatxe.xlib.capability;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class CapabilityPolicyDefinition {
    private final ResourceLocation id;
    private final InventoryPolicy inventory;
    private final EquipmentPolicy equipment;
    private final HeldItemPolicy heldItems;
    private final CraftingPolicy crafting;
    private final ContainerPolicy containers;
    private final PickupDropPolicy pickupDrop;
    private final InteractionPolicy interaction;
    private final MenuPolicy menus;
    private final MovementPolicy movement;
    private final CapabilityPolicyMergeMode mergeMode;
    private final int priority;

    private CapabilityPolicyDefinition(Builder builder) {
        this.id = builder.id;
        this.inventory = builder.inventory;
        this.equipment = builder.equipment;
        this.heldItems = builder.heldItems;
        this.crafting = builder.crafting;
        this.containers = builder.containers;
        this.pickupDrop = builder.pickupDrop;
        this.interaction = builder.interaction;
        this.menus = builder.menus;
        this.movement = builder.movement;
        this.mergeMode = builder.mergeMode;
        this.priority = builder.priority;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() { return id; }
    public InventoryPolicy inventory() { return inventory; }
    public EquipmentPolicy equipment() { return equipment; }
    public HeldItemPolicy heldItems() { return heldItems; }
    public CraftingPolicy crafting() { return crafting; }
    public ContainerPolicy containers() { return containers; }
    public PickupDropPolicy pickupDrop() { return pickupDrop; }
    public InteractionPolicy interaction() { return interaction; }
    public MenuPolicy menus() { return menus; }
    public MovementPolicy movement() { return movement; }
    public CapabilityPolicyMergeMode mergeMode() { return mergeMode; }
    public int priority() { return priority; }

    public static final class Builder {
        private final ResourceLocation id;
        private InventoryPolicy inventory = InventoryPolicy.FULL;
        private EquipmentPolicy equipment = EquipmentPolicy.FULL;
        private HeldItemPolicy heldItems = HeldItemPolicy.FULL;
        private CraftingPolicy crafting = CraftingPolicy.FULL;
        private ContainerPolicy containers = ContainerPolicy.FULL;
        private PickupDropPolicy pickupDrop = PickupDropPolicy.FULL;
        private InteractionPolicy interaction = InteractionPolicy.FULL;
        private MenuPolicy menus = MenuPolicy.FULL;
        private MovementPolicy movement = MovementPolicy.FULL;
        private CapabilityPolicyMergeMode mergeMode = CapabilityPolicyMergeMode.RESTRICTIVE;
        private int priority = 0;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder inventory(InventoryPolicy policy) { this.inventory = Objects.requireNonNull(policy); return this; }
        public Builder equipment(EquipmentPolicy policy) { this.equipment = Objects.requireNonNull(policy); return this; }
        public Builder heldItems(HeldItemPolicy policy) { this.heldItems = Objects.requireNonNull(policy); return this; }
        public Builder crafting(CraftingPolicy policy) { this.crafting = Objects.requireNonNull(policy); return this; }
        public Builder containers(ContainerPolicy policy) { this.containers = Objects.requireNonNull(policy); return this; }
        public Builder pickupDrop(PickupDropPolicy policy) { this.pickupDrop = Objects.requireNonNull(policy); return this; }
        public Builder interaction(InteractionPolicy policy) { this.interaction = Objects.requireNonNull(policy); return this; }
        public Builder menus(MenuPolicy policy) { this.menus = Objects.requireNonNull(policy); return this; }
        public Builder movement(MovementPolicy policy) { this.movement = Objects.requireNonNull(policy); return this; }
        public Builder mergeMode(CapabilityPolicyMergeMode mode) { this.mergeMode = Objects.requireNonNull(mode); return this; }
        public Builder priority(int priority) { this.priority = priority; return this; }

        public CapabilityPolicyDefinition build() {
            return new CapabilityPolicyDefinition(this);
        }
    }
}
