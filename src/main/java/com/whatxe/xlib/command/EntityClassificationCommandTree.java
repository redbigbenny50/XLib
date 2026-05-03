package com.whatxe.xlib.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

final class EntityClassificationCommandTree {
    private EntityClassificationCommandTree() {}

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("classification")
                .then(Commands.literal("grant_entity_type")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                        .suggests(EntityClassificationAdminCommands::suggestEntityTypeIds)
                                        .executes(context -> EntityClassificationAdminCommands.grantEntityType(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                ResourceLocationArgument.getId(context, "entity_type")
                                        ))
                                        .then(Commands.argument("source", ResourceLocationArgument.id())
                                                .executes(context -> EntityClassificationAdminCommands.grantEntityType(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        ResourceLocationArgument.getId(context, "entity_type"),
                                                        ResourceLocationArgument.getId(context, "source")
                                                ))))))
                .then(Commands.literal("revoke_entity_type")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                        .suggests(EntityClassificationAdminCommands::suggestEntityTypeIds)
                                        .executes(context -> EntityClassificationAdminCommands.revokeEntityType(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                ResourceLocationArgument.getId(context, "entity_type")
                                        ))
                                        .then(Commands.argument("source", ResourceLocationArgument.id())
                                                .executes(context -> EntityClassificationAdminCommands.revokeEntityType(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        ResourceLocationArgument.getId(context, "entity_type"),
                                                        ResourceLocationArgument.getId(context, "source")
                                                ))))))
                .then(Commands.literal("grant_tag")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("tag", ResourceLocationArgument.id())
                                        .executes(context -> EntityClassificationAdminCommands.grantTag(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                ResourceLocationArgument.getId(context, "tag")
                                        ))
                                        .then(Commands.argument("source", ResourceLocationArgument.id())
                                                .executes(context -> EntityClassificationAdminCommands.grantTag(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        ResourceLocationArgument.getId(context, "tag"),
                                                        ResourceLocationArgument.getId(context, "source")
                                                ))))))
                .then(Commands.literal("revoke_tag")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("tag", ResourceLocationArgument.id())
                                        .executes(context -> EntityClassificationAdminCommands.revokeTag(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                ResourceLocationArgument.getId(context, "tag")
                                        ))
                                        .then(Commands.argument("source", ResourceLocationArgument.id())
                                                .executes(context -> EntityClassificationAdminCommands.revokeTag(
                                                        context.getSource(),
                                                        EntityArgument.getEntity(context, "target"),
                                                        ResourceLocationArgument.getId(context, "tag"),
                                                        ResourceLocationArgument.getId(context, "source")
                                                ))))))
                .then(Commands.literal("clear_source")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("source", ResourceLocationArgument.id())
                                        .executes(context -> EntityClassificationAdminCommands.clearSource(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "target"),
                                                ResourceLocationArgument.getId(context, "source")
                                        )))))
                .then(Commands.literal("clear_all")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> EntityClassificationAdminCommands.clearAll(
                                        context.getSource(),
                                        EntityArgument.getEntity(context, "target")
                                ))))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> EntityClassificationAdminCommands.list(
                                        context.getSource(),
                                        EntityArgument.getEntity(context, "target")
                                ))));
    }
}
