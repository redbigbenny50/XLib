package com.whatxe.xlib.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileDefinition;
import com.whatxe.xlib.ability.ProfileGroupDefinition;
import com.whatxe.xlib.ability.ProfileOnboardingTrigger;
import com.whatxe.xlib.ability.ProfileSelectionOrigin;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class ProfileAdminCommands {
    private ProfileAdminCommands() {}

    static int catalog(CommandSourceStack source) {
        Collection<ProfileDefinition> profiles = ProfileApi.allProfiles();
        source.sendSuccess(() -> Component.literal("registered_profiles=" + profiles.size()), false);
        for (ProfileDefinition profile : profiles.stream().sorted(java.util.Comparator.comparing(definition -> definition.id().toString())).toList()) {
            source.sendSuccess(() -> Component.literal(profile.id()
                    + " | group=" + profile.groupId()
                    + " | display=" + profile.displayName().getString()
                    + " | conflicts=" + XLibCommandSupport.joinIds(profile.incompatibleProfiles())), false);
        }
        return profiles.size();
    }

    static int groups(CommandSourceStack source) {
        Collection<ProfileGroupDefinition> groups = ProfileApi.allGroups();
        source.sendSuccess(() -> Component.literal("registered_profile_groups=" + groups.size()), false);
        for (ProfileGroupDefinition group : groups.stream().sorted(java.util.Comparator.comparing(definition -> definition.id().toString())).toList()) {
            source.sendSuccess(() -> Component.literal(group.id()
                    + " | display=" + group.displayName().getString()
                    + " | limit=" + group.selectionLimit()
                    + " | required=" + group.requiredOnboarding()
                    + " | triggers=" + group.onboardingTriggers().stream()
                    .map(trigger -> trigger.name().toLowerCase(Locale.ROOT))
                    .sorted()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("-")), false);
        }
        return groups.size();
    }

    static int claim(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation profileId) throws CommandSyntaxException {
        XLibCommandSupport.validateProfile(profileId);
        for (ServerPlayer player : targets) {
            ProfileApi.claimProfile(player, profileId, ProfileSelectionOrigin.ADMIN, false, "admin command")
                    .ifPresent(failure -> player.displayClientMessage(failure, true));
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.profiles.claim", profileId.toString(), targets.size()), true);
        return targets.size();
    }

    static int reset(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation groupId) throws CommandSyntaxException {
        XLibCommandSupport.validateProfileGroup(groupId);
        for (ServerPlayer player : targets) {
            ProfileApi.resetGroup(player, groupId, true, "admin command")
                    .ifPresent(failure -> player.displayClientMessage(failure, true));
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.profiles.reset", groupId.toString(), targets.size()), true);
        return targets.size();
    }

    static int reopen(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation groupId) throws CommandSyntaxException {
        XLibCommandSupport.validateProfileGroup(groupId);
        for (ServerPlayer player : targets) {
            ProfileApi.markPendingGroup(player, groupId, ProfileOnboardingTrigger.COMMAND, "admin command");
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.profiles.reopen", groupId.toString(), targets.size()), true);
        return targets.size();
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName()
                + " | selected_profiles=" + XLibCommandSupport.joinIds(ProfileApi.get(target).selectedProfileIds())
                + " | pending_profile_groups=" + XLibCommandSupport.joinIds(ProfileApi.get(target).pendingGroupIds())), false);
        return 1;
    }

    static int pending(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName()
                + " | pending_profile_groups=" + XLibCommandSupport.joinIds(ProfileApi.get(target).pendingGroupIds())), false);
        return 1;
    }

    static int resync(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            ProfileApi.rebuild(player);
        }
        source.sendSuccess(() -> Component.translatable("command.xlib.profiles.resync", targets.size()), true);
        return targets.size();
    }
}
