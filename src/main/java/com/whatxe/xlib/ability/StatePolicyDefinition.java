package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class StatePolicyDefinition {
    private final ResourceLocation id;
    private final double cooldownTickRateMultiplier;
    private final Set<AbilitySelector> lockedAbilities;
    private final Set<AbilitySelector> silencedAbilities;
    private final Set<AbilitySelector> suppressedAbilities;

    private StatePolicyDefinition(
            ResourceLocation id,
            double cooldownTickRateMultiplier,
            Set<AbilitySelector> lockedAbilities,
            Set<AbilitySelector> silencedAbilities,
            Set<AbilitySelector> suppressedAbilities
    ) {
        this.id = id;
        this.cooldownTickRateMultiplier = cooldownTickRateMultiplier;
        this.lockedAbilities = copySelectors(lockedAbilities);
        this.silencedAbilities = copySelectors(silencedAbilities);
        this.suppressedAbilities = copySelectors(suppressedAbilities);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public double cooldownTickRateMultiplier() {
        return this.cooldownTickRateMultiplier;
    }

    public Set<AbilitySelector> lockedAbilities() {
        return this.lockedAbilities;
    }

    public Set<AbilitySelector> silencedAbilities() {
        return this.silencedAbilities;
    }

    public Set<AbilitySelector> suppressedAbilities() {
        return this.suppressedAbilities;
    }

    public boolean isEmpty() {
        return this.cooldownTickRateMultiplier == 1.0D
                && this.lockedAbilities.isEmpty()
                && this.silencedAbilities.isEmpty()
                && this.suppressedAbilities.isEmpty();
    }

    private static Set<AbilitySelector> copySelectors(Collection<AbilitySelector> selectors) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(selectors));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private double cooldownTickRateMultiplier = 1.0D;
        private final Set<AbilitySelector> lockedAbilities = new LinkedHashSet<>();
        private final Set<AbilitySelector> silencedAbilities = new LinkedHashSet<>();
        private final Set<AbilitySelector> suppressedAbilities = new LinkedHashSet<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder cooldownTickRateMultiplier(double multiplier) {
            if (!(multiplier > 0.0D)) {
                throw new IllegalArgumentException("cooldownTickRateMultiplier must be positive");
            }
            this.cooldownTickRateMultiplier = multiplier;
            return this;
        }

        public Builder lock(AbilitySelector selector) {
            this.lockedAbilities.add(Objects.requireNonNull(selector, "selector"));
            return this;
        }

        public Builder lock(Collection<AbilitySelector> selectors) {
            selectors.stream().filter(Objects::nonNull).forEach(this.lockedAbilities::add);
            return this;
        }

        public Builder silence(AbilitySelector selector) {
            this.silencedAbilities.add(Objects.requireNonNull(selector, "selector"));
            return this;
        }

        public Builder silence(Collection<AbilitySelector> selectors) {
            selectors.stream().filter(Objects::nonNull).forEach(this.silencedAbilities::add);
            return this;
        }

        public Builder suppress(AbilitySelector selector) {
            this.suppressedAbilities.add(Objects.requireNonNull(selector, "selector"));
            return this;
        }

        public Builder suppress(Collection<AbilitySelector> selectors) {
            selectors.stream().filter(Objects::nonNull).forEach(this.suppressedAbilities::add);
            return this;
        }

        public Builder seal(AbilitySelector selector) {
            AbilitySelector resolvedSelector = Objects.requireNonNull(selector, "selector");
            this.lockedAbilities.add(resolvedSelector);
            this.silencedAbilities.add(resolvedSelector);
            return this;
        }

        public Builder seal(Collection<AbilitySelector> selectors) {
            selectors.stream().filter(Objects::nonNull).forEach(this::seal);
            return this;
        }

        public StatePolicyDefinition build() {
            return new StatePolicyDefinition(
                    this.id,
                    this.cooldownTickRateMultiplier,
                    this.lockedAbilities,
                    this.silencedAbilities,
                    this.suppressedAbilities
            );
        }
    }
}
