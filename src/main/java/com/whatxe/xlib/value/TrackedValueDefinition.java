package com.whatxe.xlib.value;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class TrackedValueDefinition {
    private final ResourceLocation id;
    private final Component displayName;
    private final double minValue;
    private final double maxValue;
    private final double startingValue;
    private final double tickDelta;
    private final int hudColor;
    private final int foodReplacementPriority;
    private final double foodReplacementIntakeScale;
    private final int foodReplacementHealThreshold;
    private final int foodReplacementHealIntervalTicks;
    private final double foodReplacementHealCost;
    private final int foodReplacementStarvationThreshold;
    private final int foodReplacementStarvationIntervalTicks;
    private final float foodReplacementStarvationDamage;

    private TrackedValueDefinition(
            ResourceLocation id,
            Component displayName,
            double minValue,
            double maxValue,
            double startingValue,
            double tickDelta,
            int hudColor,
            int foodReplacementPriority,
            double foodReplacementIntakeScale,
            int foodReplacementHealThreshold,
            int foodReplacementHealIntervalTicks,
            double foodReplacementHealCost,
            int foodReplacementStarvationThreshold,
            int foodReplacementStarvationIntervalTicks,
            float foodReplacementStarvationDamage
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.minValue = validateFinite(minValue, "minValue");
        this.maxValue = validateFinite(maxValue, "maxValue");
        if (this.maxValue <= this.minValue) {
            throw new IllegalArgumentException("maxValue must be greater than minValue");
        }
        this.startingValue = clamp(validateFinite(startingValue, "startingValue"), this.minValue, this.maxValue);
        this.tickDelta = validateFinite(tickDelta, "tickDelta");
        this.hudColor = hudColor;
        this.foodReplacementPriority = foodReplacementPriority;
        this.foodReplacementIntakeScale = validateNonNegative(foodReplacementIntakeScale, "foodReplacementIntakeScale");
        this.foodReplacementHealThreshold = clampFoodLevel(foodReplacementHealThreshold, "foodReplacementHealThreshold");
        this.foodReplacementHealIntervalTicks = validatePositive(foodReplacementHealIntervalTicks, "foodReplacementHealIntervalTicks");
        this.foodReplacementHealCost = validateNonNegative(foodReplacementHealCost, "foodReplacementHealCost");
        this.foodReplacementStarvationThreshold = clampFoodLevel(foodReplacementStarvationThreshold, "foodReplacementStarvationThreshold");
        this.foodReplacementStarvationIntervalTicks = validatePositive(foodReplacementStarvationIntervalTicks, "foodReplacementStarvationIntervalTicks");
        this.foodReplacementStarvationDamage = (float) validateNonNegative(foodReplacementStarvationDamage, "foodReplacementStarvationDamage");
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Component displayName() {
        return this.displayName;
    }

    public double minValue() {
        return this.minValue;
    }

    public double maxValue() {
        return this.maxValue;
    }

    public double startingValue() {
        return this.startingValue;
    }

    public double tickDelta() {
        return this.tickDelta;
    }

    public int hudColor() {
        return this.hudColor;
    }

    public int foodReplacementPriority() {
        return this.foodReplacementPriority;
    }

    public double foodReplacementIntakeScale() {
        return this.foodReplacementIntakeScale;
    }

    public int foodReplacementHealThreshold() {
        return this.foodReplacementHealThreshold;
    }

    public int foodReplacementHealIntervalTicks() {
        return this.foodReplacementHealIntervalTicks;
    }

    public double foodReplacementHealCost() {
        return this.foodReplacementHealCost;
    }

    public int foodReplacementStarvationThreshold() {
        return this.foodReplacementStarvationThreshold;
    }

    public int foodReplacementStarvationIntervalTicks() {
        return this.foodReplacementStarvationIntervalTicks;
    }

    public float foodReplacementStarvationDamage() {
        return this.foodReplacementStarvationDamage;
    }

    public int foodReplacementLevel(double amount) {
        double span = this.maxValue - this.minValue;
        if (span <= 0.0D) {
            return 0;
        }
        double normalized = (clamp(amount) - this.minValue) / span;
        return Math.max(0, Math.min(20, (int) Math.floor((normalized * 20.0D) + 1.0E-9D)));
    }

    public double clamp(double value) {
        return clamp(value, this.minValue, this.maxValue);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private Component displayName;
        private double minValue = 0.0D;
        private double maxValue = 100.0D;
        private Double startingValue;
        private double tickDelta;
        private int hudColor = 0xFF8AD8FF;
        private int foodReplacementPriority;
        private double foodReplacementIntakeScale;
        private int foodReplacementHealThreshold = 18;
        private int foodReplacementHealIntervalTicks = 80;
        private double foodReplacementHealCost = 1.0D;
        private int foodReplacementStarvationThreshold;
        private int foodReplacementStarvationIntervalTicks = 80;
        private float foodReplacementStarvationDamage = 1.0F;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Component.literal(humanize(id));
        }

        public Builder displayName(Component displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder minValue(double minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder minimum(double minValue) {
            return minValue(minValue);
        }

        public Builder maxValue(double maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder maximum(double maxValue) {
            return maxValue(maxValue);
        }

        public Builder startingValue(double startingValue) {
            this.startingValue = startingValue;
            return this;
        }

        public Builder tickDelta(double tickDelta) {
            this.tickDelta = tickDelta;
            return this;
        }

        public Builder hudColor(int hudColor) {
            this.hudColor = hudColor;
            return this;
        }

        public Builder foodReplacementPriority(int foodReplacementPriority) {
            this.foodReplacementPriority = foodReplacementPriority;
            return this;
        }

        public Builder foodReplacementIntakeScale(double foodReplacementIntakeScale) {
            this.foodReplacementIntakeScale = foodReplacementIntakeScale;
            return this;
        }

        public Builder foodReplacementHealThreshold(int foodReplacementHealThreshold) {
            this.foodReplacementHealThreshold = foodReplacementHealThreshold;
            return this;
        }

        public Builder foodReplacementHealIntervalTicks(int foodReplacementHealIntervalTicks) {
            this.foodReplacementHealIntervalTicks = foodReplacementHealIntervalTicks;
            return this;
        }

        public Builder foodReplacementHealCost(double foodReplacementHealCost) {
            this.foodReplacementHealCost = foodReplacementHealCost;
            return this;
        }

        public Builder foodReplacementStarvationThreshold(int foodReplacementStarvationThreshold) {
            this.foodReplacementStarvationThreshold = foodReplacementStarvationThreshold;
            return this;
        }

        public Builder foodReplacementStarvationIntervalTicks(int foodReplacementStarvationIntervalTicks) {
            this.foodReplacementStarvationIntervalTicks = foodReplacementStarvationIntervalTicks;
            return this;
        }

        public Builder foodReplacementStarvationDamage(float foodReplacementStarvationDamage) {
            this.foodReplacementStarvationDamage = foodReplacementStarvationDamage;
            return this;
        }

        public TrackedValueDefinition build() {
            double resolvedStartingValue = this.startingValue != null ? this.startingValue : this.minValue;
            return new TrackedValueDefinition(
                    this.id,
                    this.displayName,
                    this.minValue,
                    this.maxValue,
                    resolvedStartingValue,
                    this.tickDelta,
                    this.hudColor,
                    this.foodReplacementPriority,
                    this.foodReplacementIntakeScale,
                    this.foodReplacementHealThreshold,
                    this.foodReplacementHealIntervalTicks,
                    this.foodReplacementHealCost,
                    this.foodReplacementStarvationThreshold,
                    this.foodReplacementStarvationIntervalTicks,
                    this.foodReplacementStarvationDamage
            );
        }

        private static String humanize(ResourceLocation id) {
            String value = id.getPath();
            StringBuilder builder = new StringBuilder(value.length());
            boolean capitalizeNext = true;
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                if (character == '_' || character == '-' || character == '/') {
                    builder.append(' ');
                    capitalizeNext = true;
                    continue;
                }
                builder.append(capitalizeNext ? Character.toUpperCase(character) : character);
                capitalizeNext = false;
            }
            return builder.toString();
        }
    }

    private static double validateFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return value;
    }

    private static int validatePositive(int value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be > 0");
        }
        return value;
    }

    private static double validateNonNegative(double value, String label) {
        double finiteValue = validateFinite(value, label);
        if (finiteValue < 0.0D) {
            throw new IllegalArgumentException(label + " must be >= 0");
        }
        return finiteValue;
    }

    private static int clampFoodLevel(int value, String label) {
        if (value < 0 || value > 20) {
            throw new IllegalArgumentException(label + " must be between 0 and 20");
        }
        return value;
    }

    private static double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }
}
