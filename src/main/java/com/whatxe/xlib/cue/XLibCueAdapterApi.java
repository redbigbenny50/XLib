package com.whatxe.xlib.cue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class XLibCueAdapterApi {
    private static final Map<XLibCueSurface, Map<ResourceLocation, XLibCueAdapter>> ADAPTERS =
            new EnumMap<>(XLibCueSurface.class);

    static {
        clearAdapters();
    }

    private XLibCueAdapterApi() {}

    public static void registerAdapter(ResourceLocation adapterId, XLibCueSurface surface, XLibCueAdapter adapter) {
        ADAPTERS.get(Objects.requireNonNull(surface, "surface"))
                .put(Objects.requireNonNull(adapterId, "adapterId"), Objects.requireNonNull(adapter, "adapter"));
    }

    public static Optional<XLibCueAdapter> unregisterAdapter(ResourceLocation adapterId, XLibCueSurface surface) {
        return Optional.ofNullable(
                ADAPTERS.get(Objects.requireNonNull(surface, "surface"))
                        .remove(Objects.requireNonNull(adapterId, "adapterId"))
        );
    }

    public static void clearAdapters() {
        ADAPTERS.clear();
        for (XLibCueSurface surface : XLibCueSurface.values()) {
            ADAPTERS.put(surface, new LinkedHashMap<>());
        }
    }

    public static Map<ResourceLocation, XLibCueAdapter> adaptersForSurface(XLibCueSurface surface) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(ADAPTERS.get(Objects.requireNonNull(surface, "surface"))));
    }

    public static Collection<XLibCueAdapter> adapters(XLibCueSurface surface) {
        return List.copyOf(ADAPTERS.get(Objects.requireNonNull(surface, "surface")).values());
    }

    public static List<XLibCueAdapter> allAdapters() {
        List<XLibCueAdapter> adapters = new ArrayList<>();
        for (XLibCueSurface surface : XLibCueSurface.values()) {
            adapters.addAll(ADAPTERS.get(surface).values());
        }
        return List.copyOf(adapters);
    }
}
