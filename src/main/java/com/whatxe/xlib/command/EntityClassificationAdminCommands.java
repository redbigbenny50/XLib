package com.whatxe.xlib.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.whatxe.xlib.XLib;
import com.whatxe.xlib.classification.EntityClassificationApi;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;

final class EntityClassificationAdminCommands {
    private static final DynamicCommandExceptionType UNKNOWN_ENTITY_TYPE =
            new DynamicCommandExceptionType(value -> Component.literal("Unknown entity type: " + value));
    private static final ResourceLocation COMMAND_SOURCE = ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command");

    private EntityClassificationAdminCommands() {}

    static int grantEntityType(CommandSourceStack source, Entity target, ResourceLocation entityTypeId) throws CommandSyntaxException {
        return grantEntityType(source, target, entityTypeId, COMMAND_SOURCE);
    }

    static int grantEntityType(CommandSourceStack source, Entity target, ResourceLocation entityTypeId, ResourceLocation sourceId)
            throws CommandSyntaxException {
        validateEntityType(entityTypeId);
        EntityClassificationApi.grantSyntheticEntityType(target, entityTypeId, sourceId);
        source.sendSuccess(
                () -> Component.literal("Granted synthetic entity type " + entityTypeId + " to " + target.getStringUUID() + " from " + sourceId),
                false
        );
        return 1;
    }

    static int revokeEntityType(CommandSourceStack source, Entity target, ResourceLocation entityTypeId) throws CommandSyntaxException {
        return revokeEntityType(source, target, entityTypeId, COMMAND_SOURCE);
    }

    static int revokeEntityType(CommandSourceStack source, Entity target, ResourceLocation entityTypeId, ResourceLocation sourceId)
            throws CommandSyntaxException {
        validateEntityType(entityTypeId);
        EntityClassificationApi.revokeSyntheticEntityType(target, entityTypeId, sourceId);
        source.sendSuccess(
                () -> Component.literal("Revoked synthetic entity type " + entityTypeId + " from " + target.getStringUUID() + " for " + sourceId),
                false
        );
        return 1;
    }

    static int grantTag(CommandSourceStack source, Entity target, ResourceLocation tagId) {
        return grantTag(source, target, tagId, COMMAND_SOURCE);
    }

    static int grantTag(CommandSourceStack source, Entity target, ResourceLocation tagId, ResourceLocation sourceId) {
        EntityClassificationApi.grantSyntheticTag(target, tagId, sourceId);
        source.sendSuccess(
                () -> Component.literal("Granted synthetic tag " + tagId + " to " + target.getStringUUID() + " from " + sourceId),
                false
        );
        return 1;
    }

    static int revokeTag(CommandSourceStack source, Entity target, ResourceLocation tagId) {
        return revokeTag(source, target, tagId, COMMAND_SOURCE);
    }

    static int revokeTag(CommandSourceStack source, Entity target, ResourceLocation tagId, ResourceLocation sourceId) {
        EntityClassificationApi.revokeSyntheticTag(target, tagId, sourceId);
        source.sendSuccess(
                () -> Component.literal("Revoked synthetic tag " + tagId + " from " + target.getStringUUID() + " for " + sourceId),
                false
        );
        return 1;
    }

    static int clearSource(CommandSourceStack source, Entity target, ResourceLocation sourceId) {
        EntityClassificationApi.clearSource(target, sourceId);
        source.sendSuccess(
                () -> Component.literal("Cleared synthetic classifications on " + target.getStringUUID() + " for " + sourceId),
                false
        );
        return 1;
    }

    static int clearAll(CommandSourceStack source, Entity target) {
        EntityClassificationApi.clearAll(target);
        source.sendSuccess(() -> Component.literal("Cleared all synthetic classifications on " + target.getStringUUID()), false);
        return 1;
    }

    static int list(CommandSourceStack source, Entity target) {
        ResourceLocation realEntityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        source.sendSuccess(
                () -> Component.literal(target.getStringUUID() + " | real_entity_type=" + realEntityTypeId),
                false
        );
        source.sendSuccess(
                () -> Component.literal("synthetic_entity_types=" + XLibCommandSupport.joinIds(EntityClassificationApi.syntheticEntityTypes(target))
                        + " | direct_tags=" + XLibCommandSupport.joinIds(EntityClassificationApi.directSyntheticTags(target))
                        + " | inherited_tags=" + XLibCommandSupport.joinIds(EntityClassificationApi.inheritedSyntheticTags(target))),
                false
        );
        source.sendSuccess(
                () -> Component.literal("effective_entity_types=" + XLibCommandSupport.joinIds(EntityClassificationApi.effectiveEntityTypes(target))
                        + " | effective_tags=" + XLibCommandSupport.joinIds(EntityClassificationApi.effectiveTags(target))),
                false
        );
        source.sendSuccess(
                () -> Component.literal("synthetic_entity_type_sources=" + formatSourceMap(EntityClassificationApi.syntheticEntityTypeSources(target))
                        + " | synthetic_tag_sources=" + formatSourceMap(EntityClassificationApi.syntheticTagSources(target))),
                false
        );
        return 1;
    }

    static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestEntityTypeIds(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder);
    }

    private static void validateEntityType(ResourceLocation entityTypeId) throws CommandSyntaxException {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityTypeId)) {
            throw UNKNOWN_ENTITY_TYPE.create(entityTypeId.toString());
        }
    }

    private static String formatSourceMap(Map<ResourceLocation, Set<ResourceLocation>> sourceMap) {
        if (sourceMap.isEmpty()) {
            return "-";
        }
        return sourceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "<-" + XLibCommandSupport.joinIds(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("-");
    }
}
