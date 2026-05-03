package com.whatxe.xlib.combat;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record CombatMarkData(Map<ResourceLocation, CombatMarkState> marks) {
    public static final Codec<CombatMarkData> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, CombatMarkState.CODEC)
            .xmap(CombatMarkData::new, CombatMarkData::marks);

    public CombatMarkData {
        marks = normalize(marks);
    }

    public static CombatMarkData empty() {
        return new CombatMarkData(Map.of());
    }

    public boolean has(ResourceLocation markId) {
        return this.marks.containsKey(markId);
    }

    public CombatMarkState state(ResourceLocation markId) {
        return this.marks.get(markId);
    }

    public CombatMarkData withMark(ResourceLocation markId, CombatMarkState state) {
        Map<ResourceLocation, CombatMarkState> updated = new LinkedHashMap<>(this.marks);
        if (state.durationTicks() <= 0) {
            updated.remove(markId);
        } else {
            updated.put(markId, state);
        }
        return new CombatMarkData(updated);
    }

    public CombatMarkData withoutMark(ResourceLocation markId) {
        if (!this.marks.containsKey(markId)) {
            return this;
        }
        Map<ResourceLocation, CombatMarkState> updated = new LinkedHashMap<>(this.marks);
        updated.remove(markId);
        return new CombatMarkData(updated);
    }

    private static Map<ResourceLocation, CombatMarkState> normalize(Map<ResourceLocation, CombatMarkState> source) {
        Map<ResourceLocation, CombatMarkState> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, CombatMarkState> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().durationTicks() <= 0) {
                continue;
            }
            normalized.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(normalized);
    }
}
