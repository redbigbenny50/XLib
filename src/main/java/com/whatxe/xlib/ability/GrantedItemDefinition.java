package com.whatxe.xlib.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class GrantedItemDefinition {
    @FunctionalInterface
    public interface StackFactory {
        ItemStack create(ServerPlayer player, AbilityData data);
    }

    private final ResourceLocation id;
    private final StackFactory stackFactory;
    private final ResourceLocation familyId;
    private final ResourceLocation groupId;
    private final ResourceLocation pageId;
    private final Set<ResourceLocation> tags;
    private final boolean undroppable;
    private final boolean removeWhenRevoked;
    private final GrantedItemStoragePolicy storagePolicy;

    private GrantedItemDefinition(
            ResourceLocation id,
            StackFactory stackFactory,
            ResourceLocation familyId,
            ResourceLocation groupId,
            ResourceLocation pageId,
            Set<ResourceLocation> tags,
            boolean undroppable,
            boolean removeWhenRevoked,
            GrantedItemStoragePolicy storagePolicy
    ) {
        this.id = id;
        this.stackFactory = stackFactory;
        this.familyId = familyId;
        this.groupId = groupId;
        this.pageId = pageId;
        this.tags = copyTags(tags);
        this.undroppable = undroppable;
        this.removeWhenRevoked = removeWhenRevoked;
        this.storagePolicy = storagePolicy;
    }

    public static Builder builder(ResourceLocation id, ItemLike itemLike) {
        Objects.requireNonNull(itemLike, "itemLike");
        return new Builder(id, (player, data) -> new ItemStack(itemLike));
    }

    public static Builder builder(ResourceLocation id, StackFactory stackFactory) {
        return new Builder(id, stackFactory);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Optional<ResourceLocation> familyId() {
        return Optional.ofNullable(this.familyId);
    }

    public Optional<ResourceLocation> groupId() {
        return Optional.ofNullable(this.groupId);
    }

    public Optional<ResourceLocation> pageId() {
        return Optional.ofNullable(this.pageId);
    }

    public Set<ResourceLocation> tags() {
        return this.tags;
    }

    public boolean hasTag(ResourceLocation tagId) {
        return this.tags.contains(tagId);
    }

    public List<ResourceLocation> metadataIds() {
        List<ResourceLocation> ids = new ArrayList<>(3 + this.tags.size());
        if (this.familyId != null) {
            ids.add(this.familyId);
        }
        if (this.groupId != null) {
            ids.add(this.groupId);
        }
        if (this.pageId != null) {
            ids.add(this.pageId);
        }
        ids.addAll(this.tags);
        return List.copyOf(ids);
    }

    public boolean undroppable() {
        return this.undroppable;
    }

    public boolean removeWhenRevoked() {
        return this.removeWhenRevoked;
    }

    public GrantedItemStoragePolicy storagePolicy() {
        return this.storagePolicy;
    }

    public ItemStack createStack(ServerPlayer player, AbilityData data) {
        ItemStack stack = this.stackFactory.create(player, data);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static Set<ResourceLocation> copyTags(Collection<ResourceLocation> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final StackFactory stackFactory;
        private ResourceLocation familyId;
        private ResourceLocation groupId;
        private ResourceLocation pageId;
        private final Set<ResourceLocation> tags = new LinkedHashSet<>();
        private boolean undroppable;
        private boolean removeWhenRevoked = true;
        private GrantedItemStoragePolicy storagePolicy;

        private Builder(ResourceLocation id, StackFactory stackFactory) {
            this.id = Objects.requireNonNull(id, "id");
            this.stackFactory = Objects.requireNonNull(stackFactory, "stackFactory");
        }

        public Builder family(ResourceLocation familyId) {
            this.familyId = Objects.requireNonNull(familyId, "familyId");
            return this;
        }

        public Builder group(ResourceLocation groupId) {
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            return this;
        }

        public Builder page(ResourceLocation pageId) {
            this.pageId = Objects.requireNonNull(pageId, "pageId");
            return this;
        }

        public Builder tag(ResourceLocation tagId) {
            this.tags.add(Objects.requireNonNull(tagId, "tagId"));
            return this;
        }

        public Builder tags(Collection<ResourceLocation> tagIds) {
            tagIds.stream().filter(Objects::nonNull).forEach(this.tags::add);
            return this;
        }

        public Builder undroppable() {
            this.undroppable = true;
            return this;
        }

        public Builder keepWhenRevoked() {
            this.removeWhenRevoked = false;
            return this;
        }

        public Builder allowExternalStorage() {
            this.storagePolicy = GrantedItemStoragePolicy.ALLOW_STORAGE;
            return this;
        }

        public Builder reclaimFromOpenStorage() {
            this.storagePolicy = GrantedItemStoragePolicy.RECLAIM_FROM_OPEN_STORAGE;
            return this;
        }

        public Builder blockExternalStorage() {
            this.storagePolicy = GrantedItemStoragePolicy.BLOCK_EXTERNAL_STORAGE;
            return this;
        }

        public GrantedItemDefinition build() {
            GrantedItemStoragePolicy resolvedStoragePolicy = this.storagePolicy != null
                    ? this.storagePolicy
                    : (this.undroppable
                            ? GrantedItemStoragePolicy.RECLAIM_FROM_OPEN_STORAGE
                            : GrantedItemStoragePolicy.ALLOW_STORAGE);
            return new GrantedItemDefinition(
                    this.id,
                    this.stackFactory,
                    this.familyId,
                    this.groupId,
                    this.pageId,
                    this.tags,
                    this.undroppable,
                    this.removeWhenRevoked,
                    resolvedStoragePolicy
            );
        }
    }
}

