package com.whatxe.xlib.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.ability.PassiveGrantApi;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class PassiveAdminCommands {
    private PassiveAdminCommands() {}

    static int grant(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation passiveId) throws CommandSyntaxException {
        XLibCommandSupport.validatePassive(passiveId);
        for (ServerPlayer player : targets) {
            PassiveGrantApi.grant(player, passiveId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.passives.grant", passiveId.toString(), targets.size()), true);
        return targets.size();
    }

    static int revoke(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation passiveId) throws CommandSyntaxException {
        XLibCommandSupport.validatePassive(passiveId);
        for (ServerPlayer player : targets) {
            PassiveGrantApi.revoke(player, passiveId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.passives.revoke", passiveId.toString(), targets.size()), true);
        return targets.size();
    }

    static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            PassiveGrantApi.clearPassives(player);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.passives.clear", targets.size()), true);
        return targets.size();
    }

    static int catalog(CommandSourceStack source) {
        Collection<PassiveDefinition> passives = PassiveApi.allPassives();
        source.sendSuccess(() -> Component.literal("registered_passives=" + passives.size()), false);
        for (PassiveDefinition passive : passives.stream().sorted(java.util.Comparator.comparing(definition -> definition.id().toString())).toList()) {
            source.sendSuccess(
                    () -> Component.literal(passive.id()
                            + " | metadata=" + XLibCommandSupport.formatMetadataBlock(
                                    passive.familyId(),
                                    passive.groupId(),
                                    passive.pageId(),
                                    passive.tags()
                            )
                            + " | hooks=" + XLibCommandSupport.formatPassiveHooks(passive)
                            + " | sound_triggers=" + XLibCommandSupport.formatPassiveSoundTriggers(passive)),
                    false
            );
        }
        return passives.size();
    }

    static int describe(CommandSourceStack source, ResourceLocation passiveId) throws CommandSyntaxException {
        XLibCommandSupport.validatePassive(passiveId);
        PassiveDefinition passive = PassiveApi.findPassive(passiveId).orElseThrow();
        source.sendSuccess(() -> Component.literal(passiveId + " | display=" + passive.displayName().getString()), false);
        source.sendSuccess(
                () -> Component.literal("metadata=" + XLibCommandSupport.formatMetadataBlock(
                        passive.familyId(),
                        passive.groupId(),
                        passive.pageId(),
                        passive.tags()
                )),
                false
        );
        source.sendSuccess(
                () -> Component.literal("grant_requirements=" + XLibCommandSupport.formatRequirementDescriptions(passive.grantRequirements())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("active_requirements=" + XLibCommandSupport.formatRequirementDescriptions(passive.activeRequirements())),
                false
        );
        source.sendSuccess(
                () -> Component.literal("cooldown_multiplier=" + String.format(java.util.Locale.ROOT, "%.2f", passive.cooldownTickRateMultiplier())
                        + " | hooks=" + XLibCommandSupport.formatPassiveHooks(passive)
                        + " | sound_triggers=" + XLibCommandSupport.formatPassiveSoundTriggers(passive)),
                false
        );
        return 1;
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        Set<ResourceLocation> grantedPassives = PassiveGrantApi.grantedPassives(target);
        String passives = grantedPassives.isEmpty()
                ? "-"
                : grantedPassives.stream().map(ResourceLocation::toString).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
        source.sendSuccess(() -> Component.translatable("command.xlib.passives.list", target.getName(), passives), false);
        return grantedPassives.size();
    }

    static int inspect(CommandSourceStack source, ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        source.sendSuccess(
                () -> Component.literal(target.getGameProfile().getName() + " | passives=" + XLibCommandSupport.joinIds(PassiveGrantApi.grantedPassives(target))),
                false
        );
        Collection<String> stateLines = XLibCommandSupport.buildPassiveStateLines(target, data);
        if (stateLines.isEmpty()) {
            source.sendSuccess(() -> Component.literal("passive_states=-"), false);
            return 1;
        }
        for (String line : stateLines) {
            source.sendSuccess(() -> Component.literal("passive_state=" + line), false);
        }
        return 1;
    }

    static int sources(CommandSourceStack source, ServerPlayer target, ResourceLocation passiveId) throws CommandSyntaxException {
        XLibCommandSupport.validatePassive(passiveId);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | " + passiveId), false);
        source.sendSuccess(() -> Component.literal("grant_sources=" + XLibCommandSupport.joinIds(ModAttachments.get(target).passiveGrantSourcesFor(passiveId))), false);
        return 1;
    }
}
