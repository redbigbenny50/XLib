package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class AbilityControlProfile {
    private final ResourceLocation id;
    private final Component displayName;
    private final int priority;
    private final boolean playerEditable;
    private final List<AbilityControlBinding> bindings;

    private AbilityControlProfile(
            ResourceLocation id,
            Component displayName,
            int priority,
            boolean playerEditable,
            List<AbilityControlBinding> bindings
    ) {
        this.id = id;
        this.displayName = displayName.copy();
        this.priority = priority;
        this.playerEditable = playerEditable;
        this.bindings = List.copyOf(bindings);
    }

    public static Builder builder(ResourceLocation id, Component displayName) {
        return new Builder(id, displayName);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Component displayName() {
        return this.displayName.copy();
    }

    public int priority() {
        return this.priority;
    }

    public boolean playerEditable() {
        return this.playerEditable;
    }

    public List<AbilityControlBinding> bindings() {
        return this.bindings;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Component displayName;
        private int priority;
        private boolean playerEditable;
        private final List<AbilityControlBinding> bindings = new ArrayList<>();

        private Builder(ResourceLocation id, Component displayName) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder playerEditable() {
            this.playerEditable = true;
            return this;
        }

        public Builder bind(AbilityControlTrigger trigger, AbilityControlAction action) {
            this.bindings.add(new AbilityControlBinding(trigger, action));
            return this;
        }

        public AbilityControlProfile build() {
            return new AbilityControlProfile(this.id, this.displayName, this.priority, this.playerEditable, this.bindings);
        }
    }
}
