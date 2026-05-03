package com.whatxe.xlib.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.binding.EntityBindingApi;
import com.whatxe.xlib.binding.EntityBindingEndReason;
import com.whatxe.xlib.binding.EntityBindingState;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class EntityBindingCommandTree {
    private EntityBindingCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("bindings")
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> listBindings(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))))
                .then(Commands.literal("unbind")
                        .then(Commands.argument("instance_id", StringArgumentType.word())
                                .executes(ctx -> unbindInstance(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "instance_id")
                                ))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearBindings(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))));
    }

    private static int listBindings(CommandSourceStack source, ServerPlayer player) {
        List<EntityBindingState> states = EntityBindingApi.bindings(player);
        if (states.isEmpty()) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no active bindings"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " bindings (" + states.size() + "):"), false);
        for (EntityBindingState state : states) {
            boolean isPrimary = state.primaryEntityId().equals(player.getUUID());
            String role = isPrimary ? "primary" : "secondary";
            String timer = state.remainingTicks().map(t -> " " + t + "t remaining").orElse("");
            source.sendSuccess(() -> Component.literal("  [" + state.bindingId() + "] " +
                    state.bindingInstanceId() + " (" + role + ")" + timer), false);
        }
        return states.size();
    }

    private static int unbindInstance(CommandSourceStack source, String instanceIdStr) {
        try {
            UUID instanceId = UUID.fromString(instanceIdStr);
            boolean removed = EntityBindingApi.unbind(instanceId, EntityBindingEndReason.MANUAL);
            if (removed) {
                source.sendSuccess(() -> Component.literal("Unbound binding " + instanceId), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("No active binding with id " + instanceId));
                return 0;
            }
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid UUID: " + instanceIdStr));
            return 0;
        }
    }

    private static int clearBindings(CommandSourceStack source, ServerPlayer player) {
        List<EntityBindingState> states = EntityBindingApi.bindings(player);
        int count = 0;
        for (EntityBindingState state : states) {
            if (EntityBindingApi.unbind(state.bindingInstanceId(), EntityBindingEndReason.MANUAL)) count++;
        }
        final int cleared = count;
        source.sendSuccess(() -> Component.literal("Cleared " + cleared + " binding(s) for " + player.getName().getString()), false);
        return cleared;
    }
}
