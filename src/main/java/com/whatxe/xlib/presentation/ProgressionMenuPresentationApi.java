package com.whatxe.xlib.presentation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class ProgressionMenuPresentationApi {
    private static final Map<ResourceLocation, ProgressionMenuPresentation> PRESENTATIONS = new LinkedHashMap<>();
    private static ResourceLocation activePresentationId;

    private ProgressionMenuPresentationApi() {}

    public static void bootstrap() {
        if (!PRESENTATIONS.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        PRESENTATIONS.clear();
        registerBuiltIn(ProgressionMenuPresentation.defaultPresentation());
        activePresentationId = ProgressionMenuPresentation.defaultPresentation().id();
    }

    public static ProgressionMenuPresentation register(ProgressionMenuPresentation presentation) {
        Objects.requireNonNull(presentation, "presentation");
        ProgressionMenuPresentation previous = PRESENTATIONS.putIfAbsent(presentation.id(), presentation);
        if (previous != null) {
            throw new IllegalStateException("Duplicate progression menu presentation registration: " + presentation.id());
        }
        return presentation;
    }

    public static Optional<ProgressionMenuPresentation> unregister(ResourceLocation presentationId) {
        ProgressionMenuPresentation removed = PRESENTATIONS.remove(Objects.requireNonNull(presentationId, "presentationId"));
        if (Objects.equals(activePresentationId, presentationId)) {
            activePresentationId = ProgressionMenuPresentation.defaultPresentation().id();
        }
        if (PRESENTATIONS.isEmpty()) {
            restoreDefaults();
        }
        return Optional.ofNullable(removed);
    }

    public static void activate(ResourceLocation presentationId) {
        if (!PRESENTATIONS.containsKey(Objects.requireNonNull(presentationId, "presentationId"))) {
            throw new IllegalArgumentException("Unknown progression menu presentation: " + presentationId);
        }
        activePresentationId = presentationId;
    }

    public static ProgressionMenuPresentation active() {
        bootstrap();
        ProgressionMenuPresentation active = PRESENTATIONS.get(activePresentationId);
        if (active != null) {
            return active;
        }
        return PRESENTATIONS.get(ProgressionMenuPresentation.defaultPresentation().id());
    }

    public static Optional<ProgressionMenuPresentation> find(ResourceLocation presentationId) {
        bootstrap();
        return Optional.ofNullable(PRESENTATIONS.get(presentationId));
    }

    public static Collection<ProgressionMenuPresentation> allPresentations() {
        bootstrap();
        return java.util.List.copyOf(PRESENTATIONS.values());
    }

    private static void registerBuiltIn(ProgressionMenuPresentation presentation) {
        PRESENTATIONS.put(presentation.id(), presentation);
    }
}
