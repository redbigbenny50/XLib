package com.whatxe.xlib.client;

import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class AbilityContainerLayoutApi {
    private static final Map<ResourceLocation, AbilityContainerLayoutDefinition> LAYOUTS = new LinkedHashMap<>();

    private AbilityContainerLayoutApi() {}

    public static void bootstrap() {
        if (!LAYOUTS.isEmpty()) {
            return;
        }
        restoreDefaults();
    }

    public static void restoreDefaults() {
        LAYOUTS.clear();
        registerBuiltIn(AbilityContainerLayoutDefinition.builder(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID)
                .layoutMode(AbilitySlotLayoutMode.STRIP)
                .anchor(AbilitySlotLayoutAnchor.BOTTOM_CENTER)
                .showPageTabs(false)
                .build());
    }

    public static AbilityContainerLayoutDefinition register(AbilityContainerLayoutDefinition definition) {
        XLibRegistryGuard.ensureMutable("ability_container_layouts");
        AbilityContainerLayoutDefinition resolvedDefinition = Objects.requireNonNull(definition, "definition");
        if (!AbilitySlotContainerApi.PRIMARY_CONTAINER_ID.equals(resolvedDefinition.containerId())) {
            throw new IllegalStateException("Auxiliary slot layouts are no longer supported: " + resolvedDefinition.containerId());
        }
        AbilityContainerLayoutDefinition previous = LAYOUTS.putIfAbsent(resolvedDefinition.containerId(), resolvedDefinition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate ability container layout registration: " + resolvedDefinition.containerId());
        }
        return resolvedDefinition;
    }

    public static Optional<AbilityContainerLayoutDefinition> unregister(ResourceLocation containerId) {
        XLibRegistryGuard.ensureMutable("ability_container_layouts");
        if (AbilitySlotContainerApi.PRIMARY_CONTAINER_ID.equals(containerId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAYOUTS.remove(Objects.requireNonNull(containerId, "containerId")));
    }

    public static Optional<AbilityContainerLayoutDefinition> find(ResourceLocation containerId) {
        bootstrap();
        if (!AbilitySlotContainerApi.PRIMARY_CONTAINER_ID.equals(containerId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(LAYOUTS.get(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID));
    }

    public static AbilityContainerLayoutDefinition resolvedLayout(ResourceLocation containerId) {
        return find(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID)
                .orElseGet(() -> AbilityContainerLayoutDefinition.builder(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID).build());
    }

    public static Collection<AbilityContainerLayoutDefinition> allLayouts() {
        bootstrap();
        return java.util.List.copyOf(LAYOUTS.values());
    }

    private static void registerBuiltIn(AbilityContainerLayoutDefinition definition) {
        LAYOUTS.put(definition.containerId(), definition);
    }
}
