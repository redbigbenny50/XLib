package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.lifecycle.LifecycleStageApi;
import com.whatxe.xlib.lifecycle.LifecycleStageState;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class LifecycleStageCommandTree {
    private LifecycleStageCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("stages")
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("stage", ResourceLocationArgument.id())
                                        .executes(ctx -> setStage(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "stage")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearStage(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> getStage(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))))
                .then(Commands.literal("transition")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("target_stage", ResourceLocationArgument.id())
                                        .executes(ctx -> requestTransition(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "target_stage")
                                        )))));
    }

    private static int setStage(CommandSourceStack source, ServerPlayer player,
            net.minecraft.resources.ResourceLocation stageId) {
        boolean success = LifecycleStageApi.setStage(player, stageId, LifecycleStageApi.COMMAND_SOURCE);
        if (success) {
            source.sendSuccess(() -> Component.literal("Set stage " + stageId + " on " + player.getName().getString()), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Unknown stage: " + stageId));
            return 0;
        }
    }

    private static int clearStage(CommandSourceStack source, ServerPlayer player) {
        boolean success = LifecycleStageApi.clearStage(player, LifecycleStageApi.COMMAND_SOURCE);
        if (success) {
            source.sendSuccess(() -> Component.literal("Cleared stage for " + player.getName().getString()), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no active stage"), false);
            return 0;
        }
    }

    private static int getStage(CommandSourceStack source, ServerPlayer player) {
        Optional<LifecycleStageState> state = LifecycleStageApi.state(player);
        if (state.isEmpty()) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no active stage"), false);
            return 0;
        }
        LifecycleStageState s = state.get();
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " stage: " +
                s.currentStageId() + " (elapsed " + s.elapsedTicks() + "t" +
                s.pendingTransition().map(p -> ", pending -> " + p.targetStageId()).orElse("") + ")"), false);
        return 1;
    }

    private static int requestTransition(CommandSourceStack source, ServerPlayer player,
            net.minecraft.resources.ResourceLocation targetId) {
        boolean success = LifecycleStageApi.requestTransition(player, targetId, LifecycleStageApi.COMMAND_SOURCE);
        if (success) {
            source.sendSuccess(() -> Component.literal("Transition to " + targetId + " queued for " + player.getName().getString()), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Transition not allowed to " + targetId));
            return 0;
        }
    }
}
