package com.whatxe.xlib.combat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class DamageModifierProfileDefinition {
    private final ResourceLocation id;
    private final Map<ResourceLocation, Double> incomingDamageTypes;
    private final Map<ResourceLocation, Double> incomingDamageTypeTags;
    private final Map<ResourceLocation, Double> outgoingDamageTypes;
    private final Map<ResourceLocation, Double> outgoingDamageTypeTags;
    private final Map<ResourceLocation, Double> incomingFlatAdditions;
    private final Map<ResourceLocation, Double> incomingFlatAdditionTags;
    private final Map<ResourceLocation, Double> outgoingFlatAdditions;
    private final Map<ResourceLocation, Double> outgoingFlatAdditionTags;
    private final DamageModifierProfileMergeMode mergeMode;
    private final int priority;

    private DamageModifierProfileDefinition(
            ResourceLocation id,
            Map<ResourceLocation, Double> incomingDamageTypes,
            Map<ResourceLocation, Double> incomingDamageTypeTags,
            Map<ResourceLocation, Double> outgoingDamageTypes,
            Map<ResourceLocation, Double> outgoingDamageTypeTags,
            Map<ResourceLocation, Double> incomingFlatAdditions,
            Map<ResourceLocation, Double> incomingFlatAdditionTags,
            Map<ResourceLocation, Double> outgoingFlatAdditions,
            Map<ResourceLocation, Double> outgoingFlatAdditionTags,
            DamageModifierProfileMergeMode mergeMode,
            int priority
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.incomingDamageTypes = Map.copyOf(incomingDamageTypes);
        this.incomingDamageTypeTags = Map.copyOf(incomingDamageTypeTags);
        this.outgoingDamageTypes = Map.copyOf(outgoingDamageTypes);
        this.outgoingDamageTypeTags = Map.copyOf(outgoingDamageTypeTags);
        this.incomingFlatAdditions = Map.copyOf(incomingFlatAdditions);
        this.incomingFlatAdditionTags = Map.copyOf(incomingFlatAdditionTags);
        this.outgoingFlatAdditions = Map.copyOf(outgoingFlatAdditions);
        this.outgoingFlatAdditionTags = Map.copyOf(outgoingFlatAdditionTags);
        this.mergeMode = Objects.requireNonNull(mergeMode, "mergeMode");
        this.priority = priority;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Map<ResourceLocation, Double> incomingDamageTypes() {
        return this.incomingDamageTypes;
    }

    public Map<ResourceLocation, Double> incomingDamageTypeTags() {
        return this.incomingDamageTypeTags;
    }

    public Map<ResourceLocation, Double> outgoingDamageTypes() {
        return this.outgoingDamageTypes;
    }

    public Map<ResourceLocation, Double> outgoingDamageTypeTags() {
        return this.outgoingDamageTypeTags;
    }

    public Map<ResourceLocation, Double> incomingFlatAdditions() {
        return this.incomingFlatAdditions;
    }

    public Map<ResourceLocation, Double> incomingFlatAdditionTags() {
        return this.incomingFlatAdditionTags;
    }

    public Map<ResourceLocation, Double> outgoingFlatAdditions() {
        return this.outgoingFlatAdditions;
    }

    public Map<ResourceLocation, Double> outgoingFlatAdditionTags() {
        return this.outgoingFlatAdditionTags;
    }

    public DamageModifierProfileMergeMode mergeMode() {
        return this.mergeMode;
    }

    public int priority() {
        return this.priority;
    }

    public double incomingMultiplier(DamageSource source) {
        return resolveMultiplier(source, this.incomingDamageTypes, this.incomingDamageTypeTags);
    }

    public double outgoingMultiplier(DamageSource source) {
        return resolveMultiplier(source, this.outgoingDamageTypes, this.outgoingDamageTypeTags);
    }

    public double incomingFlat(DamageSource source) {
        return resolveFlat(source, this.incomingFlatAdditions, this.incomingFlatAdditionTags);
    }

    public double outgoingFlat(DamageSource source) {
        return resolveFlat(source, this.outgoingFlatAdditions, this.outgoingFlatAdditionTags);
    }

    public boolean hasIncomingRules() {
        return !this.incomingDamageTypes.isEmpty() || !this.incomingDamageTypeTags.isEmpty()
                || !this.incomingFlatAdditions.isEmpty() || !this.incomingFlatAdditionTags.isEmpty();
    }

    public boolean hasOutgoingRules() {
        return !this.outgoingDamageTypes.isEmpty() || !this.outgoingDamageTypeTags.isEmpty()
                || !this.outgoingFlatAdditions.isEmpty() || !this.outgoingFlatAdditionTags.isEmpty();
    }

    private static double resolveMultiplier(
            DamageSource source,
            Map<ResourceLocation, Double> exactDamageTypes,
            Map<ResourceLocation, Double> damageTypeTags
    ) {
        double multiplier = 1.0D;
        for (Map.Entry<ResourceLocation, Double> entry : exactDamageTypes.entrySet()) {
            if (source.is(ResourceKey.create(Registries.DAMAGE_TYPE, entry.getKey()))) {
                multiplier *= entry.getValue();
            }
        }
        for (Map.Entry<ResourceLocation, Double> entry : damageTypeTags.entrySet()) {
            if (source.is(TagKey.create(Registries.DAMAGE_TYPE, entry.getKey()))) {
                multiplier *= entry.getValue();
            }
        }
        return multiplier;
    }

    private static double resolveFlat(
            DamageSource source,
            Map<ResourceLocation, Double> exactDamageTypes,
            Map<ResourceLocation, Double> damageTypeTags
    ) {
        double total = 0.0D;
        for (Map.Entry<ResourceLocation, Double> entry : exactDamageTypes.entrySet()) {
            if (source.is(ResourceKey.create(Registries.DAMAGE_TYPE, entry.getKey()))) {
                total += entry.getValue();
            }
        }
        for (Map.Entry<ResourceLocation, Double> entry : damageTypeTags.entrySet()) {
            if (source.is(TagKey.create(Registries.DAMAGE_TYPE, entry.getKey()))) {
                total += entry.getValue();
            }
        }
        return total;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Map<ResourceLocation, Double> incomingDamageTypes = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> incomingDamageTypeTags = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> outgoingDamageTypes = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> outgoingDamageTypeTags = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> incomingFlatAdditions = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> incomingFlatAdditionTags = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> outgoingFlatAdditions = new LinkedHashMap<>();
        private final Map<ResourceLocation, Double> outgoingFlatAdditionTags = new LinkedHashMap<>();
        private DamageModifierProfileMergeMode mergeMode = DamageModifierProfileMergeMode.MULTIPLICATIVE;
        private int priority = 0;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder incomingDamageType(ResourceLocation damageTypeId, double multiplier) {
            this.incomingDamageTypes.put(Objects.requireNonNull(damageTypeId, "damageTypeId"), validateMultiplier(multiplier));
            return this;
        }

        public Builder incomingDamageTypes(Map<ResourceLocation, Double> multipliers) {
            putAll(this.incomingDamageTypes, multipliers);
            return this;
        }

        public Builder incomingDamageTypeTag(ResourceLocation damageTypeTagId, double multiplier) {
            this.incomingDamageTypeTags.put(Objects.requireNonNull(damageTypeTagId, "damageTypeTagId"), validateMultiplier(multiplier));
            return this;
        }

        public Builder incomingDamageTypeTags(Map<ResourceLocation, Double> multipliers) {
            putAll(this.incomingDamageTypeTags, multipliers);
            return this;
        }

        public Builder outgoingDamageType(ResourceLocation damageTypeId, double multiplier) {
            this.outgoingDamageTypes.put(Objects.requireNonNull(damageTypeId, "damageTypeId"), validateMultiplier(multiplier));
            return this;
        }

        public Builder outgoingDamageTypes(Map<ResourceLocation, Double> multipliers) {
            putAll(this.outgoingDamageTypes, multipliers);
            return this;
        }

        public Builder outgoingDamageTypeTag(ResourceLocation damageTypeTagId, double multiplier) {
            this.outgoingDamageTypeTags.put(Objects.requireNonNull(damageTypeTagId, "damageTypeTagId"), validateMultiplier(multiplier));
            return this;
        }

        public Builder outgoingDamageTypeTags(Map<ResourceLocation, Double> multipliers) {
            putAll(this.outgoingDamageTypeTags, multipliers);
            return this;
        }

        public Builder incomingFlatAddition(ResourceLocation damageTypeId, double amount) {
            this.incomingFlatAdditions.put(Objects.requireNonNull(damageTypeId, "damageTypeId"), validateFlat(amount));
            return this;
        }

        public Builder incomingFlatAdditions(Map<ResourceLocation, Double> amounts) {
            putAllFlat(this.incomingFlatAdditions, amounts);
            return this;
        }

        public Builder incomingFlatAdditionTag(ResourceLocation damageTypeTagId, double amount) {
            this.incomingFlatAdditionTags.put(Objects.requireNonNull(damageTypeTagId, "damageTypeTagId"), validateFlat(amount));
            return this;
        }

        public Builder incomingFlatAdditionTags(Map<ResourceLocation, Double> amounts) {
            putAllFlat(this.incomingFlatAdditionTags, amounts);
            return this;
        }

        public Builder outgoingFlatAddition(ResourceLocation damageTypeId, double amount) {
            this.outgoingFlatAdditions.put(Objects.requireNonNull(damageTypeId, "damageTypeId"), validateFlat(amount));
            return this;
        }

        public Builder outgoingFlatAdditions(Map<ResourceLocation, Double> amounts) {
            putAllFlat(this.outgoingFlatAdditions, amounts);
            return this;
        }

        public Builder outgoingFlatAdditionTag(ResourceLocation damageTypeTagId, double amount) {
            this.outgoingFlatAdditionTags.put(Objects.requireNonNull(damageTypeTagId, "damageTypeTagId"), validateFlat(amount));
            return this;
        }

        public Builder outgoingFlatAdditionTags(Map<ResourceLocation, Double> amounts) {
            putAllFlat(this.outgoingFlatAdditionTags, amounts);
            return this;
        }

        public Builder mergeMode(DamageModifierProfileMergeMode mergeMode) {
            this.mergeMode = Objects.requireNonNull(mergeMode, "mergeMode");
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public DamageModifierProfileDefinition build() {
            return new DamageModifierProfileDefinition(
                    this.id,
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.incomingDamageTypes)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.incomingDamageTypeTags)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.outgoingDamageTypes)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.outgoingDamageTypeTags)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.incomingFlatAdditions)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.incomingFlatAdditionTags)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.outgoingFlatAdditions)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(this.outgoingFlatAdditionTags)),
                    this.mergeMode,
                    this.priority
            );
        }

        private static void putAll(Map<ResourceLocation, Double> target, Map<ResourceLocation, Double> multipliers) {
            for (Map.Entry<ResourceLocation, Double> entry : multipliers.entrySet()) {
                target.put(Objects.requireNonNull(entry.getKey(), "damage key"), validateMultiplier(entry.getValue()));
            }
        }

        private static void putAllFlat(Map<ResourceLocation, Double> target, Map<ResourceLocation, Double> amounts) {
            for (Map.Entry<ResourceLocation, Double> entry : amounts.entrySet()) {
                target.put(Objects.requireNonNull(entry.getKey(), "damage key"), validateFlat(entry.getValue()));
            }
        }

        private static double validateMultiplier(double multiplier) {
            if (!Double.isFinite(multiplier) || multiplier < 0.0D) {
                throw new IllegalArgumentException("Damage multipliers must be finite and >= 0.0");
            }
            return multiplier;
        }

        private static double validateFlat(double amount) {
            if (!Double.isFinite(amount)) {
                throw new IllegalArgumentException("Flat damage additions must be finite");
            }
            return amount;
        }
    }
}
