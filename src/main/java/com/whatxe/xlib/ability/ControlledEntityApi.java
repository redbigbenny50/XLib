package com.whatxe.xlib.ability;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ControlledEntityApi {
    private static final String CONTROL_COMMAND_KEY = "xlib_control_command";

    private ControlledEntityApi() {}

    public static void bind(Entity controlledEntity, ResourceLocation relationshipId, Entity controller) {
        EntityRelationshipApi.setOwner(controlledEntity, relationshipId, controller);
    }

    public static void release(Entity controlledEntity, ResourceLocation relationshipId) {
        EntityRelationshipApi.clear(controlledEntity, relationshipId);
    }

    public static Optional<UUID> controllerId(Entity controlledEntity, ResourceLocation relationshipId) {
        return EntityRelationshipApi.ownerId(controlledEntity, relationshipId);
    }

    public static Optional<Entity> controller(Entity controlledEntity, ResourceLocation relationshipId) {
        return EntityRelationshipApi.owner(controlledEntity, relationshipId);
    }

    public static boolean isControlledBy(Entity controlledEntity, Entity controller, ResourceLocation relationshipId) {
        return EntityRelationshipApi.isRelatedTo(controlledEntity, controller, relationshipId);
    }

    public static List<LivingEntity> controlledEntities(ServerLevel level, Entity controller, ResourceLocation relationshipId) {
        return EntityRelationshipApi.relatedEntities(level, controller, relationshipId).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();
    }

    public static void setCommand(Entity controlledEntity, ResourceLocation commandId) {
        controlledEntity.getPersistentData().putString(CONTROL_COMMAND_KEY, commandId.toString());
    }

    public static Optional<ResourceLocation> currentCommand(Entity controlledEntity) {
        String rawCommandId = controlledEntity.getPersistentData().getString(CONTROL_COMMAND_KEY);
        if (rawCommandId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ResourceLocation.parse(rawCommandId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static void clearCommand(Entity controlledEntity) {
        controlledEntity.getPersistentData().remove(CONTROL_COMMAND_KEY);
    }
}
