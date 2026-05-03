package com.whatxe.xlib.binding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record EntityBindingData(
        Map<UUID, EntityBindingState> primaryBindings,
        Set<UUID> secondaryRefs
) {
    private static final Codec<UUID> UUID_KEY_CODEC = EntityBindingState.UUID_CODEC;

    private static final Codec<Set<UUID>> UUID_SET_CODEC = UUID_KEY_CODEC.listOf().xmap(
            list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)),
            set -> List.copyOf(set)
    );

    public static final Codec<EntityBindingData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(UUID_KEY_CODEC, EntityBindingState.CODEC)
                            .optionalFieldOf("primary_bindings", Map.of())
                            .forGetter(EntityBindingData::primaryBindings),
                    UUID_SET_CODEC
                            .optionalFieldOf("secondary_refs", Set.of())
                            .forGetter(EntityBindingData::secondaryRefs)
            ).apply(instance, EntityBindingData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityBindingData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static EntityBindingData empty() {
        return new EntityBindingData(Map.of(), Set.of());
    }

    public Collection<EntityBindingState> allPrimary() {
        return Collections.unmodifiableCollection(primaryBindings.values());
    }

    public boolean hasPrimaryBinding(UUID instanceId) {
        return primaryBindings.containsKey(instanceId);
    }

    public boolean hasSecondaryRef(UUID instanceId) {
        return secondaryRefs.contains(instanceId);
    }

    public EntityBindingData withBinding(EntityBindingState state) {
        Map<UUID, EntityBindingState> updated = new LinkedHashMap<>(primaryBindings);
        updated.put(state.bindingInstanceId(), state);
        return new EntityBindingData(Collections.unmodifiableMap(updated), secondaryRefs);
    }

    public EntityBindingData withoutBinding(UUID instanceId) {
        if (!primaryBindings.containsKey(instanceId)) return this;
        Map<UUID, EntityBindingState> updated = new LinkedHashMap<>(primaryBindings);
        updated.remove(instanceId);
        return new EntityBindingData(Collections.unmodifiableMap(updated), secondaryRefs);
    }

    public EntityBindingData withSecondaryRef(UUID instanceId) {
        if (secondaryRefs.contains(instanceId)) return this;
        Set<UUID> updated = new LinkedHashSet<>(secondaryRefs);
        updated.add(instanceId);
        return new EntityBindingData(primaryBindings, Collections.unmodifiableSet(updated));
    }

    public EntityBindingData withoutSecondaryRef(UUID instanceId) {
        if (!secondaryRefs.contains(instanceId)) return this;
        Set<UUID> updated = new LinkedHashSet<>(secondaryRefs);
        updated.remove(instanceId);
        return new EntityBindingData(primaryBindings, Collections.unmodifiableSet(updated));
    }

    public EntityBindingData updateBinding(EntityBindingState state) {
        if (!primaryBindings.containsKey(state.bindingInstanceId())) return this;
        return withBinding(state);
    }
}
