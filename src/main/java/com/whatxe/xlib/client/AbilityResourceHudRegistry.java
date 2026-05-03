package com.whatxe.xlib.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class AbilityResourceHudRegistry {
    private static final Map<ResourceLocation, AbilityResourceHudRegistration> REGISTRATIONS = new LinkedHashMap<>();

    private AbilityResourceHudRegistry() {}

    public static void register(ResourceLocation resourceId, AbilityResourceHudRenderer renderer) {
        register(resourceId, renderer, AbilityResourceHudLayout.defaultLayout());
    }

    public static void register(ResourceLocation resourceId, AbilityResourceHudLayout layout) {
        register(resourceId, null, layout);
    }

    public static void register(
            ResourceLocation resourceId,
            AbilityResourceHudRenderer renderer,
            AbilityResourceHudLayout layout
    ) {
        REGISTRATIONS.put(resourceId, new AbilityResourceHudRegistration(renderer, layout));
    }

    public static Optional<AbilityResourceHudRegistration> find(ResourceLocation resourceId) {
        return Optional.ofNullable(REGISTRATIONS.get(resourceId));
    }
}

