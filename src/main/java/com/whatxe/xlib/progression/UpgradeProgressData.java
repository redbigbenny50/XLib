package com.whatxe.xlib.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record UpgradeProgressData(
        Map<ResourceLocation, Integer> pointBalances,
        Map<ResourceLocation, Integer> counters,
        Set<ResourceLocation> unlockedNodes,
        Map<ResourceLocation, Set<ResourceLocation>> managedUnlockSources
) {
    private static final Codec<Set<ResourceLocation>> RESOURCE_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(Set::copyOf, List::copyOf);
    private static final Codec<Map<ResourceLocation, Set<ResourceLocation>>> RESOURCE_SET_MAP_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, RESOURCE_SET_CODEC);

    public static final Codec<UpgradeProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("point_balances", Map.of()).forGetter(UpgradeProgressData::pointBalances),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(UpgradeProgressData::counters),
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("unlocked_nodes", Set.of())
                    .forGetter(UpgradeProgressData::committedUnlockedNodes),
            RESOURCE_SET_MAP_CODEC.optionalFieldOf("managed_unlock_sources", Map.of()).forGetter(UpgradeProgressData::managedUnlockSources)
    ).apply(instance, UpgradeProgressData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeProgressData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public UpgradeProgressData {
        pointBalances = normalize(pointBalances);
        counters = normalize(counters);
        unlockedNodes = Set.copyOf(unlockedNodes);
        managedUnlockSources = normalizeManagedSources(managedUnlockSources);
    }

    public static UpgradeProgressData empty() {
        return new UpgradeProgressData(Map.of(), Map.of(), Set.of(), Map.of());
    }

    public Set<ResourceLocation> committedUnlockedNodes() {
        return this.unlockedNodes;
    }

    @Override
    public Set<ResourceLocation> unlockedNodes() {
        java.util.LinkedHashSet<ResourceLocation> combined = new java.util.LinkedHashSet<>(this.unlockedNodes);
        combined.addAll(this.managedUnlockSources.keySet());
        return Set.copyOf(combined);
    }

    public int points(ResourceLocation pointTypeId) {
        return this.pointBalances.getOrDefault(pointTypeId, 0);
    }

    public int counter(ResourceLocation counterId) {
        return this.counters.getOrDefault(counterId, 0);
    }

    public boolean hasUnlockedNode(ResourceLocation nodeId) {
        return this.unlockedNodes.contains(nodeId) || this.managedUnlockSources.containsKey(nodeId);
    }

    public Set<ResourceLocation> unlockSourcesFor(ResourceLocation nodeId) {
        return this.managedUnlockSources.getOrDefault(nodeId, Set.of());
    }

    public UpgradeProgressData withPoints(ResourceLocation pointTypeId, int amount) {
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(this.pointBalances);
        if (amount > 0) {
            updated.put(pointTypeId, amount);
        } else {
            updated.remove(pointTypeId);
        }
        return new UpgradeProgressData(updated, this.counters, this.unlockedNodes, this.managedUnlockSources);
    }

    public UpgradeProgressData addPoints(ResourceLocation pointTypeId, int delta) {
        return withPoints(pointTypeId, points(pointTypeId) + delta);
    }

    public UpgradeProgressData withCounter(ResourceLocation counterId, int amount) {
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(this.counters);
        if (amount > 0) {
            updated.put(counterId, amount);
        } else {
            updated.remove(counterId);
        }
        return new UpgradeProgressData(this.pointBalances, updated, this.unlockedNodes, this.managedUnlockSources);
    }

    public UpgradeProgressData addCounter(ResourceLocation counterId, int delta) {
        return withCounter(counterId, counter(counterId) + delta);
    }

    public UpgradeProgressData withUnlockedNode(ResourceLocation nodeId, boolean unlocked) {
        Set<ResourceLocation> updated = new java.util.LinkedHashSet<>(this.unlockedNodes);
        if (unlocked) {
            updated.add(nodeId);
        } else {
            updated.remove(nodeId);
        }
        return new UpgradeProgressData(this.pointBalances, this.counters, updated, this.managedUnlockSources);
    }

    public UpgradeProgressData withUnlockedNodeSource(ResourceLocation nodeId, ResourceLocation sourceId, boolean unlocked) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>(this.managedUnlockSources);
        Set<ResourceLocation> sources = new java.util.LinkedHashSet<>(updated.getOrDefault(nodeId, Set.of()));
        if (unlocked) {
            sources.add(sourceId);
        } else {
            sources.remove(sourceId);
        }
        if (sources.isEmpty()) {
            updated.remove(nodeId);
        } else {
            updated.put(nodeId, Set.copyOf(sources));
        }
        return new UpgradeProgressData(this.pointBalances, this.counters, this.unlockedNodes, updated);
    }

    private static Map<ResourceLocation, Integer> normalize(Map<ResourceLocation, Integer> values) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : values.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, Set<ResourceLocation>> normalizeManagedSources(Map<ResourceLocation, Set<ResourceLocation>> values) {
        Map<ResourceLocation, Set<ResourceLocation>> normalized = new LinkedHashMap<>();
        if (values == null) {
            return Map.of();
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            Set<ResourceLocation> filtered = entry.getValue().stream()
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            if (!filtered.isEmpty()) {
                normalized.put(entry.getKey(), Set.copyOf(filtered));
            }
        }
        return Map.copyOf(normalized);
    }
}
