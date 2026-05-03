package com.whatxe.xlib.classification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public final class EntityClassificationCompatApi {
    private static final Map<ResourceLocation, EntityClassificationChangeHook> CHANGE_HOOKS = new LinkedHashMap<>();

    private EntityClassificationCompatApi() {}

    public static void registerChangeHook(ResourceLocation hookId, EntityClassificationChangeHook hook) {
        CHANGE_HOOKS.put(Objects.requireNonNull(hookId, "hookId"), Objects.requireNonNull(hook, "hook"));
    }

    public static void unregisterChangeHook(ResourceLocation hookId) {
        CHANGE_HOOKS.remove(Objects.requireNonNull(hookId, "hookId"));
    }

    public static void clearChangeHooks() {
        CHANGE_HOOKS.clear();
    }

    static void notifyChanged(
            Entity entity,
            EntityClassificationSnapshot previousSnapshot,
            EntityClassificationSnapshot currentSnapshot
    ) {
        for (EntityClassificationChangeHook hook : CHANGE_HOOKS.values()) {
            hook.onChanged(entity, previousSnapshot, currentSnapshot);
        }
    }

    @FunctionalInterface
    public interface EntityClassificationChangeHook {
        void onChanged(Entity entity, EntityClassificationSnapshot previousSnapshot, EntityClassificationSnapshot currentSnapshot);
    }

    public record EntityClassificationSnapshot(
            ResourceLocation realEntityTypeId,
            Set<ResourceLocation> realTagIds,
            Set<ResourceLocation> syntheticEntityTypeIds,
            Set<ResourceLocation> directSyntheticTagIds,
            Set<ResourceLocation> inheritedSyntheticTagIds
    ) {}
}
