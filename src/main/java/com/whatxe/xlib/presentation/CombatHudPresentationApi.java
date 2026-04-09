package com.whatxe.xlib.presentation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class CombatHudPresentationApi {
    private static final Map<ResourceLocation, CombatHudPresentation> PRESENTATIONS = new LinkedHashMap<>();
    private static ResourceLocation activePresentationId;

    private CombatHudPresentationApi() {}

    public static void bootstrap() {
        if (!PRESENTATIONS.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        PRESENTATIONS.clear();
        registerBuiltIn(CombatHudPresentation.defaultPresentation());
        registerBuiltIn(CombatHudPresentation.minimalPresentation());
        activePresentationId = CombatHudPresentation.defaultPresentation().id();
    }

    public static CombatHudPresentation register(CombatHudPresentation presentation) {
        Objects.requireNonNull(presentation, "presentation");
        CombatHudPresentation previous = PRESENTATIONS.putIfAbsent(presentation.id(), presentation);
        if (previous != null) {
            throw new IllegalStateException("Duplicate combat HUD presentation registration: " + presentation.id());
        }
        return presentation;
    }

    public static Optional<CombatHudPresentation> unregister(ResourceLocation presentationId) {
        CombatHudPresentation removed = PRESENTATIONS.remove(Objects.requireNonNull(presentationId, "presentationId"));
        if (Objects.equals(activePresentationId, presentationId)) {
            activePresentationId = CombatHudPresentation.defaultPresentation().id();
        }
        if (PRESENTATIONS.isEmpty()) {
            restoreDefaults();
        }
        return Optional.ofNullable(removed);
    }

    public static void activate(ResourceLocation presentationId) {
        if (!PRESENTATIONS.containsKey(Objects.requireNonNull(presentationId, "presentationId"))) {
            throw new IllegalArgumentException("Unknown combat HUD presentation: " + presentationId);
        }
        activePresentationId = presentationId;
    }

    public static CombatHudPresentation active() {
        bootstrap();
        CombatHudPresentation active = PRESENTATIONS.get(activePresentationId);
        if (active != null) {
            return active;
        }
        return PRESENTATIONS.get(CombatHudPresentation.defaultPresentation().id());
    }

    public static Optional<CombatHudPresentation> find(ResourceLocation presentationId) {
        bootstrap();
        return Optional.ofNullable(PRESENTATIONS.get(presentationId));
    }

    public static Collection<CombatHudPresentation> allPresentations() {
        bootstrap();
        return java.util.List.copyOf(PRESENTATIONS.values());
    }

    private static void registerBuiltIn(CombatHudPresentation presentation) {
        PRESENTATIONS.put(presentation.id(), presentation);
    }
}
