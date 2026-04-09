package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public record ArtifactDefinition(
        ResourceLocation id,
        Set<ResourceLocation> itemIds,
        Set<ArtifactPresenceMode> presenceModes,
        Set<ResourceLocation> equippedBundles,
        Set<ResourceLocation> unlockedBundles,
        List<AbilityRequirement> requirements,
        boolean unlockOnConsume
) {
    public ArtifactDefinition {
        Objects.requireNonNull(id, "id");
        itemIds = Set.copyOf(itemIds);
        presenceModes = Set.copyOf(presenceModes);
        equippedBundles = Set.copyOf(equippedBundles);
        unlockedBundles = Set.copyOf(unlockedBundles);
        requirements = List.copyOf(requirements);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public boolean matchesItem(ResourceLocation itemId) {
        return this.itemIds.contains(itemId);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        private final Set<ArtifactPresenceMode> presenceModes = new LinkedHashSet<>();
        private final Set<ResourceLocation> equippedBundles = new LinkedHashSet<>();
        private final Set<ResourceLocation> unlockedBundles = new LinkedHashSet<>();
        private final List<AbilityRequirement> requirements = new ArrayList<>();
        private boolean unlockOnConsume;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
            this.presenceModes.add(ArtifactPresenceMode.INVENTORY);
        }

        public Builder item(ItemLike itemLike) {
            Item item = Objects.requireNonNull(itemLike, "itemLike").asItem();
            return itemId(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
        }

        public Builder itemId(ResourceLocation itemId) {
            this.itemIds.add(Objects.requireNonNull(itemId, "itemId"));
            return this;
        }

        public Builder itemIds(Collection<ResourceLocation> itemIds) {
            itemIds.stream().filter(Objects::nonNull).forEach(this.itemIds::add);
            return this;
        }

        public Builder presence(ArtifactPresenceMode presenceMode) {
            this.presenceModes.add(Objects.requireNonNull(presenceMode, "presenceMode"));
            return this;
        }

        public Builder presenceModes(Collection<ArtifactPresenceMode> presenceModes) {
            presenceModes.stream().filter(Objects::nonNull).forEach(this.presenceModes::add);
            return this;
        }

        public Builder equippedBundle(ResourceLocation bundleId) {
            this.equippedBundles.add(Objects.requireNonNull(bundleId, "bundleId"));
            return this;
        }

        public Builder equippedBundles(Collection<ResourceLocation> bundleIds) {
            bundleIds.stream().filter(Objects::nonNull).forEach(this.equippedBundles::add);
            return this;
        }

        public Builder unlockedBundle(ResourceLocation bundleId) {
            this.unlockedBundles.add(Objects.requireNonNull(bundleId, "bundleId"));
            return this;
        }

        public Builder unlockedBundles(Collection<ResourceLocation> bundleIds) {
            bundleIds.stream().filter(Objects::nonNull).forEach(this.unlockedBundles::add);
            return this;
        }

        public Builder requirement(AbilityRequirement requirement) {
            this.requirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        public Builder requirements(Collection<AbilityRequirement> requirements) {
            requirements.stream().filter(Objects::nonNull).forEach(this.requirements::add);
            return this;
        }

        public Builder unlockOnConsume() {
            this.unlockOnConsume = true;
            return this;
        }

        public ArtifactDefinition build() {
            if (this.itemIds.isEmpty()) {
                throw new IllegalStateException("Artifacts require at least one item id");
            }
            return new ArtifactDefinition(
                    this.id,
                    this.itemIds,
                    this.presenceModes,
                    this.equippedBundles,
                    this.unlockedBundles,
                    this.requirements,
                    this.unlockOnConsume
            );
        }
    }
}
