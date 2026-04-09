package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class PassiveCommandTree {
    private PassiveCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("passives")
                .then(Commands.literal("catalog")
                        .executes(context -> PassiveAdminCommands.catalog(context.getSource())))
                .then(Commands.literal("describe")
                        .then(Commands.argument("passive", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        PassiveApi.allPassives().stream().map(PassiveDefinition::id),
                                        builder
                                ))
                                .executes(context -> PassiveAdminCommands.describe(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "passive")
                                ))))
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("passive", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                PassiveApi.allPassives().stream().map(PassiveDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> PassiveAdminCommands.grant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "passive")
                                        )))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("passive", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                PassiveApi.allPassives().stream().map(PassiveDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> PassiveAdminCommands.revoke(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "passive")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> PassiveAdminCommands.clear(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> PassiveAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> PassiveAdminCommands.inspect(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("sources")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("passive", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                PassiveApi.allPassives().stream().map(PassiveDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> PassiveAdminCommands.sources(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "passive")
                                        )))));
    }
}
