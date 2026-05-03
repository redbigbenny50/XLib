package com.whatxe.xlib.value;

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

public record TrackedValueData(
        Map<ResourceLocation, Integer> wholeAmounts,
        Map<ResourceLocation, Integer> partialAmounts,
        Map<ResourceLocation, Set<ResourceLocation>> foodReplacementSources
) {
    private static final Codec<Set<ResourceLocation>> SOURCE_SET_CODEC =
            ResourceLocation.CODEC.listOf().xmap(
                    list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)),
                    List::copyOf
            );

    public static final Codec<TrackedValueData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                            .optionalFieldOf("whole_amounts", Map.of())
                            .forGetter(TrackedValueData::wholeAmounts),
                    Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                            .optionalFieldOf("partial_amounts", Map.of())
                            .forGetter(TrackedValueData::partialAmounts),
                    Codec.unboundedMap(ResourceLocation.CODEC, SOURCE_SET_CODEC)
                            .optionalFieldOf("food_replacement_sources", Map.of())
                            .forGetter(TrackedValueData::foodReplacementSources)
            ).apply(instance, TrackedValueData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedValueData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static TrackedValueData empty() {
        return new TrackedValueData(Map.of(), Map.of(), Map.of());
    }

    public double exactAmount(ResourceLocation valueId) {
        return wholeAmount(valueId) + (partialAmount(valueId) / 1000.0D);
    }

    public int wholeAmount(ResourceLocation valueId) {
        return this.wholeAmounts.getOrDefault(valueId, 0);
    }

    public int partialAmount(ResourceLocation valueId) {
        return this.partialAmounts.getOrDefault(valueId, 0);
    }

    public boolean hasStoredAmount(ResourceLocation valueId) {
        return this.wholeAmounts.containsKey(valueId) || this.partialAmounts.containsKey(valueId);
    }

    public Set<ResourceLocation> storedValueIds() {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>(this.wholeAmounts.keySet());
        ids.addAll(this.partialAmounts.keySet());
        return Collections.unmodifiableSet(ids);
    }

    public Set<ResourceLocation> activeFoodReplacementValueIds() {
        return Collections.unmodifiableSet(this.foodReplacementSources.keySet());
    }

    public Set<ResourceLocation> foodReplacementSourcesFor(ResourceLocation valueId) {
        Set<ResourceLocation> sources = this.foodReplacementSources.get(valueId);
        return sources != null ? Collections.unmodifiableSet(sources) : Set.of();
    }

    public boolean hasFoodReplacement(ResourceLocation valueId) {
        Set<ResourceLocation> sources = this.foodReplacementSources.get(valueId);
        return sources != null && !sources.isEmpty();
    }

    public TrackedValueData withExactAmount(ResourceLocation valueId, double amount) {
        Map<ResourceLocation, Integer> updatedWholes = new LinkedHashMap<>(this.wholeAmounts);
        Map<ResourceLocation, Integer> updatedPartials = new LinkedHashMap<>(this.partialAmounts);

        int whole = (int) Math.floor(amount + 1.0E-9D);
        int partial = (int) Math.round((amount - whole) * 1000.0D);
        if (partial >= 1000) {
            whole += 1;
            partial -= 1000;
        }
        if (partial < 0) {
            partial = 0;
        }

        updatedWholes.put(valueId, whole);
        if (partial > 0) {
            updatedPartials.put(valueId, partial);
        } else {
            updatedPartials.remove(valueId);
        }
        return new TrackedValueData(
                Collections.unmodifiableMap(updatedWholes),
                Collections.unmodifiableMap(updatedPartials),
                this.foodReplacementSources
        );
    }

    public TrackedValueData withoutStoredAmount(ResourceLocation valueId) {
        if (!this.wholeAmounts.containsKey(valueId) && !this.partialAmounts.containsKey(valueId)) {
            return this;
        }

        Map<ResourceLocation, Integer> updatedWholes = new LinkedHashMap<>(this.wholeAmounts);
        Map<ResourceLocation, Integer> updatedPartials = new LinkedHashMap<>(this.partialAmounts);
        updatedWholes.remove(valueId);
        updatedPartials.remove(valueId);
        return new TrackedValueData(
                Collections.unmodifiableMap(updatedWholes),
                Collections.unmodifiableMap(updatedPartials),
                this.foodReplacementSources
        );
    }

    public TrackedValueData withFoodReplacementSource(ResourceLocation valueId, ResourceLocation sourceId, boolean add) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>(this.foodReplacementSources);
        if (add) {
            updated.computeIfAbsent(valueId, ignored -> new LinkedHashSet<>()).add(sourceId);
        } else {
            Set<ResourceLocation> existing = updated.get(valueId);
            if (existing != null) {
                LinkedHashSet<ResourceLocation> mutable = new LinkedHashSet<>(existing);
                mutable.remove(sourceId);
                if (mutable.isEmpty()) {
                    updated.remove(valueId);
                } else {
                    updated.put(valueId, Collections.unmodifiableSet(mutable));
                }
            }
        }
        return new TrackedValueData(
                this.wholeAmounts,
                this.partialAmounts,
                Collections.unmodifiableMap(updated)
        );
    }

    public TrackedValueData clearFoodReplacementSource(ResourceLocation sourceId) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : this.foodReplacementSources.entrySet()) {
            LinkedHashSet<ResourceLocation> remaining = new LinkedHashSet<>(entry.getValue());
            remaining.remove(sourceId);
            if (!remaining.isEmpty()) {
                updated.put(entry.getKey(), Collections.unmodifiableSet(remaining));
            }
        }
        return new TrackedValueData(
                this.wholeAmounts,
                this.partialAmounts,
                Collections.unmodifiableMap(updated)
        );
    }
}
