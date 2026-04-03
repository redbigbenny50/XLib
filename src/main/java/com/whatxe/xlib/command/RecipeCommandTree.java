package com.whatxe.xlib.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class RecipeCommandTree {
    private RecipeCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("recipes")
                .then(buildRestrictRecipeCommand())
                .then(buildUnrestrictRecipeCommand())
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                        .suggests(RecipeAdminCommands::suggestRecipeIds)
                                        .executes(context -> RecipeAdminCommands.grant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "recipe")
                                        )))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                        .suggests(RecipeAdminCommands::suggestRecipeIds)
                                        .executes(context -> RecipeAdminCommands.revoke(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "recipe")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> RecipeAdminCommands.clear(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> RecipeAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                        .suggests(RecipeAdminCommands::suggestRecipeIds)
                                        .executes(context -> RecipeAdminCommands.inspect(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "recipe")
                                        )))))
                .then(Commands.literal("sources")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                        .suggests(RecipeAdminCommands::suggestRecipeIds)
                                        .executes(context -> RecipeAdminCommands.sources(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "recipe")
                                        )))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRestrictRecipeCommand() {
        return Commands.literal("restrict")
                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                        .suggests(RecipeAdminCommands::suggestRecipeIds)
                        .executes(context -> RecipeAdminCommands.restrict(
                                context.getSource(),
                                ResourceLocationArgument.getId(context, "recipe"),
                                true
                        ))
                        .then(Commands.argument("hidden_when_locked", BoolArgumentType.bool())
                                .executes(context -> RecipeAdminCommands.restrict(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "recipe"),
                                        BoolArgumentType.getBool(context, "hidden_when_locked")
                                ))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildUnrestrictRecipeCommand() {
        return Commands.literal("unrestrict")
                .then(Commands.argument("recipe", ResourceLocationArgument.id())
                        .suggests(RecipeAdminCommands::suggestRecipeIds)
                        .executes(context -> RecipeAdminCommands.unrestrict(
                                context.getSource(),
                                ResourceLocationArgument.getId(context, "recipe")
                        )));
    }
}
