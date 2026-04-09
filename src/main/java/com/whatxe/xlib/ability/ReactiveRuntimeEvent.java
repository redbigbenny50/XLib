package com.whatxe.xlib.ability;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ReactiveRuntimeEvent(
        ReactiveEventType type,
        @Nullable ResourceLocation primaryId,
        @Nullable ResourceLocation relatedId,
        @Nullable AbilityEndReason endReason
) {
    public ReactiveRuntimeEvent {
        Objects.requireNonNull(type, "type");
    }

    public static ReactiveRuntimeEvent abilityActivate(ResourceLocation abilityId) {
        return new ReactiveRuntimeEvent(ReactiveEventType.ABILITY_ACTIVATE, abilityId, null, null);
    }

    public static ReactiveRuntimeEvent abilityFail(ResourceLocation abilityId) {
        return new ReactiveRuntimeEvent(ReactiveEventType.ABILITY_FAIL, abilityId, null, null);
    }

    public static ReactiveRuntimeEvent abilityEnd(ResourceLocation abilityId, AbilityEndReason reason) {
        return new ReactiveRuntimeEvent(ReactiveEventType.ABILITY_END, abilityId, null, Objects.requireNonNull(reason, "reason"));
    }

    public static ReactiveRuntimeEvent hitConfirm(@Nullable ResourceLocation abilityId) {
        return new ReactiveRuntimeEvent(ReactiveEventType.HIT_CONFIRM, abilityId, null, null);
    }

    public static ReactiveRuntimeEvent hurt() {
        return new ReactiveRuntimeEvent(ReactiveEventType.HURT, null, null, null);
    }

    public static ReactiveRuntimeEvent kill(@Nullable ResourceLocation abilityId) {
        return new ReactiveRuntimeEvent(ReactiveEventType.KILL, abilityId, null, null);
    }

    public static ReactiveRuntimeEvent jump() {
        return new ReactiveRuntimeEvent(ReactiveEventType.JUMP, null, null, null);
    }

    public static ReactiveRuntimeEvent blockBreak() {
        return new ReactiveRuntimeEvent(ReactiveEventType.BLOCK_BREAK, null, null, null);
    }

    public static ReactiveRuntimeEvent itemConsumed(ResourceLocation itemId) {
        return new ReactiveRuntimeEvent(ReactiveEventType.ITEM_CONSUMED, Objects.requireNonNull(itemId, "itemId"), null, null);
    }

    public static ReactiveRuntimeEvent armorChanged(@Nullable ResourceLocation fromItemId, @Nullable ResourceLocation toItemId) {
        return new ReactiveRuntimeEvent(ReactiveEventType.ARMOR_CHANGED, toItemId, fromItemId, null);
    }
}
