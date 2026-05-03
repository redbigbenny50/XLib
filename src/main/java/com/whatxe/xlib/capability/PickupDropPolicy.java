package com.whatxe.xlib.capability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

public record PickupDropPolicy(
        boolean canPickupItems,
        boolean canDropItems,
        Set<ResourceLocation> allowedItemIds,
        Set<ResourceLocation> blockedItemIds,
        Set<ResourceLocation> allowedItemTags,
        Set<ResourceLocation> blockedItemTags
) {
    public static final PickupDropPolicy FULL = new PickupDropPolicy(true, true, Set.of(), Set.of(), Set.of(), Set.of());

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canPickupItems = true;
        private boolean canDropItems = true;
        private final Set<ResourceLocation> allowedItemIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedItemIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> allowedItemTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedItemTags = new LinkedHashSet<>();

        private Builder() {}

        public Builder canPickupItems(boolean value) { this.canPickupItems = value; return this; }
        public Builder canDropItems(boolean value) { this.canDropItems = value; return this; }
        public Builder allowItem(ResourceLocation itemId) { this.allowedItemIds.add(itemId); return this; }
        public Builder blockItem(ResourceLocation itemId) { this.blockedItemIds.add(itemId); return this; }
        public Builder allowItemTag(ResourceLocation tag) { this.allowedItemTags.add(tag); return this; }
        public Builder blockItemTag(ResourceLocation tag) { this.blockedItemTags.add(tag); return this; }

        public PickupDropPolicy build() {
            return new PickupDropPolicy(
                    canPickupItems,
                    canDropItems,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedItemIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedItemIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedItemTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedItemTags))
            );
        }
    }

    PickupDropPolicy mergeRestrictive(PickupDropPolicy other) {
        Set<ResourceLocation> mergedAllowedIds = new LinkedHashSet<>(this.allowedItemIds);
        mergedAllowedIds.addAll(other.allowedItemIds);
        Set<ResourceLocation> mergedBlockedIds = new LinkedHashSet<>(this.blockedItemIds);
        mergedBlockedIds.addAll(other.blockedItemIds);
        Set<ResourceLocation> mergedAllowed = new LinkedHashSet<>(this.allowedItemTags);
        mergedAllowed.addAll(other.allowedItemTags);
        Set<ResourceLocation> mergedBlocked = new LinkedHashSet<>(this.blockedItemTags);
        mergedBlocked.addAll(other.blockedItemTags);
        return new PickupDropPolicy(
                this.canPickupItems && other.canPickupItems,
                this.canDropItems && other.canDropItems,
                Collections.unmodifiableSet(mergedAllowedIds),
                Collections.unmodifiableSet(mergedBlockedIds),
                Collections.unmodifiableSet(mergedAllowed),
                Collections.unmodifiableSet(mergedBlocked)
        );
    }

    public boolean allowsPickup(ItemStack stack) {
        if (!this.canPickupItems) {
            return false;
        }
        if (stack.isEmpty()) {
            return true;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (this.blockedItemIds.contains(itemId) || matchesAnyTag(stack, this.blockedItemTags)) {
            return false;
        }
        return (this.allowedItemIds.isEmpty() && this.allowedItemTags.isEmpty())
                || this.allowedItemIds.contains(itemId)
                || matchesAnyTag(stack, this.allowedItemTags);
    }

    private static boolean matchesAnyTag(ItemStack stack, Set<ResourceLocation> tagIds) {
        for (ResourceLocation tagId : tagIds) {
            if (stack.is(TagKey.create(Registries.ITEM, tagId))) {
                return true;
            }
        }
        return false;
    }
}
