package com.whatxe.xlib.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class DebugCommandTree {
    private DebugCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("debug")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> DebugAdminCommands.dump(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "target")
                        )))
                .then(Commands.literal("counters")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> DebugAdminCommands.counters(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("source")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("source", ResourceLocationArgument.id())
                                        .executes(context -> DebugAdminCommands.source(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "source")
                                        )))))
                .then(Commands.literal("diff")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("snapshot", StringArgumentType.word())
                                        .executes(context -> DebugAdminCommands.diff(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                StringArgumentType.getString(context, "snapshot")
                                        )))))
                .then(Commands.literal("export")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> DebugAdminCommands.export(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))));
    }
}
