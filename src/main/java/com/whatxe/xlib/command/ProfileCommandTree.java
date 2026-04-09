package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileDefinition;
import com.whatxe.xlib.ability.ProfileGroupDefinition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class ProfileCommandTree {
    private ProfileCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("profiles")
                .then(Commands.literal("catalog")
                        .executes(context -> ProfileAdminCommands.catalog(context.getSource())))
                .then(Commands.literal("groups")
                        .executes(context -> ProfileAdminCommands.groups(context.getSource())))
                .then(Commands.literal("claim")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("profile", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                ProfileApi.allProfiles().stream().map(ProfileDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> ProfileAdminCommands.claim(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "profile")
                                        )))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("group", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                ProfileApi.allGroups().stream().map(ProfileGroupDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> ProfileAdminCommands.reset(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "group")
                                        )))))
                .then(Commands.literal("reopen")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("group", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                ProfileApi.allGroups().stream().map(ProfileGroupDefinition::id),
                                                builder
                                        ))
                                        .executes(context -> ProfileAdminCommands.reopen(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "group")
                                        )))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> ProfileAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("pending")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> ProfileAdminCommands.pending(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("resync")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> ProfileAdminCommands.resync(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }
}
