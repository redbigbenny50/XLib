package com.whatxe.xlib.capability;

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

public record CapabilityPolicyData(
        Map<ResourceLocation, Set<ResourceLocation>> policySources
) {
    private static final Codec<Set<ResourceLocation>> SOURCE_SET_CODEC =
            ResourceLocation.CODEC.listOf().xmap(
                    list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)),
                    set -> List.copyOf(set)
            );

    public static final Codec<CapabilityPolicyData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, SOURCE_SET_CODEC)
                            .optionalFieldOf("policy_sources", Map.of())
                            .forGetter(CapabilityPolicyData::policySources)
            ).apply(instance, CapabilityPolicyData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CapabilityPolicyData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static CapabilityPolicyData empty() {
        return new CapabilityPolicyData(Map.of());
    }

    public Set<ResourceLocation> activePolicies() {
        return Collections.unmodifiableSet(policySources.keySet());
    }

    public Set<ResourceLocation> sourcesFor(ResourceLocation policyId) {
        Set<ResourceLocation> sources = policySources.get(policyId);
        return sources != null ? Collections.unmodifiableSet(sources) : Set.of();
    }

    public boolean hasPolicy(ResourceLocation policyId) {
        Set<ResourceLocation> sources = policySources.get(policyId);
        return sources != null && !sources.isEmpty();
    }

    public CapabilityPolicyData withPolicySource(ResourceLocation policyId, ResourceLocation sourceId, boolean add) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>(policySources);
        if (add) {
            updated.computeIfAbsent(policyId, k -> new LinkedHashSet<>()).add(sourceId);
        } else {
            Set<ResourceLocation> sources = updated.get(policyId);
            if (sources != null) {
                Set<ResourceLocation> mutable = new LinkedHashSet<>(sources);
                mutable.remove(sourceId);
                if (mutable.isEmpty()) {
                    updated.remove(policyId);
                } else {
                    updated.put(policyId, Collections.unmodifiableSet(mutable));
                }
            }
        }
        return new CapabilityPolicyData(Collections.unmodifiableMap(updated));
    }

    public CapabilityPolicyData clearPolicySource(ResourceLocation sourceId) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : policySources.entrySet()) {
            Set<ResourceLocation> remaining = new LinkedHashSet<>(entry.getValue());
            remaining.remove(sourceId);
            if (!remaining.isEmpty()) {
                updated.put(entry.getKey(), Collections.unmodifiableSet(remaining));
            }
        }
        return new CapabilityPolicyData(Collections.unmodifiableMap(updated));
    }

    public CapabilityPolicyData clearAllPolicies() {
        return empty();
    }

    public CapabilityPolicyData retainRegistered(Set<ResourceLocation> registeredIds) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : policySources.entrySet()) {
            if (registeredIds.contains(entry.getKey()) && !entry.getValue().isEmpty()) {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        if (updated.size() == policySources.size()) {
            return this;
        }
        return new CapabilityPolicyData(Collections.unmodifiableMap(updated));
    }
}
