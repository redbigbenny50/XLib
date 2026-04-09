package com.whatxe.xlib.client;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.api.event.XLibMenuOpenEvent;
import com.whatxe.xlib.client.screen.ProgressionMenuScreen;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;

public final class ProgressionMenuScreenFactoryApi {
    private static final ResourceLocation BUILT_IN_SCREEN_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "built_in_progression_menu");
    private static final Map<ResourceLocation, ProgressionMenuScreenFactory> FACTORIES = new LinkedHashMap<>();
    private static ResourceLocation activeFactoryId = BUILT_IN_SCREEN_ID;

    private ProgressionMenuScreenFactoryApi() {}

    public static void bootstrap() {
        if (!FACTORIES.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        FACTORIES.clear();
        registerBuiltIn(BUILT_IN_SCREEN_ID, ProgressionMenuScreen::new);
        activeFactoryId = BUILT_IN_SCREEN_ID;
    }

    public static void register(ResourceLocation factoryId, Supplier<Screen> factory) {
        register(factoryId, context -> factory.get());
    }

    public static void register(ResourceLocation factoryId, ProgressionMenuScreenFactory factory) {
        Objects.requireNonNull(factoryId, "factoryId");
        Objects.requireNonNull(factory, "factory");
        ProgressionMenuScreenFactory previous = FACTORIES.putIfAbsent(factoryId, factory);
        if (previous != null) {
            throw new IllegalStateException("Duplicate progression menu screen registration: " + factoryId);
        }
    }

    public static Optional<ProgressionMenuScreenFactory> unregister(ResourceLocation factoryId) {
        ProgressionMenuScreenFactory removed = FACTORIES.remove(Objects.requireNonNull(factoryId, "factoryId"));
        if (Objects.equals(activeFactoryId, factoryId)) {
            activeFactoryId = BUILT_IN_SCREEN_ID;
        }
        if (FACTORIES.isEmpty()) {
            restoreDefaults();
        }
        return Optional.ofNullable(removed);
    }

    public static void activate(ResourceLocation factoryId) {
        bootstrap();
        if (!FACTORIES.containsKey(Objects.requireNonNull(factoryId, "factoryId"))) {
            throw new IllegalArgumentException("Unknown progression menu screen factory: " + factoryId);
        }
        activeFactoryId = factoryId;
    }

    public static ResourceLocation activeFactoryId() {
        bootstrap();
        return activeFactoryId;
    }

    public static Screen createActive() {
        bootstrap();
        return create(activeFactoryId, ProgressionMenuScreenContext.defaultContext());
    }

    public static Screen createActive(ProgressionMenuScreenContext context) {
        bootstrap();
        return create(activeFactoryId, context);
    }

    public static Screen create(ResourceLocation factoryId) {
        return create(factoryId, ProgressionMenuScreenContext.defaultContext());
    }

    public static Screen create(ResourceLocation factoryId, ProgressionMenuScreenContext context) {
        bootstrap();
        ProgressionMenuScreenFactory factory = FACTORIES.get(Objects.requireNonNull(factoryId, "factoryId"));
        if (factory == null) {
            throw new IllegalArgumentException("Unknown progression menu screen factory: " + factoryId);
        }
        Screen screen = factory.create(Objects.requireNonNull(context, "context"));
        if (screen == null) {
            throw new IllegalStateException("Progression menu screen factory returned null: " + factoryId);
        }
        return screen;
    }

    public static void openActive(Minecraft minecraft) {
        openActive(minecraft, ProgressionMenuScreenContext.defaultContext());
    }

    public static void openActive(Minecraft minecraft, ProgressionMenuScreenContext context) {
        ProgressionMenuScreenContext resolvedContext = Objects.requireNonNull(context, "context");
        XLibMenuOpenEvent.Pre event = new XLibMenuOpenEvent.Pre(XLibMenuOpenEvent.MenuType.PROGRESSION, activeFactoryId(), null);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return;
        }
        ProgressionMenuSessionStateApi.setState(resolvedContext.sessionState());
        Objects.requireNonNull(minecraft, "minecraft").setScreen(createActive(resolvedContext));
        NeoForge.EVENT_BUS.post(new XLibMenuOpenEvent.Post(XLibMenuOpenEvent.MenuType.PROGRESSION, activeFactoryId(), null));
    }

    public static Collection<ResourceLocation> allFactoryIds() {
        bootstrap();
        return java.util.List.copyOf(FACTORIES.keySet());
    }

    private static void registerBuiltIn(ResourceLocation factoryId, ProgressionMenuScreenFactory factory) {
        FACTORIES.put(factoryId, factory);
    }
}
