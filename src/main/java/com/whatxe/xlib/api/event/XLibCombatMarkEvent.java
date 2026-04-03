package com.whatxe.xlib.api.event;

import com.whatxe.xlib.combat.CombatMarkState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

public sealed class XLibCombatMarkEvent extends Event permits XLibCombatMarkEvent.Applied, XLibCombatMarkEvent.Refreshed, XLibCombatMarkEvent.Expired, XLibCombatMarkEvent.Removed {
    private final LivingEntity entity;
    private final ResourceLocation markId;
    private final @Nullable CombatMarkState previousState;
    private final @Nullable CombatMarkState currentState;

    protected XLibCombatMarkEvent(
            LivingEntity entity,
            ResourceLocation markId,
            @Nullable CombatMarkState previousState,
            @Nullable CombatMarkState currentState
    ) {
        this.entity = entity;
        this.markId = markId;
        this.previousState = previousState;
        this.currentState = currentState;
    }

    public LivingEntity entity() {
        return this.entity;
    }

    public ResourceLocation markId() {
        return this.markId;
    }

    public @Nullable CombatMarkState previousState() {
        return this.previousState;
    }

    public @Nullable CombatMarkState currentState() {
        return this.currentState;
    }

    public static final class Applied extends XLibCombatMarkEvent {
        public Applied(LivingEntity entity, ResourceLocation markId, @Nullable CombatMarkState previousState, @Nullable CombatMarkState currentState) {
            super(entity, markId, previousState, currentState);
        }
    }

    public static final class Refreshed extends XLibCombatMarkEvent {
        public Refreshed(LivingEntity entity, ResourceLocation markId, @Nullable CombatMarkState previousState, @Nullable CombatMarkState currentState) {
            super(entity, markId, previousState, currentState);
        }
    }

    public static final class Expired extends XLibCombatMarkEvent {
        public Expired(LivingEntity entity, ResourceLocation markId, @Nullable CombatMarkState previousState, @Nullable CombatMarkState currentState) {
            super(entity, markId, previousState, currentState);
        }
    }

    public static final class Removed extends XLibCombatMarkEvent {
        public Removed(LivingEntity entity, ResourceLocation markId, @Nullable CombatMarkState previousState, @Nullable CombatMarkState currentState) {
            super(entity, markId, previousState, currentState);
        }
    }
}
