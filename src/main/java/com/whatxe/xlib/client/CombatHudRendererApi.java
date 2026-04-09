package com.whatxe.xlib.client;

import com.whatxe.xlib.XLib;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class CombatHudRendererApi {
    private static final ResourceLocation BUILT_IN_RENDERER_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "built_in_combat_hud_renderer");
    private static final Map<ResourceLocation, CombatHudRenderer> RENDERERS = new LinkedHashMap<>();
    private static ResourceLocation activeRendererId = BUILT_IN_RENDERER_ID;

    private CombatHudRendererApi() {}

    public static void bootstrap() {
        if (!RENDERERS.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        RENDERERS.clear();
        registerBuiltIn(BUILT_IN_RENDERER_ID, CombatBarOverlay::renderBuiltIn);
        activeRendererId = BUILT_IN_RENDERER_ID;
    }

    public static void register(ResourceLocation rendererId, CombatHudRenderer renderer) {
        Objects.requireNonNull(rendererId, "rendererId");
        Objects.requireNonNull(renderer, "renderer");
        CombatHudRenderer previous = RENDERERS.putIfAbsent(rendererId, renderer);
        if (previous != null) {
            throw new IllegalStateException("Duplicate combat HUD renderer registration: " + rendererId);
        }
    }

    public static Optional<CombatHudRenderer> unregister(ResourceLocation rendererId) {
        CombatHudRenderer removed = RENDERERS.remove(Objects.requireNonNull(rendererId, "rendererId"));
        if (Objects.equals(activeRendererId, rendererId)) {
            activeRendererId = BUILT_IN_RENDERER_ID;
        }
        if (RENDERERS.isEmpty()) {
            restoreDefaults();
        }
        return Optional.ofNullable(removed);
    }

    public static void activate(ResourceLocation rendererId) {
        bootstrap();
        if (!RENDERERS.containsKey(Objects.requireNonNull(rendererId, "rendererId"))) {
            throw new IllegalArgumentException("Unknown combat HUD renderer: " + rendererId);
        }
        activeRendererId = rendererId;
    }

    public static ResourceLocation activeRendererId() {
        bootstrap();
        return activeRendererId;
    }

    public static CombatHudRenderer active() {
        bootstrap();
        CombatHudRenderer renderer = RENDERERS.get(activeRendererId);
        if (renderer != null) {
            return renderer;
        }
        return RENDERERS.get(BUILT_IN_RENDERER_ID);
    }

    public static Optional<CombatHudRenderer> find(ResourceLocation rendererId) {
        bootstrap();
        return Optional.ofNullable(RENDERERS.get(rendererId));
    }

    public static void renderActive(CombatHudRenderContext context) {
        active().render(Objects.requireNonNull(context, "context"));
    }

    public static Collection<ResourceLocation> allRendererIds() {
        bootstrap();
        return java.util.List.copyOf(RENDERERS.keySet());
    }

    private static void registerBuiltIn(ResourceLocation rendererId, CombatHudRenderer renderer) {
        RENDERERS.put(rendererId, renderer);
    }
}
