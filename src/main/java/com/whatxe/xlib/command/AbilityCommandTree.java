package com.whatxe.xlib.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class AbilityCommandTree {
    private AbilityCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("abilities")
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                AbilityApi.allAbilities().stream().map(AbilityDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> AbilityAdminCommands.grant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "ability")
                                        )))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                AbilityApi.allAbilities().stream().map(AbilityDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> AbilityAdminCommands.revoke(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "ability")
                                        )))))
                .then(Commands.literal("grantonly")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                AbilityApi.allAbilities().stream().map(AbilityDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> AbilityAdminCommands.grantOnly(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "ability")
                                        )))))
                .then(Commands.literal("restrict")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> AbilityAdminCommands.restrict(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                BoolArgumentType.getBool(context, "enabled")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> AbilityAdminCommands.clear(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> AbilityAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> AbilityAdminCommands.inspect(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("sources")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("ability", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                AbilityApi.allAbilities().stream().map(AbilityDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> AbilityAdminCommands.sources(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "ability")
                                        )))));
    }
}
