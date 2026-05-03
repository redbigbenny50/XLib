package com.whatxe.xlib.classification;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record ResolvedEntityClassificationState(
        Set<ResourceLocation> syntheticEntityTypeIds,
        Set<ResourceLocation> directSyntheticTagIds,
        Set<ResourceLocation> inheritedSyntheticTagIds
) {
    public boolean countsAsEntity(ResourceLocation realEntityTypeId, ResourceLocation targetEntityTypeId, EntityClassificationMatchMode mode) {
        boolean realMatch = realEntityTypeId != null && realEntityTypeId.equals(targetEntityTypeId);
        boolean syntheticMatch = this.syntheticEntityTypeIds.contains(targetEntityTypeId);
        return switch (mode) {
            case REAL_ONLY -> realMatch;
            case SYNTHETIC_ONLY -> syntheticMatch;
            case MERGED -> realMatch || syntheticMatch;
        };
    }

    public boolean matchesEntityTag(Set<ResourceLocation> realTagIds, ResourceLocation targetTagId, EntityClassificationMatchMode mode) {
        boolean realMatch = realTagIds.contains(targetTagId);
        boolean syntheticMatch = this.directSyntheticTagIds.contains(targetTagId) || this.inheritedSyntheticTagIds.contains(targetTagId);
        return switch (mode) {
            case REAL_ONLY -> realMatch;
            case SYNTHETIC_ONLY -> syntheticMatch;
            case MERGED -> realMatch || syntheticMatch;
        };
    }
}
