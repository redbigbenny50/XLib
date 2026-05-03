package com.whatxe.xlib.capability;

import com.whatxe.xlib.classification.EntityClassificationApi;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public record InteractionPolicy(
        boolean canInteractWithBlocks,
        boolean canInteractWithEntities,
        boolean canUseBeds,
        boolean canRideEntities,
        boolean canAttackPlayers,
        boolean canAttackMobs,
        Set<ResourceLocation> allowedBlockIds,
        Set<ResourceLocation> blockedBlockIds,
        Set<ResourceLocation> allowedBlockTags,
        Set<ResourceLocation> blockedBlockTags,
        Set<ResourceLocation> allowedEntityIds,
        Set<ResourceLocation> blockedEntityIds,
        Set<ResourceLocation> allowedEntityTags,
        Set<ResourceLocation> blockedEntityTags
) {
    public static final InteractionPolicy FULL = new InteractionPolicy(
            true, true, true, true, true, true,
            Set.of(), Set.of(), Set.of(), Set.of(),
            Set.of(), Set.of(), Set.of(), Set.of()
    );

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean canInteractWithBlocks = true;
        private boolean canInteractWithEntities = true;
        private boolean canUseBeds = true;
        private boolean canRideEntities = true;
        private boolean canAttackPlayers = true;
        private boolean canAttackMobs = true;
        private final Set<ResourceLocation> allowedBlockIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedBlockIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> allowedBlockTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedBlockTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> allowedEntityIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedEntityIds = new LinkedHashSet<>();
        private final Set<ResourceLocation> allowedEntityTags = new LinkedHashSet<>();
        private final Set<ResourceLocation> blockedEntityTags = new LinkedHashSet<>();

        private Builder() {}

        public Builder canInteractWithBlocks(boolean value) { this.canInteractWithBlocks = value; return this; }
        public Builder canInteractWithEntities(boolean value) { this.canInteractWithEntities = value; return this; }
        public Builder canUseBeds(boolean value) { this.canUseBeds = value; return this; }
        public Builder canRideEntities(boolean value) { this.canRideEntities = value; return this; }
        public Builder canAttackPlayers(boolean value) { this.canAttackPlayers = value; return this; }
        public Builder canAttackMobs(boolean value) { this.canAttackMobs = value; return this; }
        public Builder allowBlock(ResourceLocation blockId) { this.allowedBlockIds.add(blockId); return this; }
        public Builder blockBlock(ResourceLocation blockId) { this.blockedBlockIds.add(blockId); return this; }
        public Builder allowBlockTag(ResourceLocation tagId) { this.allowedBlockTags.add(tagId); return this; }
        public Builder blockBlockTag(ResourceLocation tagId) { this.blockedBlockTags.add(tagId); return this; }
        public Builder allowEntity(ResourceLocation entityId) { this.allowedEntityIds.add(entityId); return this; }
        public Builder blockEntity(ResourceLocation entityId) { this.blockedEntityIds.add(entityId); return this; }
        public Builder allowEntityTag(ResourceLocation tagId) { this.allowedEntityTags.add(tagId); return this; }
        public Builder blockEntityTag(ResourceLocation tagId) { this.blockedEntityTags.add(tagId); return this; }

        public InteractionPolicy build() {
            return new InteractionPolicy(
                    canInteractWithBlocks,
                    canInteractWithEntities,
                    canUseBeds,
                    canRideEntities,
                    canAttackPlayers,
                    canAttackMobs,
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedBlockIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedBlockIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedBlockTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedBlockTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedEntityIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedEntityIds)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(allowedEntityTags)),
                    Collections.unmodifiableSet(new LinkedHashSet<>(blockedEntityTags))
            );
        }
    }

    InteractionPolicy mergeRestrictive(InteractionPolicy other) {
        return new InteractionPolicy(
                this.canInteractWithBlocks && other.canInteractWithBlocks,
                this.canInteractWithEntities && other.canInteractWithEntities,
                this.canUseBeds && other.canUseBeds,
                this.canRideEntities && other.canRideEntities,
                this.canAttackPlayers && other.canAttackPlayers,
                this.canAttackMobs && other.canAttackMobs,
                mergeUnion(this.allowedBlockIds, other.allowedBlockIds),
                mergeUnion(this.blockedBlockIds, other.blockedBlockIds),
                mergeUnion(this.allowedBlockTags, other.allowedBlockTags),
                mergeUnion(this.blockedBlockTags, other.blockedBlockTags),
                mergeUnion(this.allowedEntityIds, other.allowedEntityIds),
                mergeUnion(this.blockedEntityIds, other.blockedEntityIds),
                mergeUnion(this.allowedEntityTags, other.allowedEntityTags),
                mergeUnion(this.blockedEntityTags, other.blockedEntityTags)
        );
    }

    public boolean allowsBlock(BlockState blockState) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (this.blockedBlockIds.contains(blockId) || matchesAnyBlockTag(blockState, this.blockedBlockTags)) {
            return false;
        }
        return (this.allowedBlockIds.isEmpty() && this.allowedBlockTags.isEmpty())
                || this.allowedBlockIds.contains(blockId)
                || matchesAnyBlockTag(blockState, this.allowedBlockTags);
    }

    public boolean allowsEntity(Entity entity) {
        if (EntityClassificationApi.matchesSelector(entity, this.blockedEntityIds, this.blockedEntityTags)) {
            return false;
        }
        return EntityClassificationApi.matchesSelector(entity, this.allowedEntityIds, this.allowedEntityTags);
    }

    private static boolean matchesAnyBlockTag(BlockState blockState, Set<ResourceLocation> tagIds) {
        for (ResourceLocation tagId : tagIds) {
            if (blockState.is(TagKey.create(Registries.BLOCK, tagId))) {
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
}
