package com.whatxe.xlib.cue;

import com.whatxe.xlib.XLib;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record XLibCueRouteProfile(
        ResourceLocation id,
        Map<XLibRuntimeCueType, Set<XLibCueSurface>> routes
) {
    public XLibCueRouteProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(routes, "routes");
        EnumMap<XLibRuntimeCueType, Set<XLibCueSurface>> normalizedRoutes = new EnumMap<>(XLibRuntimeCueType.class);
        for (XLibRuntimeCueType type : XLibRuntimeCueType.values()) {
            Set<XLibCueSurface> surfaces = routes.getOrDefault(type, Set.of());
            EnumSet<XLibCueSurface> orderedSurfaces = surfaces.isEmpty()
                    ? EnumSet.noneOf(XLibCueSurface.class)
                    : EnumSet.copyOf(surfaces);
            normalizedRoutes.put(type, Collections.unmodifiableSet(orderedSurfaces));
        }
        routes = Map.copyOf(normalizedRoutes);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static XLibCueRouteProfile defaultProfile() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "default_cue_routes"))
                .route(XLibRuntimeCueType.ACTIVATION_START, XLibCueSurface.PLAYER_BODY_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.ACTIVATION_FAIL, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.CHARGE_PROGRESS, XLibCueSurface.MODEL_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.RELEASE, XLibCueSurface.PLAYER_BODY_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.HIT_CONFIRM, XLibCueSurface.MODEL_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.INTERRUPT, XLibCueSurface.PLAYER_BODY_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.STATE_ENTER, XLibCueSurface.PLAYER_BODY_ANIMATION, XLibCueSurface.MODEL_ANIMATION)
                .route(XLibRuntimeCueType.STATE_EXIT, XLibCueSurface.PLAYER_BODY_ANIMATION, XLibCueSurface.MODEL_ANIMATION)
                .build();
    }

    public static XLibCueRouteProfile effectsHeavyProfile() {
        return builder(ResourceLocation.fromNamespaceAndPath(XLib.MODID, "effects_heavy_cue_routes"))
                .route(XLibRuntimeCueType.ACTIVATION_START, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.ACTIVATION_FAIL, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.CHARGE_PROGRESS, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.RELEASE, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.HIT_CONFIRM, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.INTERRUPT, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.STATE_ENTER, XLibCueSurface.MODEL_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .route(XLibRuntimeCueType.STATE_EXIT, XLibCueSurface.MODEL_ANIMATION, XLibCueSurface.EFFECT_PLAYBACK)
                .build();
    }

    public Set<XLibCueSurface> surfacesFor(XLibRuntimeCueType cueType) {
        return this.routes.getOrDefault(Objects.requireNonNull(cueType, "cueType"), Set.of());
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Map<XLibRuntimeCueType, Set<XLibCueSurface>> routes = new EnumMap<>(XLibRuntimeCueType.class);

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder route(XLibRuntimeCueType cueType, XLibCueSurface... surfaces) {
            Objects.requireNonNull(cueType, "cueType");
            EnumSet<XLibCueSurface> resolvedSurfaces = EnumSet.noneOf(XLibCueSurface.class);
            for (XLibCueSurface surface : Objects.requireNonNull(surfaces, "surfaces")) {
                resolvedSurfaces.add(Objects.requireNonNull(surface, "surface"));
            }
            this.routes.put(cueType, Set.copyOf(resolvedSurfaces));
            return this;
        }

        public XLibCueRouteProfile build() {
            return new XLibCueRouteProfile(this.id, this.routes);
        }
    }
}
