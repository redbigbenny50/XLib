package com.whatxe.xlib.combat;

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

public record DamageModifierProfileData(
        Map<ResourceLocation, Set<ResourceLocation>> profileSources
) {
    private static final Codec<Set<ResourceLocation>> SOURCE_SET_CODEC =
            ResourceLocation.CODEC.listOf().xmap(
                    list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)),
                    List::copyOf
            );

    public static final Codec<DamageModifierProfileData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, SOURCE_SET_CODEC)
                            .optionalFieldOf("profile_sources", Map.of())
                            .forGetter(DamageModifierProfileData::profileSources)
            ).apply(instance, DamageModifierProfileData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageModifierProfileData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static DamageModifierProfileData empty() {
        return new DamageModifierProfileData(Map.of());
    }

    public Set<ResourceLocation> activeProfiles() {
        return Collections.unmodifiableSet(this.profileSources.keySet());
    }

    public Set<ResourceLocation> sourcesFor(ResourceLocation profileId) {
        Set<ResourceLocation> sources = this.profileSources.get(profileId);
        return sources != null ? Collections.unmodifiableSet(sources) : Set.of();
    }

    public boolean hasProfile(ResourceLocation profileId) {
        Set<ResourceLocation> sources = this.profileSources.get(profileId);
        return sources != null && !sources.isEmpty();
    }

    public DamageModifierProfileData withProfileSource(ResourceLocation profileId, ResourceLocation sourceId, boolean add) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>(this.profileSources);
        if (add) {
            updated.computeIfAbsent(profileId, ignored -> new LinkedHashSet<>()).add(sourceId);
        } else {
            Set<ResourceLocation> sources = updated.get(profileId);
            if (sources != null) {
                Set<ResourceLocation> mutable = new LinkedHashSet<>(sources);
                mutable.remove(sourceId);
                if (mutable.isEmpty()) {
                    updated.remove(profileId);
                } else {
                    updated.put(profileId, Collections.unmodifiableSet(mutable));
                }
            }
        }
        return new DamageModifierProfileData(Collections.unmodifiableMap(updated));
    }

    public DamageModifierProfileData clearProfileSource(ResourceLocation sourceId) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : this.profileSources.entrySet()) {
            Set<ResourceLocation> remaining = new LinkedHashSet<>(entry.getValue());
            remaining.remove(sourceId);
            if (!remaining.isEmpty()) {
                updated.put(entry.getKey(), Collections.unmodifiableSet(remaining));
            }
        }
        return new DamageModifierProfileData(Collections.unmodifiableMap(updated));
    }

    public DamageModifierProfileData retainRegistered(Set<ResourceLocation> registeredIds) {
        Map<ResourceLocation, Set<ResourceLocation>> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> entry : this.profileSources.entrySet()) {
            if (registeredIds.contains(entry.getKey()) && !entry.getValue().isEmpty()) {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        if (updated.size() == this.profileSources.size()) {
            return this;
        }
        return new DamageModifierProfileData(Collections.unmodifiableMap(updated));
    }
}
