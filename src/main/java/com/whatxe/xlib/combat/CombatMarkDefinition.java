package com.whatxe.xlib.combat;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class CombatMarkDefinition {
    private final ResourceLocation id;
    private final int maxStacks;
    private final boolean refreshDurationOnReapply;
    private final boolean addValueOnReapply;

    private CombatMarkDefinition(
            ResourceLocation id,
            int maxStacks,
            boolean refreshDurationOnReapply,
            boolean addValueOnReapply
    ) {
        this.id = id;
        this.maxStacks = maxStacks;
        this.refreshDurationOnReapply = refreshDurationOnReapply;
        this.addValueOnReapply = addValueOnReapply;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public int maxStacks() {
        return this.maxStacks;
    }

    public boolean refreshDurationOnReapply() {
        return this.refreshDurationOnReapply;
    }

    public boolean addValueOnReapply() {
        return this.addValueOnReapply;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private int maxStacks = 1;
        private boolean refreshDurationOnReapply = true;
        private boolean addValueOnReapply = true;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder maxStacks(int maxStacks) {
            this.maxStacks = maxStacks;
            return this;
        }

        public Builder refreshDurationOnReapply(boolean refreshDurationOnReapply) {
            this.refreshDurationOnReapply = refreshDurationOnReapply;
            return this;
        }

        public Builder addValueOnReapply(boolean addValueOnReapply) {
            this.addValueOnReapply = addValueOnReapply;
            return this;
        }

        public CombatMarkDefinition build() {
            if (this.maxStacks <= 0) {
                throw new IllegalStateException("maxStacks must be positive");
            }
            return new CombatMarkDefinition(
                    this.id,
                    this.maxStacks,
                    this.refreshDurationOnReapply,
                    this.addValueOnReapply
            );
        }
    }
}
