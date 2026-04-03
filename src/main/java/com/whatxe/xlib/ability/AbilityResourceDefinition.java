package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AbilityResourceDefinition {
    @FunctionalInterface
    public interface ResourceTicker {
        AbilityData tick(ServerPlayer player, AbilityData data, AbilityResourceDefinition resource);
    }

    @FunctionalInterface
    public interface ResourceVisibility {
        boolean shouldRender(Player player, AbilityData data, AbilityResourceDefinition resource);
    }

    private static final ResourceTicker NOOP_TICKER = (player, data, resource) -> data;
    private static final ResourceVisibility DEFAULT_VISIBILITY =
            (player, data, resource) -> data.resourceAmount(resource.id()) > 0
                    || data.resourceRegenDelay(resource.id()) > 0
                    || data.resourceDecayDelay(resource.id()) > 0;

    private final ResourceLocation id;
    private final int maxAmount;
    private final int startingAmount;
    private final int regenAmount;
    private final int regenIntervalTicks;
    private final int color;
    private final int overflowAmount;
    private final boolean shieldStyle;
    private final List<AbilityResourceBehavior> behaviors;
    private final ResourceTicker ticker;
    private final ResourceVisibility visibility;

    private AbilityResourceDefinition(
            ResourceLocation id,
            int maxAmount,
            int startingAmount,
            int regenAmount,
            int regenIntervalTicks,
            int color,
            int overflowAmount,
            boolean shieldStyle,
            List<AbilityResourceBehavior> behaviors,
            ResourceTicker ticker,
            ResourceVisibility visibility
    ) {
        this.id = id;
        this.maxAmount = maxAmount;
        this.startingAmount = startingAmount;
        this.regenAmount = regenAmount;
        this.regenIntervalTicks = regenIntervalTicks;
        this.color = color;
        this.overflowAmount = overflowAmount;
        this.shieldStyle = shieldStyle;
        this.behaviors = List.copyOf(behaviors);
        this.ticker = ticker;
        this.visibility = visibility;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public int maxAmount() {
        return this.maxAmount;
    }

    public int startingAmount() {
        return this.startingAmount;
    }

    public int regenAmount() {
        return this.regenAmount;
    }

    public int regenIntervalTicks() {
        return this.regenIntervalTicks;
    }

    public int color() {
        return this.color;
    }

    public int overflowAmount() {
        return this.overflowAmount;
    }

    public int totalCapacity() {
        return this.maxAmount + this.overflowAmount;
    }

    public boolean shieldStyle() {
        return this.shieldStyle;
    }

    public List<AbilityResourceBehavior> behaviors() {
        return this.behaviors;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey());
    }

    public String translationKey() {
        return "resource." + this.id.getNamespace() + "." + this.id.getPath();
    }

    public AbilityData tick(ServerPlayer player, AbilityData data) {
        AbilityData updatedData = this.ticker.tick(player, data, this);
        for (AbilityResourceBehavior behavior : this.behaviors) {
            updatedData = behavior.onTick(player, updatedData, this);
        }
        return updatedData;
    }

    public boolean shouldRender(Player player, AbilityData data) {
        return this.visibility.shouldRender(player, data, this);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private int maxAmount = 100;
        private Integer startingAmount;
        private int regenAmount;
        private int regenIntervalTicks = 20;
        private int color = 0xFF8BCF9A;
        private int overflowAmount;
        private boolean shieldStyle;
        private final List<AbilityResourceBehavior> behaviors = new ArrayList<>();
        private ResourceTicker ticker = NOOP_TICKER;
        private ResourceVisibility visibility = DEFAULT_VISIBILITY;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder maxAmount(int maxAmount) {
            this.maxAmount = maxAmount;
            return this;
        }

        public Builder startingAmount(int startingAmount) {
            this.startingAmount = startingAmount;
            return this;
        }

        public Builder regeneration(int regenAmount, int regenIntervalTicks) {
            this.regenAmount = regenAmount;
            this.regenIntervalTicks = regenIntervalTicks;
            return this;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder overflow(int overflowAmount) {
            this.overflowAmount = overflowAmount;
            return this;
        }

        public Builder shieldStyle() {
            this.shieldStyle = true;
            return this;
        }

        public Builder behavior(AbilityResourceBehavior behavior) {
            this.behaviors.add(Objects.requireNonNull(behavior, "behavior"));
            return this;
        }

        public Builder ticker(ResourceTicker ticker) {
            this.ticker = Objects.requireNonNull(ticker, "ticker");
            return this;
        }

        public Builder visibility(ResourceVisibility visibility) {
            this.visibility = Objects.requireNonNull(visibility, "visibility");
            return this;
        }

        public AbilityResourceDefinition build() {
            if (this.maxAmount <= 0) {
                throw new IllegalStateException("maxAmount must be positive");
            }

            int resolvedStartingAmount = this.startingAmount != null ? this.startingAmount : this.maxAmount;
            if (resolvedStartingAmount < 0 || resolvedStartingAmount > this.maxAmount + this.overflowAmount) {
                throw new IllegalStateException("startingAmount must be between 0 and total capacity");
            }
            if (this.regenAmount < 0) {
                throw new IllegalStateException("regenAmount cannot be negative");
            }
            if (this.regenAmount > 0 && this.regenIntervalTicks <= 0) {
                throw new IllegalStateException("regenIntervalTicks must be positive when regeneration is enabled");
            }
            if (this.overflowAmount < 0) {
                throw new IllegalStateException("overflowAmount cannot be negative");
            }

            return new AbilityResourceDefinition(
                    this.id,
                    this.maxAmount,
                    resolvedStartingAmount,
                    this.regenAmount,
                    this.regenIntervalTicks,
                    this.color,
                    this.overflowAmount,
                    this.shieldStyle,
                    this.behaviors,
                    this.ticker,
                    this.visibility
            );
        }
    }
}

