package com.whatxe.xlib.ability;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class EntityRelationshipApi {
    private static final String OWNER_RELATIONSHIPS_KEY = "xlib_relationship_owners";
    private static final String RELATED_ENTITY_IDS_KEY = "xlib_relationship_members";

    private EntityRelationshipApi() {}

    public static void setOwner(Entity target, ResourceLocation relationshipId, Entity owner) {
        setOwner(target, relationshipId, owner.getUUID(), owner);
    }

    public static void setOwner(Entity target, ResourceLocation relationshipId, UUID ownerId) {
        setOwner(target, relationshipId, ownerId, resolveEntity(target, ownerId).orElse(null));
    }

    public static void clear(Entity target, ResourceLocation relationshipId) {
        Optional<UUID> previousOwnerId = ownerId(target, relationshipId);
        if (previousOwnerId.isEmpty()) {
            return;
        }

        CompoundTag ownersRoot = target.getPersistentData().getCompound(OWNER_RELATIONSHIPS_KEY);
        ownersRoot.remove(relationshipId.toString());
        if (ownersRoot.isEmpty()) {
            target.getPersistentData().remove(OWNER_RELATIONSHIPS_KEY);
        } else {
            target.getPersistentData().put(OWNER_RELATIONSHIPS_KEY, ownersRoot);
        }

        resolveEntity(target, previousOwnerId.get()).ifPresent(owner -> removeRelatedEntity(owner, relationshipId, target.getUUID()));
    }

    public static Optional<UUID> ownerId(Entity target, ResourceLocation relationshipId) {
        CompoundTag ownersRoot = target.getPersistentData().getCompound(OWNER_RELATIONSHIPS_KEY);
        String key = relationshipId.toString();
        return ownersRoot.hasUUID(key) ? Optional.of(ownersRoot.getUUID(key)) : Optional.empty();
    }

    public static Optional<Entity> owner(Entity target, ResourceLocation relationshipId) {
        return ownerId(target, relationshipId).flatMap(ownerId -> resolveEntity(target, ownerId));
    }

    public static boolean hasRelationship(Entity target, ResourceLocation relationshipId) {
        return ownerId(target, relationshipId).isPresent();
    }

    public static boolean isRelatedTo(Entity target, Entity owner, ResourceLocation relationshipId) {
        return ownerId(target, relationshipId).filter(owner.getUUID()::equals).isPresent();
    }

    public static boolean matchesAny(Entity target, Entity owner, Collection<ResourceLocation> relationshipIds) {
        if (relationshipIds.isEmpty()) {
            return true;
        }
        return relationshipIds.stream().anyMatch(relationshipId -> isRelatedTo(target, owner, relationshipId));
    }

    public static Set<ResourceLocation> relationshipIds(Entity target) {
        Set<ResourceLocation> relationshipIds = new LinkedHashSet<>();
        for (String rawId : target.getPersistentData().getCompound(OWNER_RELATIONSHIPS_KEY).getAllKeys()) {
            try {
                relationshipIds.add(ResourceLocation.parse(rawId));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed persisted ids rather than failing the entire relationship view.
            }
        }
        return Set.copyOf(relationshipIds);
    }

    public static Set<UUID> relatedEntityIds(Entity owner, ResourceLocation relationshipId) {
        CompoundTag membersRoot = owner.getPersistentData().getCompound(RELATED_ENTITY_IDS_KEY);
        String key = relationshipId.toString();
        if (!membersRoot.contains(key, Tag.TAG_COMPOUND)) {
            return Set.of();
        }

        Set<UUID> relatedEntityIds = new LinkedHashSet<>();
        CompoundTag relationshipMembers = membersRoot.getCompound(key);
        for (String rawUuid : relationshipMembers.getAllKeys()) {
            try {
                relatedEntityIds.add(UUID.fromString(rawUuid));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed persisted ids rather than failing the entire relationship view.
            }
        }
        return Set.copyOf(relatedEntityIds);
    }

    public static List<Entity> relatedEntities(ServerLevel level, Entity owner, ResourceLocation relationshipId) {
        return relatedEntityIds(owner, relationshipId).stream()
                .map(level::getEntity)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static void setOwner(
            Entity target,
            ResourceLocation relationshipId,
            UUID ownerId,
            Entity owner
    ) {
        Optional<UUID> previousOwnerId = ownerId(target, relationshipId);
        if (previousOwnerId.filter(ownerId::equals).isPresent()) {
            if (owner != null) {
                addRelatedEntity(owner, relationshipId, target.getUUID());
            }
            return;
        }

        previousOwnerId.flatMap(previousId -> resolveEntity(target, previousId))
                .ifPresent(previousOwner -> removeRelatedEntity(previousOwner, relationshipId, target.getUUID()));

        CompoundTag ownersRoot = target.getPersistentData().getCompound(OWNER_RELATIONSHIPS_KEY);
        ownersRoot.putUUID(relationshipId.toString(), ownerId);
        target.getPersistentData().put(OWNER_RELATIONSHIPS_KEY, ownersRoot);
        if (owner != null) {
            addRelatedEntity(owner, relationshipId, target.getUUID());
        }
    }

    private static void addRelatedEntity(Entity owner, ResourceLocation relationshipId, UUID targetId) {
        CompoundTag membersRoot = owner.getPersistentData().getCompound(RELATED_ENTITY_IDS_KEY);
        String key = relationshipId.toString();
        CompoundTag relationshipMembers = membersRoot.contains(key, Tag.TAG_COMPOUND)
                ? membersRoot.getCompound(key)
                : new CompoundTag();
        relationshipMembers.putBoolean(targetId.toString(), true);
        membersRoot.put(key, relationshipMembers);
        owner.getPersistentData().put(RELATED_ENTITY_IDS_KEY, membersRoot);
    }

    private static void removeRelatedEntity(Entity owner, ResourceLocation relationshipId, UUID targetId) {
        CompoundTag membersRoot = owner.getPersistentData().getCompound(RELATED_ENTITY_IDS_KEY);
        String key = relationshipId.toString();
        if (!membersRoot.contains(key, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag relationshipMembers = membersRoot.getCompound(key);
        relationshipMembers.remove(targetId.toString());
        if (relationshipMembers.isEmpty()) {
            membersRoot.remove(key);
        } else {
            membersRoot.put(key, relationshipMembers);
        }
        if (membersRoot.isEmpty()) {
            owner.getPersistentData().remove(RELATED_ENTITY_IDS_KEY);
        } else {
            owner.getPersistentData().put(RELATED_ENTITY_IDS_KEY, membersRoot);
        }
    }

    private static Optional<Entity> resolveEntity(Entity reference, UUID entityId) {
        if (!(reference.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        Entity entity = serverLevel.getEntity(entityId);
        if (entity != null) {
            return Optional.of(entity);
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entityId);
        return Optional.ofNullable(player);
    }
}
