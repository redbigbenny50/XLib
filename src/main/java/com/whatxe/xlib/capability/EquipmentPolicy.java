package com.whatxe.xlib.capability;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record EquipmentPolicy(
        boolean canEquipArmor,
        boolean canUnequipArmor,
        boolean canEquipHeldItems,
        Set<ResourceLocation> allowedArmorItemIds,
        Set<ResourceLocation> blockedArmorItemIds,
        Set<ResourceLocation> allowedArmorItemTags,
        Set<ResourceLocation> blockedArmorItemTags,
        Set<String> suppressedArmorSlots
) {
    public static final EquipmentPolicy FULL = new EquipmentPolicy(true, true, true, Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canEquipArmor = true;
        private boolean canUnequipArmor = true;
        private boolean canEquipHeldItems = true;
        private final Set<ResourceLocation> allowedArmorItemIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedArmorItemIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> allowedArmorItemTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedArmorItemTags = new LinkedHashSet<>();
        private final Set<String> suppressedArmorSlots = new LinkedHashSet<>();

        private Builder() {}

        public Builder canEquipArmor(boolean value) { this.canEquipArmor = value; return this; }
        public Builder canUnequipArmor(boolean value) { this.canUnequipArmor = value; return this; }
        public Builder canEquipHeldItems(boolean value) { this.canEquipHeldItems = value; return this; }
        public Builder allowArmorItem(ResourceLocation itemId) { this.allowedArmorItemIds.add(itemId); return this; }
        public Builder blockArmorItem(ResourceLocation itemId) { this.blockedArmorItemIds.add(itemId); return this; }
        public Builder allowArmorItemTag(ResourceLocation tagId) { this.allowedArmorItemTags.add(tagId); return this; }
        public Builder blockArmorItemTag(ResourceLocation tagId) { this.blockedArmorItemTags.add(tagId); return this; }
        public Builder suppressArmorSlot(String slotName) {
            this.suppressedArmorSlots.add(slotName.toLowerCase(Locale.ROOT));
            return this;
        }

        public EquipmentPolicy build() {
            return new EquipmentPolicy(
                    canEquipArmor,
                    canUnequipArmor,
                    canEquipHeldItems,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedArmorItemIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedArmorItemIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedArmorItemTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedArmorItemTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(suppressedArmorSlots))
            );
        }
    }

    EquipmentPolicy mergeRestrictive(EquipmentPolicy other) {
        return new EquipmentPolicy(
                this.canEquipArmor && other.canEquipArmor,
                this.canUnequipArmor && other.canUnequipArmor,
                this.canEquipHeldItems && other.canEquipHeldItems,
                mergeUnion(this.allowedArmorItemIds, other.allowedArmorItemIds),
                mergeUnion(this.blockedArmorItemIds, other.blockedArmorItemIds),
                mergeUnion(this.allowedArmorItemTags, other.allowedArmorItemTags),
                mergeUnion(this.blockedArmorItemTags, other.blockedArmorItemTags),
                mergeUnionStrings(this.suppressedArmorSlots, other.suppressedArmorSlots)
        );
    }

    public boolean suppressesSlot(EquipmentSlot slot) {
        return this.suppressedArmorSlots.contains(slot.getName().toLowerCase(Locale.ROOT));
    }

    public boolean allowsArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (this.blockedArmorItemIds.contains(itemId) || matchesAnyTag(stack, this.blockedArmorItemTags)) {
            return false;
        }
        return (this.allowedArmorItemIds.isEmpty() && this.allowedArmorItemTags.isEmpty())
                || this.allowedArmorItemIds.contains(itemId)
                || matchesAnyTag(stack, this.allowedArmorItemTags);
    }

    private static boolean matchesAnyTag(ItemStack stack, Set<ResourceLocation> tagIds) {
        for (ResourceLocation tagId : tagIds) {
            if (stack.is(TagKey.create(Registries.ITEM, tagId))) {
                return true;
            }
        }
        return false;
    }

    private static Set<ResourceLocation> mergeUnion(Set<ResourceLocation> left, Set<ResourceLocation> right) {
        LinkedHashSet<ResourceLocation> merged = new LinkedHashSet<>(left);
        merged.addAll(right);
        return Collections.unmodifiableSet(merged);
    }

    private static Set<String> mergeUnionStrings(Set<String> left, Set<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(left);
        merged.addAll(right);
        return Collections.unmodifiableSet(merged);
    }
}
