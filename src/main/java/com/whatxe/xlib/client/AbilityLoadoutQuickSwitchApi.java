package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityLoadoutFeatureApi;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class AbilityLoadoutQuickSwitchApi {
    private static final Map<ResourceLocation, AbilityLoadoutQuickSwitchHandler> HANDLERS = new LinkedHashMap<>();

    private AbilityLoadoutQuickSwitchApi() {}

    public static void register(ResourceLocation handlerId, AbilityLoadoutQuickSwitchHandler handler) {
        Objects.requireNonNull(handlerId, "handlerId");
        Objects.requireNonNull(handler, "handler");
        AbilityLoadoutQuickSwitchHandler previous = HANDLERS.putIfAbsent(handlerId, handler);
        if (previous != null) {
            throw new IllegalStateException("Duplicate loadout quick-switch handler registration: " + handlerId);
        }
    }

    public static Optional<AbilityLoadoutQuickSwitchHandler> unregister(ResourceLocation handlerId) {
        return Optional.ofNullable(HANDLERS.remove(Objects.requireNonNull(handlerId, "handlerId")));
    }

    public static Optional<AbilityLoadoutQuickSwitchHandler> find(ResourceLocation handlerId) {
        return Optional.ofNullable(HANDLERS.get(Objects.requireNonNull(handlerId, "handlerId")));
    }

    public static Collection<ResourceLocation> allHandlerIds() {
        return java.util.List.copyOf(HANDLERS.keySet());
    }

    public static void clearHandlers() {
        HANDLERS.clear();
    }

    public static boolean handleActive(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        if (minecraft.player == null || !AbilityLoadoutFeatureApi.decision(minecraft.player).quickSwitchEnabled()) {
            return false;
        }
        for (AbilityLoadoutQuickSwitchHandler handler : HANDLERS.values()) {
            if (handler.handle(minecraft)) {
                return true;
            }
        }
        return false;
    }
}
