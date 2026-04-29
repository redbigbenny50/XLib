package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.body.BodyTransitionApi;
import com.whatxe.xlib.body.BodyTransitionState;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class BodyTransitionCommandTree {
    private BodyTransitionCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("body")
                .then(Commands.literal("return")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> returnBody(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearTransition(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> getTransition(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))));
    }

    private static int returnBody(CommandSourceStack source, ServerPlayer player) {
        boolean success = BodyTransitionApi.returnToOrigin(player, BodyTransitionApi.COMMAND_SOURCE);
        if (success) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " returned to origin body"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Cannot return: no reversible transition active for " + player.getName().getString()));
            return 0;
        }
    }

    private static int clearTransition(CommandSourceStack source, ServerPlayer player) {
        boolean success = BodyTransitionApi.clear(player, BodyTransitionApi.COMMAND_SOURCE);
        if (success) {
            source.sendSuccess(() -> Component.literal("Cleared body transition for " + player.getName().getString()), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no active body transition"), false);
            return 0;
        }
    }

    private static int getTransition(CommandSourceStack source, ServerPlayer player) {
        Optional<BodyTransitionState> state = BodyTransitionApi.active(player);
        if (state.isEmpty()) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no active body transition"), false);
            return 0;
        }
        BodyTransitionState s = state.get();
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " body: " +
                s.transitionId() + " -> " + s.currentBodyEntityId() +
                s.originBodyEntityId().map(id -> " (origin: " + id + ")").orElse("") +
                " [" + s.status() + "]"), false);
        return 1;
    }
}
