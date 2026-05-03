package com.whatxe.xlib.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeApi;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class AbilityAdminCommands {
    private AbilityAdminCommands() {}

    static int grant(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation abilityId) throws CommandSyntaxException {
        XLibCommandSupport.validateAbility(abilityId);
        for (ServerPlayer player : targets) {
            AbilityGrantApi.grant(player, abilityId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.abilities.grant", abilityId.toString(), targets.size()), true);
        return targets.size();
    }

    static int revoke(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation abilityId) throws CommandSyntaxException {
        XLibCommandSupport.validateAbility(abilityId);
        for (ServerPlayer player : targets) {
            UpgradeApi.revokeNodesGrantingAbility(player, abilityId);
            AbilityGrantApi.revoke(player, abilityId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.abilities.revoke", abilityId.toString(), targets.size()), true);
        return targets.size();
    }

    static int grantOnly(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation abilityId) throws CommandSyntaxException {
        XLibCommandSupport.validateAbility(abilityId);
        for (ServerPlayer player : targets) {
            AbilityGrantApi.grantOnly(player, abilityId);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.abilities.grant_only", abilityId.toString(), targets.size()), true);
        return targets.size();
    }

    static int restrict(CommandSourceStack source, Collection<ServerPlayer> targets, boolean enabled) {
        for (ServerPlayer player : targets) {
            AbilityGrantApi.setRestricted(player, enabled);
        }
        source.sendSuccess(
                () -> Component.translatable(enabled ? "command.xlib.abilities.restrict_on" : "command.xlib.abilities.restrict_off", targets.size()),
                true
        );
        return targets.size();
    }

    static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            AbilityGrantApi.clearGrants(player);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.abilities.clear", targets.size()), true);
        return targets.size();
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        Set<ResourceLocation> grantedAbilities = AbilityGrantApi.grantedAbilities(target);
        String abilities = grantedAbilities.isEmpty()
                ? "-"
                : grantedAbilities.stream().map(ResourceLocation::toString).sorted().reduce((left, right) -> left + ", " + right).orElse("-");
        source.sendSuccess(
                () -> Component.translatable("command.xlib.abilities.list", target.getName(), AbilityGrantApi.isRestricted(target), abilities),
                false
        );
        return grantedAbilities.size();
    }

    static int inspect(CommandSourceStack source, ServerPlayer target) {
        AbilityData data = ModAttachments.get(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | restricted=" + AbilityGrantApi.isRestricted(target)
                + " | grants=" + XLibCommandSupport.joinIds(AbilityGrantApi.grantedAbilities(target))), false);
        source.sendSuccess(() -> Component.literal("slots=" + XLibCommandSupport.formatSlots(data)), false);
        source.sendSuccess(() -> Component.literal("resolved_slots=" + XLibCommandSupport.formatResolvedSlots(data)), false);
        source.sendSuccess(() -> Component.literal("active=" + XLibCommandSupport.joinIds(data.activeModes())), false);
        source.sendSuccess(() -> Component.literal("combo_windows=" + XLibCommandSupport.formatComboWindows(data)), false);
        source.sendSuccess(() -> Component.literal("cooldowns=" + XLibCommandSupport.formatCooldowns(data)), false);
        source.sendSuccess(() -> Component.literal("charges=" + XLibCommandSupport.formatCharges(data)), false);
        source.sendSuccess(() -> Component.literal("resources=" + XLibCommandSupport.formatResources(data)), false);
        source.sendSuccess(() -> Component.literal("blocked=" + XLibCommandSupport.joinIds(AbilityGrantApi.blockedAbilities(target))), false);

        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            Optional<ResourceLocation> abilityId = data.abilityInSlot(slot);
            if (abilityId.isEmpty()) {
                continue;
            }
            Optional<AbilityDefinition> ability = AbilityApi.findAbility(abilityId.get());
            if (ability.isEmpty()) {
                continue;
            }
            String view = AbilityGrantApi.firstViewFailure(target, data, ability.get()).map(Component::getString).orElse("ok");
            String assign = AbilityGrantApi.firstAssignmentFailure(target, data, ability.get()).map(Component::getString).orElse("ok");
            String activate = AbilityGrantApi.firstActivationFailure(target, data, ability.get()).map(Component::getString).orElse("ok");
            String statusLine = "slot " + (slot + 1) + " " + abilityId.get() + " | view=" + view + " | assign=" + assign + " | activate=" + activate;
            source.sendSuccess(() -> Component.literal(statusLine), false);
        }
        return 1;
    }

    static int sources(CommandSourceStack source, ServerPlayer target, ResourceLocation abilityId) throws CommandSyntaxException {
        XLibCommandSupport.validateAbility(abilityId);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | " + abilityId), false);
        source.sendSuccess(() -> Component.literal("grant_sources=" + XLibCommandSupport.joinIds(AbilityGrantApi.grantSources(target, abilityId))), false);
        source.sendSuccess(
                () -> Component.literal("activation_block_sources=" + XLibCommandSupport.joinIds(AbilityGrantApi.activationBlockSources(target, abilityId))),
                false
        );
        return 1;
    }
}
