package com.whatxe.xlib.classification;

import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class EntityClassificationApi {
    private static final Map<UUID, ResolvedEntityClassificationState> RUNTIME_CACHE = new ConcurrentHashMap<>();

    private EntityClassificationApi() {}

    public static void bootstrap() {}

    public static void onEntityLoad(Entity entity) {
        if (!entity.level().isClientSide()) {
            RUNTIME_CACHE.put(entity.getUUID(), resolveState(getData(entity)));
        }
    }

    public static void onEntityUnload(Entity entity) {
        RUNTIME_CACHE.remove(entity.getUUID());
    }

    public static void grantSyntheticEntityType(Entity entity, ResourceLocation entityTypeId, ResourceLocation sourceId) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityTypeId)) {
            return;
        }
        update(entity, getData(entity).withSyntheticEntityType(entityTypeId, sourceId, true));
    }

    public static void revokeSyntheticEntityType(Entity entity, ResourceLocation entityTypeId, ResourceLocation sourceId) {
        update(entity, getData(entity).withSyntheticEntityType(entityTypeId, sourceId, false));
    }

    public static void grantSyntheticTag(Entity entity, ResourceLocation tagId, ResourceLocation sourceId) {
        update(entity, getData(entity).withSyntheticTag(tagId, sourceId, true));
    }

    public static void revokeSyntheticTag(Entity entity, ResourceLocation tagId, ResourceLocation sourceId) {
        update(entity, getData(entity).withSyntheticTag(tagId, sourceId, false));
    }

    public static void clearSource(Entity entity, ResourceLocation sourceId) {
        update(entity, getData(entity).clearSource(sourceId));
    }

    public static void clearAll(Entity entity) {
        update(entity, EntityClassificationData.empty());
    }

    public static boolean countsAsEntity(Entity entity, ResourceLocation entityTypeId) {
        return countsAsEntity(entity, entityTypeId, EntityClassificationMatchMode.MERGED);
    }

    public static boolean countsAsEntity(Entity entity, ResourceLocation entityTypeId, EntityClassificationMatchMode mode) {
        ResourceLocation realEntityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return state(entity).countsAsEntity(realEntityTypeId, entityTypeId, mode);
    }

    public static boolean matchesEntityTag(Entity entity, ResourceLocation tagId) {
        return matchesEntityTag(entity, tagId, EntityClassificationMatchMode.MERGED);
    }

    public static boolean matchesEntityTag(Entity entity, ResourceLocation tagId, EntityClassificationMatchMode mode) {
        return state(entity).matchesEntityTag(realTagIds(entity), tagId, mode);
    }

    public static boolean matchesSelector(
            Entity entity,
            Collection<ResourceLocation> entityTypeIds,
            Collection<ResourceLocation> tagIds
    ) {
        return matchesSelector(entity, entityTypeIds, tagIds, EntityClassificationMatchMode.MERGED);
    }

    public static boolean matchesSelector(
            Entity entity,
            Collection<ResourceLocation> entityTypeIds,
            Collection<ResourceLocation> tagIds,
            EntityClassificationMatchMode mode
    ) {
        for (ResourceLocation entityTypeId : entityTypeIds) {
            if (countsAsEntity(entity, entityTypeId, mode)) {
                return true;
            }
        }
        for (ResourceLocation tagId : tagIds) {
            if (matchesEntityTag(entity, tagId, mode)) {
                return true;
            }
        }
        return entityTypeIds.isEmpty() && tagIds.isEmpty();
    }

    public static boolean hasSyntheticEntityType(Entity entity, ResourceLocation entityTypeId) {
        return getData(entity).hasSyntheticEntityType(entityTypeId);
    }

    public static boolean hasSyntheticTag(Entity entity, ResourceLocation tagId) {
        return getData(entity).hasSyntheticTag(tagId);
    }

    public static Collection<ResourceLocation> syntheticEntityTypes(Entity entity) {
        return List.copyOf(state(entity).syntheticEntityTypeIds());
    }

    public static Collection<ResourceLocation> directSyntheticTags(Entity entity) {
        return List.copyOf(state(entity).directSyntheticTagIds());
    }

    public static Collection<ResourceLocation> inheritedSyntheticTags(Entity entity) {
        return List.copyOf(state(entity).inheritedSyntheticTagIds());
    }

    public static Collection<ResourceLocation> syntheticTags(Entity entity) {
        LinkedHashSet<ResourceLocation> tags = new LinkedHashSet<>(state(entity).directSyntheticTagIds());
        tags.addAll(state(entity).inheritedSyntheticTagIds());
        return List.copyOf(tags);
    }

    public static Collection<ResourceLocation> effectiveEntityTypes(Entity entity) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        ResourceLocation realEntityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (realEntityTypeId != null) {
            ids.add(realEntityTypeId);
        }
        ids.addAll(state(entity).syntheticEntityTypeIds());
        return List.copyOf(ids);
    }

    public static Collection<ResourceLocation> effectiveTags(Entity entity) {
        LinkedHashSet<ResourceLocation> tags = new LinkedHashSet<>(realTagIds(entity));
        tags.addAll(state(entity).directSyntheticTagIds());
        tags.addAll(state(entity).inheritedSyntheticTagIds());
        return List.copyOf(tags);
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> syntheticEntityTypeSources(Entity entity) {
        return Map.copyOf(getData(entity).syntheticEntityTypeSources());
    }

    public static Map<ResourceLocation, Set<ResourceLocation>> syntheticTagSources(Entity entity) {
        return Map.copyOf(getData(entity).syntheticTagSources());
    }

    public static EntityClassificationData getData(Entity entity) {
        return entity.getData(ModAttachments.ENTITY_CLASSIFICATIONS);
    }

    public static void setData(Entity entity, EntityClassificationData data) {
        EntityClassificationData sanitizedData = Objects.requireNonNull(data, "data");
        EntityClassificationData previousData = entity.getData(ModAttachments.ENTITY_CLASSIFICATIONS);
        ResolvedEntityClassificationState previousState = state(entity);
        entity.setData(ModAttachments.ENTITY_CLASSIFICATIONS, sanitizedData);
        ResolvedEntityClassificationState resolvedState = resolveState(sanitizedData);
        RUNTIME_CACHE.put(entity.getUUID(), resolvedState);
        if (!sanitizedData.equals(previousData)) {
            EntityClassificationCompatApi.notifyChanged(
                    entity,
                    snapshot(entity, previousState),
                    snapshot(entity, resolvedState)
            );
        }
    }

    static ResolvedEntityClassificationState resolveState(EntityClassificationData data) {
        LinkedHashSet<ResourceLocation> inheritedSyntheticTagIds = new LinkedHashSet<>();
        for (ResourceLocation syntheticEntityTypeId : data.syntheticEntityTypeIds()) {
            inheritedSyntheticTagIds.addAll(tagsForEntityType(syntheticEntityTypeId));
        }
        return new ResolvedEntityClassificationState(
                Set.copyOf(data.syntheticEntityTypeIds()),
                Set.copyOf(data.syntheticTagIds()),
                Set.copyOf(inheritedSyntheticTagIds)
        );
    }

    private static void update(Entity entity, EntityClassificationData updatedData) {
        EntityClassificationData currentData = getData(entity);
        if (!updatedData.equals(currentData)) {
            setData(entity, updatedData);
        }
    }

    private static ResolvedEntityClassificationState state(Entity entity) {
        return RUNTIME_CACHE.computeIfAbsent(entity.getUUID(), ignored -> resolveState(getData(entity)));
    }

    private static Set<ResourceLocation> realTagIds(Entity entity) {
        return entity.getType().builtInRegistryHolder().tags()
                .map(TagKey::location)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<ResourceLocation> tagsForEntityType(ResourceLocation entityTypeId) {
        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
        if (entityType.isEmpty()) {
            return Set.of();
        }
        return entityType.get().builtInRegistryHolder().tags()
                .map(TagKey::location)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static EntityClassificationCompatApi.EntityClassificationSnapshot snapshot(
            Entity entity,
            ResolvedEntityClassificationState resolvedState
    ) {
        return new EntityClassificationCompatApi.EntityClassificationSnapshot(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                realTagIds(entity),
                Set.copyOf(resolvedState.syntheticEntityTypeIds()),
                Set.copyOf(resolvedState.directSyntheticTagIds()),
                Set.copyOf(resolvedState.inheritedSyntheticTagIds())
        );
    }
}
