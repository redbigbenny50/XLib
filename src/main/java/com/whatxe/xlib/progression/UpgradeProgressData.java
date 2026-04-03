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
        Set<ResourceLocation> unlockedNodes
) {
    public static final Codec<UpgradeProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("point_balances", Map.of()).forGetter(UpgradeProgressData::pointBalances),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(UpgradeProgressData::counters),
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("unlocked_nodes", Set.of()).forGetter(UpgradeProgressData::unlockedNodes)
    ).apply(instance, UpgradeProgressData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeProgressData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public UpgradeProgressData {
        pointBalances = normalize(pointBalances);
        counters = normalize(counters);
        unlockedNodes = Set.copyOf(unlockedNodes);
    }

    public static UpgradeProgressData empty() {
        return new UpgradeProgressData(Map.of(), Map.of(), Set.of());
    }

    public int points(ResourceLocation pointTypeId) {
        return this.pointBalances.getOrDefault(pointTypeId, 0);
    }

    public int counter(ResourceLocation counterId) {
        return this.counters.getOrDefault(counterId, 0);
    }

    public boolean hasUnlockedNode(ResourceLocation nodeId) {
        return this.unlockedNodes.contains(nodeId);
    }

    public UpgradeProgressData withPoints(ResourceLocation pointTypeId, int amount) {
        Map<ResourceLocation, Integer> updated = new LinkedHashMap<>(this.pointBalances);
        if (amount > 0) {
            updated.put(pointTypeId, amount);
        } else {
            updated.remove(pointTypeId);
        }
        return new UpgradeProgressData(updated, this.counters, this.unlockedNodes);
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
        return new UpgradeProgressData(this.pointBalances, updated, this.unlockedNodes);
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
        return new UpgradeProgressData(this.pointBalances, this.counters, updated);
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
}
