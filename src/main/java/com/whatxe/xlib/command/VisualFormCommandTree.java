package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.whatxe.xlib.form.VisualFormApi;
import com.whatxe.xlib.form.VisualFormDefinition;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class VisualFormCommandTree {
    private VisualFormCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("visual_form")
                .then(Commands.literal("apply")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("form", ResourceLocationArgument.id())
                                        .executes(ctx -> applyForm(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "form")
                                        )))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("form", ResourceLocationArgument.id())
                                        .executes(ctx -> revokeForm(
                                                ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                ResourceLocationArgument.getId(ctx, "form")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> clearForms(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> getForm(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")
                                ))));
    }

    private static int applyForm(CommandSourceStack source, ServerPlayer player,
            net.minecraft.resources.ResourceLocation formId) {
        if (VisualFormApi.findDefinition(formId).isEmpty()) {
            source.sendFailure(Component.literal("Unknown visual form: " + formId));
            return 0;
        }
        VisualFormApi.apply(player, formId, VisualFormApi.COMMAND_SOURCE);
        source.sendSuccess(() -> Component.literal("Applied form " + formId + " to " + player.getName().getString()), true);
        return 1;
    }

    private static int revokeForm(CommandSourceStack source, ServerPlayer player,
            net.minecraft.resources.ResourceLocation formId) {
        VisualFormApi.revoke(player, formId, VisualFormApi.COMMAND_SOURCE);
        source.sendSuccess(() -> Component.literal("Revoked form " + formId + " from " + player.getName().getString()), true);
        return 1;
    }

    private static int clearForms(CommandSourceStack source, ServerPlayer player) {
        VisualFormApi.clearAll(player);
        source.sendSuccess(() -> Component.literal("Cleared all visual forms for " + player.getName().getString()), true);
        return 1;
    }

    private static int getForm(CommandSourceStack source, ServerPlayer player) {
        Optional<VisualFormDefinition> active = VisualFormApi.active(player);
        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.literal(player.getName().getString() + " has no active visual form"), false);
            return 0;
        }
        VisualFormDefinition def = active.get();
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " visual form: " +
                def.id() + " (" + def.kind() + ", scale=" + def.renderScale() + ")"), false);
        return 1;
    }
}
