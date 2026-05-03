package com.whatxe.xlib.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class TrackedValueHudApi {
    private static final Map<ResourceLocation, TrackedValueHudRenderer> RENDERERS = new LinkedHashMap<>();

    private TrackedValueHudApi() {}

    public static void registerRenderer(ResourceLocation valueId, TrackedValueHudRenderer renderer) {
        if (valueId == null) {
            throw new IllegalArgumentException("valueId must not be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must not be null");
        }
        RENDERERS.put(valueId, renderer);
    }

    public static Optional<TrackedValueHudRenderer> findRenderer(ResourceLocation valueId) {
        return Optional.ofNullable(RENDERERS.get(valueId));
    }

    public static void clearRenderers() {
        RENDERERS.clear();
    }
}
