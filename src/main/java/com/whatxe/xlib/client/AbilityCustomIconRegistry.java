package com.whatxe.xlib.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class AbilityCustomIconRegistry {
    private static final Map<ResourceLocation, AbilityCustomIconRenderer> RENDERERS = new LinkedHashMap<>();

    private AbilityCustomIconRegistry() {}

    public static void register(ResourceLocation rendererId, AbilityCustomIconRenderer renderer) {
        RENDERERS.put(rendererId, renderer);
    }

    public static Optional<AbilityCustomIconRenderer> find(ResourceLocation rendererId) {
        return Optional.ofNullable(RENDERERS.get(rendererId));
    }
}

