package com.whatxe.xlib.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.GrantedItemDefinition;
import com.whatxe.xlib.ability.GrantedItemGrantApi;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class GrantedItemAdminCommands {
    private GrantedItemAdminCommands() {}

    static int grant(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation grantedItemId) throws CommandSyntaxException {
        XLibCommandSupport.validateGrantedItem(grantedItemId);
        for (ServerPlayer player : targets) {
            GrantedItemGrantApi.grant(player, grantedItemId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.items.grant", grantedItemId.toString(), targets.size()), true);
        return targets.size();
    }

    static int revoke(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation grantedItemId) throws CommandSyntaxException {
        XLibCommandSupport.validateGrantedItem(grantedItemId);
        for (ServerPlayer player : targets) {
            GrantedItemGrantApi.revoke(player, grantedItemId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.items.revoke", grantedItemId.toString(), targets.size()), true);
        return targets.size();
    }

    static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            GrantedItemGrantApi.clearGrantedItems(player);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.items.clear", targets.size()), true);
        return targets.size();
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(
                () -> Component.translatable("command.xlib.items.list", target.getName(), XLibCommandSupport.joinIds(GrantedItemGrantApi.grantedItems(target))),
                false
        );
        return GrantedItemGrantApi.grantedItems(target).size();
    }

    static int inspect(CommandSourceStack source, ServerPlayer target) {
        Set<ResourceLocation> grantedItems = GrantedItemGrantApi.grantedItems(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | granted_items=" + XLibCommandSupport.joinIds(grantedItems)), false);
        for (ResourceLocation grantedItemId : grantedItems) {
            GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
            String definitionSummary = definition == null
                    ? "definition=missing"
                    : "undroppable=" + definition.undroppable()
                            + " | remove_when_revoked=" + definition.removeWhenRevoked()
                            + " | storage_policy=" + definition.storagePolicy().name().toLowerCase(Locale.ROOT);
            source.sendSuccess(
                    () -> Component.literal(grantedItemId + " | sources=" + XLibCommandSupport.joinIds(GrantedItemGrantApi.grantSources(target, grantedItemId))
                            + " | " + definitionSummary),
                    false
            );
            if (definition != null) {
                source.sendSuccess(
                        () -> Component.literal(grantedItemId + " | metadata="
                                + XLibCommandSupport.formatMetadata(
                                        definition.familyId(),
                                        definition.groupId(),
                                        definition.pageId(),
                                        definition.tags()
                                )),
                        false
                );
            }
        }
        return grantedItems.size();
    }

    static int sources(CommandSourceStack source, ServerPlayer target, ResourceLocation grantedItemId) throws CommandSyntaxException {
        XLibCommandSupport.validateGrantedItem(grantedItemId);
        GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElseThrow();
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | " + grantedItemId), false);
        source.sendSuccess(() -> Component.literal("grant_sources=" + XLibCommandSupport.joinIds(GrantedItemGrantApi.grantSources(target, grantedItemId))), false);
        source.sendSuccess(() -> Component.literal(
                "undroppable=" + definition.undroppable()
                        + " | remove_when_revoked=" + definition.removeWhenRevoked()
                        + " | storage_policy=" + definition.storagePolicy().name().toLowerCase(Locale.ROOT)
        ), false);
        source.sendSuccess(
                () -> Component.literal("metadata="
                        + XLibCommandSupport.formatMetadata(
                                definition.familyId(),
                                definition.groupId(),
                                definition.pageId(),
                                definition.tags()
                        )),
                false
        );
        return 1;
    }
}
