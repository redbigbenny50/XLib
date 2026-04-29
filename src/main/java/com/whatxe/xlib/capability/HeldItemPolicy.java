package com.whatxe.xlib.capability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record HeldItemPolicy(
        boolean canUseMainHand,
        boolean canUseOffhand,
        boolean canBlockWithShields,
        boolean canUseTools,
        boolean canUseWeapons,
        boolean canPlaceBlocks,
        boolean canBreakBlocks,
        Set<ResourceLocation> allowedItemTags,
        Set<ResourceLocation> blockedItemTags
) {
    public static final HeldItemPolicy FULL = new HeldItemPolicy(true, true, true, true, true, true, true, Set.of(), Set.of());

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canUseMainHand = true;
        private boolean canUseOffhand = true;
        private boolean canBlockWithShields = true;
        private boolean canUseTools = true;
        private boolean canUseWeapons = true;
        private boolean canPlaceBlocks = true;
        private boolean canBreakBlocks = true;
        private final Set<ResourceLocation> allowedItemTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedItemTags = new LinkedHashSet<>();

        private Builder() {}

        public Builder canUseMainHand(boolean value) { this.canUseMainHand = value; return this; }
        public Builder canUseOffhand(boolean value) { this.canUseOffhand = value; return this; }
        public Builder canBlockWithShields(boolean value) { this.canBlockWithShields = value; return this; }
        public Builder canUseTools(boolean value) { this.canUseTools = value; return this; }
        public Builder canUseWeapons(boolean value) { this.canUseWeapons = value; return this; }
        public Builder canPlaceBlocks(boolean value) { this.canPlaceBlocks = value; return this; }
        public Builder canBreakBlocks(boolean value) { this.canBreakBlocks = value; return this; }
        public Builder allowItemTag(ResourceLocation tag) { this.allowedItemTags.add(tag); return this; }
        public Builder blockItemTag(ResourceLocation tag) { this.blockedItemTags.add(tag); return this; }

        public HeldItemPolicy build() {
            return new HeldItemPolicy(
                    canUseMainHand, canUseOffhand, canBlockWithShields,
                    canUseTools, canUseWeapons, canPlaceBlocks, canBreakBlocks,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedItemTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedItemTags))
            );
        }
    }

    HeldItemPolicy mergeRestrictive(HeldItemPolicy other) {
        Set<ResourceLocation> mergedAllowed = new LinkedHashSet<>(this.allowedItemTags);
        mergedAllowed.addAll(other.allowedItemTags);
        Set<ResourceLocation> mergedBlocked = new LinkedHashSet<>(this.blockedItemTags);
        mergedBlocked.addAll(other.blockedItemTags);
        return new HeldItemPolicy(
                this.canUseMainHand && other.canUseMainHand,
                this.canUseOffhand && other.canUseOffhand,
                this.canBlockWithShields && other.canBlockWithShields,
                this.canUseTools && other.canUseTools,
                this.canUseWeapons && other.canUseWeapons,
                this.canPlaceBlocks && other.canPlaceBlocks,
                this.canBreakBlocks && other.canBreakBlocks,
                Collections.unmodifiableSet(mergedAllowed),
                Collections.unmodifiableSet(mergedBlocked)
        );
    }
}
