package com.whatxe.xlib.client;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.api.event.XLibMenuOpenEvent;
import com.whatxe.xlib.client.screen.AbilityMenuScreen;
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

public final class AbilityMenuScreenFactoryApi {
    private static final ResourceLocation BUILT_IN_SCREEN_ID =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "built_in_ability_menu");
    private static final Map<ResourceLocation, AbilityMenuScreenFactory> FACTORIES = new LinkedHashMap<>();
    private static ResourceLocation activeFactoryId = BUILT_IN_SCREEN_ID;

    private AbilityMenuScreenFactoryApi() {}

    public static void bootstrap() {
        if (!FACTORIES.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        FACTORIES.clear();
        registerBuiltIn(BUILT_IN_SCREEN_ID, AbilityMenuScreen::new);
        activeFactoryId = BUILT_IN_SCREEN_ID;
    }

    public static void register(ResourceLocation factoryId, Supplier<Screen> factory) {
        register(factoryId, context -> factory.get());
    }

    public static void register(ResourceLocation factoryId, AbilityMenuScreenFactory factory) {
        Objects.requireNonNull(factoryId, "factoryId");
        Objects.requireNonNull(factory, "factory");
        AbilityMenuScreenFactory previous = FACTORIES.putIfAbsent(factoryId, factory);
        if (previous != null) {
            throw new IllegalStateException("Duplicate ability menu screen registration: " + factoryId);
        }
    }

    public static Optional<AbilityMenuScreenFactory> unregister(ResourceLocation factoryId) {
        AbilityMenuScreenFactory removed = FACTORIES.remove(Objects.requireNonNull(factoryId, "factoryId"));
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
            throw new IllegalArgumentException("Unknown ability menu screen factory: " + factoryId);
        }
        activeFactoryId = factoryId;
    }

    public static ResourceLocation activeFactoryId() {
        bootstrap();
        return activeFactoryId;
    }

    public static Screen createActive() {
        bootstrap();
        return create(activeFactoryId, AbilityMenuScreenContext.defaultContext());
    }

    public static Screen createActive(AbilityMenuScreenContext context) {
        bootstrap();
        return create(activeFactoryId, context);
    }

    public static Screen create(ResourceLocation factoryId) {
        return create(factoryId, AbilityMenuScreenContext.defaultContext());
    }

    public static Screen create(ResourceLocation factoryId, AbilityMenuScreenContext context) {
        bootstrap();
        AbilityMenuScreenFactory factory = FACTORIES.get(Objects.requireNonNull(factoryId, "factoryId"));
        if (factory == null) {
            throw new IllegalArgumentException("Unknown ability menu screen factory: " + factoryId);
        }
        Screen screen = factory.create(Objects.requireNonNull(context, "context"));
        if (screen == null) {
            throw new IllegalStateException("Ability menu screen factory returned null: " + factoryId);
        }
        return screen;
    }

    public static void openActive(Minecraft minecraft) {
        openActive(minecraft, AbilityMenuScreenContext.defaultContext());
    }

    public static void openActive(Minecraft minecraft, AbilityMenuScreenContext context) {
        AbilityMenuScreenContext resolvedContext = Objects.requireNonNull(context, "context");
        XLibMenuOpenEvent.Pre event = new XLibMenuOpenEvent.Pre(XLibMenuOpenEvent.MenuType.ABILITY, activeFactoryId(), null);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return;
        }
        AbilityMenuSessionStateApi.setState(resolvedContext.sessionState());
        Objects.requireNonNull(minecraft, "minecraft").setScreen(createActive(resolvedContext));
        NeoForge.EVENT_BUS.post(new XLibMenuOpenEvent.Post(XLibMenuOpenEvent.MenuType.ABILITY, activeFactoryId(), null));
    }

    public static Collection<ResourceLocation> allFactoryIds() {
        bootstrap();
        return java.util.List.copyOf(FACTORIES.keySet());
    }

    private static void registerBuiltIn(ResourceLocation factoryId, AbilityMenuScreenFactory factory) {
        FACTORIES.put(factoryId, factory);
    }
}
