package com.whatxe.xlib.ability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class AbilitySlotMigrationPlan {
    private final ResourceLocation id;
    private final int priority;
    private final Map<Integer, AbilitySlotReference> baseSlotRemaps;
    private final Map<Integer, AbilitySlotReference> comboOverrideRemaps;
    private final Map<ResourceLocation, Map<Integer, AbilitySlotReference>> modeSlotRemaps;

    private AbilitySlotMigrationPlan(
            ResourceLocation id,
            int priority,
            Map<Integer, AbilitySlotReference> baseSlotRemaps,
            Map<Integer, AbilitySlotReference> comboOverrideRemaps,
            Map<ResourceLocation, Map<Integer, AbilitySlotReference>> modeSlotRemaps
    ) {
        this.id = id;
        this.priority = priority;
        this.baseSlotRemaps = Map.copyOf(baseSlotRemaps);
        this.comboOverrideRemaps = Map.copyOf(comboOverrideRemaps);
        Map<ResourceLocation, Map<Integer, AbilitySlotReference>> copiedModeSlotRemaps = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Map<Integer, AbilitySlotReference>> entry : modeSlotRemaps.entrySet()) {
            copiedModeSlotRemaps.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        this.modeSlotRemaps = Map.copyOf(copiedModeSlotRemaps);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public int priority() {
        return this.priority;
    }

    public Map<Integer, AbilitySlotReference> baseSlotRemaps() {
        return this.baseSlotRemaps;
    }

    public Map<Integer, AbilitySlotReference> comboOverrideRemaps() {
        return this.comboOverrideRemaps;
    }

    public Map<ResourceLocation, Map<Integer, AbilitySlotReference>> modeSlotRemaps() {
        return this.modeSlotRemaps;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private int priority;
        private final Map<Integer, AbilitySlotReference> baseSlotRemaps = new LinkedHashMap<>();
        private final Map<Integer, AbilitySlotReference> comboOverrideRemaps = new LinkedHashMap<>();
        private final Map<ResourceLocation, Map<Integer, AbilitySlotReference>> modeSlotRemaps = new LinkedHashMap<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder remapBaseSlot(int legacySlot, AbilitySlotReference target) {
            validateSlot(legacySlot);
            this.baseSlotRemaps.put(legacySlot, Objects.requireNonNull(target, "target"));
            return this;
        }

        public Builder remapComboOverride(int legacySlot, AbilitySlotReference target) {
            validateSlot(legacySlot);
            this.comboOverrideRemaps.put(legacySlot, Objects.requireNonNull(target, "target"));
            return this;
        }

        public Builder remapModeSlot(ResourceLocation modeId, int legacySlot, AbilitySlotReference target) {
            validateSlot(legacySlot);
            this.modeSlotRemaps.computeIfAbsent(Objects.requireNonNull(modeId, "modeId"), ignored -> new LinkedHashMap<>())
                    .put(legacySlot, Objects.requireNonNull(target, "target"));
            return this;
        }

        public AbilitySlotMigrationPlan build() {
            return new AbilitySlotMigrationPlan(
                    this.id,
                    this.priority,
                    this.baseSlotRemaps,
                    this.comboOverrideRemaps,
                    this.modeSlotRemaps
            );
        }

        private static void validateSlot(int legacySlot) {
            if (legacySlot < 0) {
                throw new IllegalArgumentException("legacySlot cannot be negative");
            }
        }
    }
}
