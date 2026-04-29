package com.whatxe.xlib.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record VisualFormData(
        Map<ResourceLocation, ResourceLocation> formSources
) {
    public static final Codec<VisualFormData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
                            .optionalFieldOf("form_sources", Map.of())
                            .forGetter(VisualFormData::formSources)
            ).apply(instance, VisualFormData::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VisualFormData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static VisualFormData empty() {
        return new VisualFormData(Map.of());
    }

    public boolean hasForm(ResourceLocation formId) {
        return formSources.containsKey(formId);
    }

    public Optional<ResourceLocation> sourceFor(ResourceLocation formId) {
        return Optional.ofNullable(formSources.get(formId));
    }

    public Set<ResourceLocation> activeForms() {
        return Collections.unmodifiableSet(formSources.keySet());
    }

    public Optional<ResourceLocation> primaryForm() {
        return formSources.keySet().stream().findFirst();
    }

    public VisualFormData withForm(ResourceLocation formId, ResourceLocation sourceId) {
        Map<ResourceLocation, ResourceLocation> updated = new LinkedHashMap<>(formSources);
        updated.put(formId, sourceId);
        return new VisualFormData(Collections.unmodifiableMap(updated));
    }

    public VisualFormData withoutForm(ResourceLocation formId) {
        if (!formSources.containsKey(formId)) return this;
        Map<ResourceLocation, ResourceLocation> updated = new LinkedHashMap<>(formSources);
        updated.remove(formId);
        return new VisualFormData(Collections.unmodifiableMap(updated));
    }

    public VisualFormData clearSource(ResourceLocation sourceId) {
        Map<ResourceLocation, ResourceLocation> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : formSources.entrySet()) {
            if (!entry.getValue().equals(sourceId)) {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        if (updated.size() == formSources.size()) return this;
        return new VisualFormData(Collections.unmodifiableMap(updated));
    }

    public VisualFormData retainRegistered(Set<ResourceLocation> registeredIds) {
        Map<ResourceLocation, ResourceLocation> updated = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : formSources.entrySet()) {
            if (registeredIds.contains(entry.getKey())) {
                updated.put(entry.getKey(), entry.getValue());
            }
        }
        if (updated.size() == formSources.size()) return this;
        return new VisualFormData(Collections.unmodifiableMap(updated));
    }
}
