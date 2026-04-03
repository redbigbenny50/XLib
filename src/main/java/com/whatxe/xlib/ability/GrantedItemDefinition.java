package com.whatxe.xlib.ability;

import java.util.Objects;
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
    private final boolean undroppable;
    private final boolean removeWhenRevoked;
    private final GrantedItemStoragePolicy storagePolicy;

    private GrantedItemDefinition(
            ResourceLocation id,
            StackFactory stackFactory,
            boolean undroppable,
            boolean removeWhenRevoked,
            GrantedItemStoragePolicy storagePolicy
    ) {
        this.id = id;
        this.stackFactory = stackFactory;
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

    public static final class Builder {
        private final ResourceLocation id;
        private final StackFactory stackFactory;
        private boolean undroppable;
        private boolean removeWhenRevoked = true;
        private GrantedItemStoragePolicy storagePolicy;

        private Builder(ResourceLocation id, StackFactory stackFactory) {
            this.id = Objects.requireNonNull(id, "id");
            this.stackFactory = Objects.requireNonNull(stackFactory, "stackFactory");
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
                    this.undroppable,
                    this.removeWhenRevoked,
                    resolvedStoragePolicy
            );
        }
    }
}

