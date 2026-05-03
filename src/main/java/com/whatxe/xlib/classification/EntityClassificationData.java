package com.whatxe.xlib.classification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record EntityClassificationData(
        Map<ResourceLocation, Set<ResourceLocation>> syntheticEntityTypeSources,
        Map<ResourceLocation, Set<ResourceLocation>> syntheticTagSources
) {
    private static final Codec<Set<ResourceLocation>> SOURCE_SET_CODEC =
            ResourceLocation.CODEC.listOf().xmap(
                    list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)),
                    List::copyOf
            );

    public static final Codec<EntityClassificationData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, SOURCE_SET_CODEC)
                            .optionalFieldOf("synthetic_entity_type_sources", Map.of())
                            .forGetter(EntityClassificationData::syntheticEntityTypeSources),
                    Codec.unboundedMap(ResourceLocation.CODEC, SOURCE_SET_CODEC)
                            .optionalFieldOf("synthetic_tag_sources", Map.of())
                            .forGetter(EntityClassificationData::syntheticTagSources)
            ).apply(instance, EntityClassificationData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityClassificationData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static EntityClassificationData empty() {
        return new EntityClassificationData(Map.of(), Map.of());
    }

    public Set<ResourceLocation> syntheticEntityTypeIds() {
        return Collections.unmodifiableSet(this.syntheticEntityTypeSources.keySet());
    }

    public Set<ResourceLocation> syntheticTagIds() {
        return Collections.unmodifiableSet(this.syntheticTagSources.keySet());
    }

    public boolean hasSyntheticEntityType(ResourceLocation entityTypeId) {
        Set<ResourceLocation> sources = this.syntheticEntityTypeSources.get(entityTypeId);
        return sources != null && !sources.isEmpty();
    }

    public boolean hasSyntheticTag(ResourceLocation tagId) {
        Set<ResourceLocation> sources = this.syntheticTagSources.get(tagId);
        return sources != null && !sources.isEmpty();
    }

    public EntityClassificationData withSyntheticEntityType(ResourceLocation entityTypeId, ResourceLocation sourceId, boolean add) {
        return new EntityClassificationData(
                updateSources(this.syntheticEntityTypeSources, entityTypeId, sourceId, add),
                this.syntheticTagSources
        );
    }

    public EntityClassificationData withSyntheticTag(ResourceLocation tagId, ResourceLocation sourceId, boolean add) {
        return new EntityClassificationData(
                this.syntheticEntityTypeSources,
                updateSources(this.syntheticTagSources, tagId, sourceId, add)
        );
    }

    public EntityClassificationData clearSource(ResourceLocation sourceId) {
        return new EntityClassificationData(
                clearSource(this.syntheticEntityTypeSources, sourceId),
                clearSource(this.syntheticTagSources, sourceId)
        );
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> updateSources(
            Map<ResourceLocation, Set<ResourceLocation>> currentSources,
            ResourceLocation id,
            ResourceLocation sourceId,
            boolean add
    ) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>(currentSources);
        if (add) {
            updated.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(sourceId);
        } else {
            Set<ResourceLocation> sources = updated.get(id);
            if (sources != null) {
                LinkedHashSet<ResourceLocation> mutable = new LinkedHashSet<>(sources);
                mutable.remove(sourceId);
                if (mutable.isEmpty()) {
                    updated.remove(id);
                } else {
                    updated.put(id, Collections.unmodifiableSet(mutable));
                }
            }
        }
        return Collections.unmodifiableMap(updated);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> clearSource(
            Map<ResourceLocation, Set<ResourceLocation>> currentSources,
            ResourceLocation sourceId
    ) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : currentSources.entrySet()) {
            LinkedHashSet<ResourceLocation> remaining = new LinkedHashSet<>(entry.getValue());
            remaining.remove(sourceId);
            if (!remaining.isEmpty()) {
                updated.put(entry.getKey(), Collections.unmodifiableSet(remaining));
            }
        }
        return Collections.unmodifiableMap(updated);
    }
}
