package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ComboChainDefinition {
    public enum TriggerType {
        ACTIVATION,
        HIT_CONFIRM,
        END
    }

    @FunctionalInterface
    public interface BranchCondition {
        boolean test(ServerPlayer player, AbilityData data);
    }

    public record Branch(ResourceLocation comboAbilityId, BranchCondition condition) {
        public Branch {
            Objects.requireNonNull(comboAbilityId, "comboAbilityId");
            Objects.requireNonNull(condition, "condition");
        }
    }

    private final ResourceLocation id;
    private final ResourceLocation triggerAbilityId;
    private final ResourceLocation comboAbilityId;
    private final List<Branch> branches;
    private final TriggerType triggerType;
    private final int windowTicks;
    private final boolean transformTriggeredSlot;
    private final @Nullable Integer targetSlot;
    private final @Nullable AbilitySlotReference targetSlotReference;

    private ComboChainDefinition(
            ResourceLocation id,
            ResourceLocation triggerAbilityId,
            ResourceLocation comboAbilityId,
            List<Branch> branches,
            TriggerType triggerType,
            int windowTicks,
            boolean transformTriggeredSlot,
            @Nullable Integer targetSlot,
            @Nullable AbilitySlotReference targetSlotReference
    ) {
        this.id = id;
        this.triggerAbilityId = triggerAbilityId;
        this.comboAbilityId = comboAbilityId;
        this.branches = List.copyOf(branches);
        this.triggerType = triggerType;
        this.windowTicks = windowTicks;
        this.transformTriggeredSlot = transformTriggeredSlot;
        this.targetSlot = targetSlot;
        this.targetSlotReference = targetSlotReference;
    }

    public static Builder builder(ResourceLocation id, ResourceLocation triggerAbilityId, ResourceLocation comboAbilityId) {
        return new Builder(id, triggerAbilityId, comboAbilityId);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public ResourceLocation triggerAbilityId() {
        return this.triggerAbilityId;
    }

    public ResourceLocation comboAbilityId() {
        return this.comboAbilityId;
    }

    public List<Branch> branches() {
        return this.branches;
    }

    public TriggerType triggerType() {
        return this.triggerType;
    }

    public int windowTicks() {
        return this.windowTicks;
    }

    public boolean transformTriggeredSlot() {
        return this.transformTriggeredSlot;
    }

    public @Nullable Integer targetSlot() {
        return this.targetSlot;
    }

    public @Nullable AbilitySlotReference targetSlotReference() {
        return this.targetSlotReference;
    }

    public ResourceLocation resolveComboAbilityId(ServerPlayer player, AbilityData data) {
        for (Branch branch : this.branches) {
            if (branch.condition().test(player, data)) {
                return branch.comboAbilityId();
            }
        }
        return this.comboAbilityId;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation triggerAbilityId;
        private final ResourceLocation comboAbilityId;
        private final List<Branch> branches = new ArrayList<>();
        private TriggerType triggerType = TriggerType.ACTIVATION;
        private int windowTicks = 30;
        private boolean transformTriggeredSlot;
        private Integer targetSlot;
        private AbilitySlotReference targetSlotReference;

        private Builder(ResourceLocation id, ResourceLocation triggerAbilityId, ResourceLocation comboAbilityId) {
            this.id = Objects.requireNonNull(id, "id");
            this.triggerAbilityId = Objects.requireNonNull(triggerAbilityId, "triggerAbilityId");
            this.comboAbilityId = Objects.requireNonNull(comboAbilityId, "comboAbilityId");
        }

        public Builder windowTicks(int windowTicks) {
            this.windowTicks = windowTicks;
            return this;
        }

        public Builder branch(ResourceLocation comboAbilityId, BranchCondition condition) {
            this.branches.add(new Branch(comboAbilityId, condition));
            return this;
        }

        public Builder triggerOnHit() {
            this.triggerType = TriggerType.HIT_CONFIRM;
            return this;
        }

        public Builder triggerOnEnd() {
            this.triggerType = TriggerType.END;
            return this;
        }

        public Builder triggerOnActivation() {
            this.triggerType = TriggerType.ACTIVATION;
            return this;
        }

        public Builder transformTriggeredSlot() {
            this.transformTriggeredSlot = true;
            this.targetSlot = null;
            this.targetSlotReference = null;
            return this;
        }

        public Builder targetSlot(int targetSlot) {
            if (targetSlot < 0 || targetSlot >= AbilityData.SLOT_COUNT) {
                throw new IllegalArgumentException("Invalid combo target slot: " + targetSlot);
            }
            this.targetSlot = targetSlot;
            this.targetSlotReference = AbilitySlotReference.primary(targetSlot);
            this.transformTriggeredSlot = false;
            return this;
        }

        public Builder targetSlot(AbilitySlotReference slotReference) {
            AbilitySlotReference resolvedSlotReference = Objects.requireNonNull(slotReference, "slotReference");
            if (!AbilitySlotContainerApi.isPrimarySlotReference(resolvedSlotReference)) {
                throw new IllegalArgumentException("Auxiliary combo target slots are no longer supported: " + resolvedSlotReference);
            }
            this.targetSlotReference = resolvedSlotReference;
            this.targetSlot = resolvedSlotReference.slotIndex();
            this.transformTriggeredSlot = false;
            return this;
        }

        public ComboChainDefinition build() {
            if (this.windowTicks <= 0) {
                throw new IllegalStateException("Combo chains require a positive window");
            }
            return new ComboChainDefinition(
                    this.id,
                    this.triggerAbilityId,
                    this.comboAbilityId,
                    this.branches,
                    this.triggerType,
                    this.windowTicks,
                    this.transformTriggeredSlot,
                    this.targetSlot,
                    this.targetSlotReference
            );
        }
    }
}
