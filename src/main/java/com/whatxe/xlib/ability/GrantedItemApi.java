package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class GrantedItemApi {
    private static final Map<ResourceLocation, GrantedItemDefinition> GRANTED_ITEMS = new LinkedHashMap<>();

    private GrantedItemApi() {}

    public static void bootstrap() {}

    public static GrantedItemDefinition registerGrantedItem(GrantedItemDefinition definition) {
        XLibRegistryGuard.ensureMutable("granted_items");
        GrantedItemDefinition previous = GRANTED_ITEMS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate granted item registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<GrantedItemDefinition> unregisterGrantedItem(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("granted_items");
        return Optional.ofNullable(GRANTED_ITEMS.remove(id));
    }

    public static Optional<GrantedItemDefinition> findGrantedItem(ResourceLocation id) {
        return Optional.ofNullable(GRANTED_ITEMS.get(id));
    }

    public static Collection<GrantedItemDefinition> allGrantedItems() {
        return List.copyOf(GRANTED_ITEMS.values());
    }
}

