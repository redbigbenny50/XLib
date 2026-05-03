package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
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

    public static Collection<GrantedItemDefinition> grantedItemsInFamily(ResourceLocation familyId) {
        ResourceLocation resolvedFamilyId = java.util.Objects.requireNonNull(familyId, "familyId");
        return filterGrantedItems(definition -> definition.familyId().filter(resolvedFamilyId::equals).isPresent());
    }

    public static Collection<GrantedItemDefinition> grantedItemsInGroup(ResourceLocation groupId) {
        ResourceLocation resolvedGroupId = java.util.Objects.requireNonNull(groupId, "groupId");
        return filterGrantedItems(definition -> definition.groupId().filter(resolvedGroupId::equals).isPresent());
    }

    public static Collection<GrantedItemDefinition> grantedItemsOnPage(ResourceLocation pageId) {
        ResourceLocation resolvedPageId = java.util.Objects.requireNonNull(pageId, "pageId");
        return filterGrantedItems(definition -> definition.pageId().filter(resolvedPageId::equals).isPresent());
    }

    public static Collection<GrantedItemDefinition> grantedItemsWithTag(ResourceLocation tagId) {
        ResourceLocation resolvedTagId = java.util.Objects.requireNonNull(tagId, "tagId");
        return filterGrantedItems(definition -> definition.hasTag(resolvedTagId));
    }

    private static Collection<GrantedItemDefinition> filterGrantedItems(Predicate<GrantedItemDefinition> predicate) {
        return GRANTED_ITEMS.values().stream().filter(predicate).toList();
    }
}

