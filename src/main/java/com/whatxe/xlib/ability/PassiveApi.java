package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

public final class PassiveApi {
    private static final Map<ResourceLocation, PassiveDefinition> PASSIVES = new LinkedHashMap<>();

    private PassiveApi() {}

    public static void bootstrap() {}

    public static PassiveDefinition registerPassive(PassiveDefinition passive) {
        XLibRegistryGuard.ensureMutable("passives");
        PassiveDefinition previous = PASSIVES.putIfAbsent(passive.id(), passive);
        if (previous != null) {
            throw new IllegalStateException("Duplicate passive registration: " + passive.id());
        }
        return passive;
    }

    public static Optional<PassiveDefinition> unregisterPassive(ResourceLocation id) {
        XLibRegistryGuard.ensureMutable("passives");
        return Optional.ofNullable(PASSIVES.remove(id));
    }

    public static Optional<PassiveDefinition> findPassive(ResourceLocation id) {
        return Optional.ofNullable(PASSIVES.get(id));
    }

    public static Collection<PassiveDefinition> allPassives() {
        return List.copyOf(PASSIVES.values());
    }

    public static Collection<PassiveDefinition> passivesInFamily(ResourceLocation familyId) {
        ResourceLocation resolvedFamilyId = java.util.Objects.requireNonNull(familyId, "familyId");
        return filterPassives(passive -> passive.familyId().filter(resolvedFamilyId::equals).isPresent());
    }

    public static Collection<PassiveDefinition> passivesInGroup(ResourceLocation groupId) {
        ResourceLocation resolvedGroupId = java.util.Objects.requireNonNull(groupId, "groupId");
        return filterPassives(passive -> passive.groupId().filter(resolvedGroupId::equals).isPresent());
    }

    public static Collection<PassiveDefinition> passivesOnPage(ResourceLocation pageId) {
        ResourceLocation resolvedPageId = java.util.Objects.requireNonNull(pageId, "pageId");
        return filterPassives(passive -> passive.pageId().filter(resolvedPageId::equals).isPresent());
    }

    public static Collection<PassiveDefinition> passivesWithTag(ResourceLocation tagId) {
        ResourceLocation resolvedTagId = java.util.Objects.requireNonNull(tagId, "tagId");
        return filterPassives(passive -> passive.hasTag(resolvedTagId));
    }

    private static Collection<PassiveDefinition> filterPassives(Predicate<PassiveDefinition> predicate) {
        return PASSIVES.values().stream().filter(predicate).toList();
    }
}

