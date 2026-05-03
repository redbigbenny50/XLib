package com.whatxe.xlib.capability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record CraftingPolicy(
        boolean canUsePlayerCrafting,
        boolean canUseCraftingTable,
        Set<ResourceLocation> allowedStationTags,
        Set<ResourceLocation> blockedStationTags
) {
    public static final CraftingPolicy FULL = new CraftingPolicy(true, true, Set.of(), Set.of());

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canUsePlayerCrafting = true;
        private boolean canUseCraftingTable = true;
        private final Set<ResourceLocation> allowedStationTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedStationTags = new LinkedHashSet<>();

        private Builder() {}

        public Builder canUsePlayerCrafting(boolean value) { this.canUsePlayerCrafting = value; return this; }
        public Builder canUseCraftingTable(boolean value) { this.canUseCraftingTable = value; return this; }
        public Builder allowStationTag(ResourceLocation tag) { this.allowedStationTags.add(tag); return this; }
        public Builder blockStationTag(ResourceLocation tag) { this.blockedStationTags.add(tag); return this; }

        public CraftingPolicy build() {
            return new CraftingPolicy(
                    canUsePlayerCrafting, canUseCraftingTable,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedStationTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedStationTags))
            );
        }
    }

    CraftingPolicy mergeRestrictive(CraftingPolicy other) {
        Set<ResourceLocation> mergedAllowed = new LinkedHashSet<>(this.allowedStationTags);
        mergedAllowed.addAll(other.allowedStationTags);
        Set<ResourceLocation> mergedBlocked = new LinkedHashSet<>(this.blockedStationTags);
        mergedBlocked.addAll(other.blockedStationTags);
        return new CraftingPolicy(
                this.canUsePlayerCrafting && other.canUsePlayerCrafting,
                this.canUseCraftingTable && other.canUseCraftingTable,
                Collections.unmodifiableSet(mergedAllowed),
                Collections.unmodifiableSet(mergedBlocked)
        );
    }
}
