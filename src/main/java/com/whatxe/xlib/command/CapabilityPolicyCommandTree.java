package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class CapabilityPolicyCommandTree {
    private CapabilityPolicyCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("capability_policy")
                .then(Commands.literal("apply")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("policy", ResourceLocationArgument.id())
                                        .executes(context -> CapabilityPolicyAdminCommands.apply(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "policy")
                                        )))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("policy", ResourceLocationArgument.id())
                                        .executes(context -> CapabilityPolicyAdminCommands.revoke(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target"),
                                                ResourceLocationArgument.getId(context, "policy")
                                        )))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> CapabilityPolicyAdminCommands.clear(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> CapabilityPolicyAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("debug")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> CapabilityPolicyAdminCommands.debug(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))));
    }
}
