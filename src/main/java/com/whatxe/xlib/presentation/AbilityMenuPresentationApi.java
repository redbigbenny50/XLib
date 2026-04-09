package com.whatxe.xlib.presentation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class AbilityMenuPresentationApi {
    private static final Map<ResourceLocation, AbilityMenuPresentation> PRESENTATIONS = new LinkedHashMap<>();
    private static ResourceLocation activePresentationId;

    private AbilityMenuPresentationApi() {}

    public static void bootstrap() {
        if (!PRESENTATIONS.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        PRESENTATIONS.clear();
        registerBuiltIn(AbilityMenuPresentation.defaultPresentation());
        registerBuiltIn(AbilityMenuPresentation.catalogDensePresentation());
        activePresentationId = AbilityMenuPresentation.defaultPresentation().id();
    }

    public static AbilityMenuPresentation register(AbilityMenuPresentation presentation) {
        Objects.requireNonNull(presentation, "presentation");
        AbilityMenuPresentation previous = PRESENTATIONS.putIfAbsent(presentation.id(), presentation);
        if (previous != null) {
            throw new IllegalStateException("Duplicate ability menu presentation registration: " + presentation.id());
        }
        return presentation;
    }

    public static Optional<AbilityMenuPresentation> unregister(ResourceLocation presentationId) {
        AbilityMenuPresentation removed = PRESENTATIONS.remove(Objects.requireNonNull(presentationId, "presentationId"));
        if (Objects.equals(activePresentationId, presentationId)) {
            activePresentationId = AbilityMenuPresentation.defaultPresentation().id();
        }
        if (PRESENTATIONS.isEmpty()) {
            restoreDefaults();
        }
        return Optional.ofNullable(removed);
    }

    public static void activate(ResourceLocation presentationId) {
        if (!PRESENTATIONS.containsKey(Objects.requireNonNull(presentationId, "presentationId"))) {
            throw new IllegalArgumentException("Unknown ability menu presentation: " + presentationId);
        }
        activePresentationId = presentationId;
    }

    public static AbilityMenuPresentation active() {
        bootstrap();
        AbilityMenuPresentation active = PRESENTATIONS.get(activePresentationId);
        if (active != null) {
            return active;
        }
        return PRESENTATIONS.get(AbilityMenuPresentation.defaultPresentation().id());
    }

    public static Optional<AbilityMenuPresentation> find(ResourceLocation presentationId) {
        bootstrap();
        return Optional.ofNullable(PRESENTATIONS.get(presentationId));
    }

    public static Collection<AbilityMenuPresentation> allPresentations() {
        bootstrap();
        return java.util.List.copyOf(PRESENTATIONS.values());
    }

    private static void registerBuiltIn(AbilityMenuPresentation presentation) {
        PRESENTATIONS.put(presentation.id(), presentation);
    }
}
