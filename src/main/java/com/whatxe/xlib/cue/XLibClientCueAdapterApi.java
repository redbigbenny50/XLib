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

public final class XLibClientCueAdapterApi {
    private static final Map<XLibCueSurface, Map<ResourceLocation, XLibClientCueAdapter>> ADAPTERS =
            new EnumMap<>(XLibCueSurface.class);

    static {
        clearAdapters();
    }

    private XLibClientCueAdapterApi() {}

    public static void registerAdapter(
            ResourceLocation adapterId,
            XLibCueSurface surface,
            XLibClientCueAdapter adapter
    ) {
        ADAPTERS.get(Objects.requireNonNull(surface, "surface"))
                .put(Objects.requireNonNull(adapterId, "adapterId"), Objects.requireNonNull(adapter, "adapter"));
    }

    public static Optional<XLibClientCueAdapter> unregisterAdapter(ResourceLocation adapterId, XLibCueSurface surface) {
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

    public static Map<ResourceLocation, XLibClientCueAdapter> adaptersForSurface(XLibCueSurface surface) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(ADAPTERS.get(Objects.requireNonNull(surface, "surface"))));
    }

    public static Collection<XLibClientCueAdapter> adapters(XLibCueSurface surface) {
        return List.copyOf(ADAPTERS.get(Objects.requireNonNull(surface, "surface")).values());
    }

    public static List<XLibClientCueAdapter> allAdapters() {
        List<XLibClientCueAdapter> adapters = new ArrayList<>();
        for (XLibCueSurface surface : XLibCueSurface.values()) {
            adapters.addAll(ADAPTERS.get(surface).values());
        }
        return List.copyOf(adapters);
    }
}
