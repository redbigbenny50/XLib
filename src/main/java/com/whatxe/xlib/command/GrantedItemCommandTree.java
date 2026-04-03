package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.GrantedItemDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class GrantedItemCommandTree {
    private GrantedItemCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("items")
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                GrantedItemApi.allGrantedItems().stream().map(GrantedItemDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> GrantedItemAdminCommands.grant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "item")
                                        )))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                GrantedItemApi.allGrantedItems().stream().map(GrantedItemDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> GrantedItemAdminCommands.revoke(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "item")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> GrantedItemAdminCommands.clear(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> GrantedItemAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> GrantedItemAdminCommands.inspect(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("sources")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                GrantedItemApi.allGrantedItems().stream().map(GrantedItemDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> GrantedItemAdminCommands.sources(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "item")
                                        )))));
    }
}
