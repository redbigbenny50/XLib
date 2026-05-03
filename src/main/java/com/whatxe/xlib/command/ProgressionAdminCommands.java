package com.whatxe.xlib.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeProgressData;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class ProgressionAdminCommands {
    private ProgressionAdminCommands() {}

    static int unlock(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation nodeId) throws CommandSyntaxException {
        XLibCommandSupport.validateUpgradeNode(nodeId);
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (UpgradeApi.unlockNode(player, nodeId)) {
                changed++;
            }
        }
        int unlockedCount = changed;
        source.sendSuccess(() -> Component.translatable("command.xlib.progression.unlock", nodeId.toString(), unlockedCount, targets.size()), true);
        return changed;
    }

    static int revoke(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation nodeId) throws CommandSyntaxException {
        XLibCommandSupport.validateUpgradeNode(nodeId);
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (UpgradeApi.revokeNode(player, nodeId)) {
                changed++;
            }
        }
        int revokedCount = changed;
        source.sendSuccess(() -> Component.translatable("command.xlib.progression.revoke", nodeId.toString(), revokedCount, targets.size()), true);
        return changed;
    }

    static int revokeTrack(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation trackId) throws CommandSyntaxException {
        XLibCommandSupport.validateUpgradeTrack(trackId);
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (UpgradeApi.revokeTrack(player, trackId)) {
                changed++;
            }
        }
        int revokedCount = changed;
        source.sendSuccess(() -> Component.translatable("command.xlib.progression.track_revoke", trackId.toString(), revokedCount, targets.size()), true);
        return changed;
    }

    static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (UpgradeApi.clearProgress(player)) {
                changed++;
            }
        }
        int clearedCount = changed;
        source.sendSuccess(() -> Component.translatable("command.xlib.progression.clear", clearedCount, targets.size()), true);
        return changed;
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        UpgradeProgressData data = UpgradeApi.get(target);
        source.sendSuccess(
                () -> Component.translatable(
                        "command.xlib.progression.list",
                        target.getName(),
                        XLibCommandSupport.joinIds(data.unlockedNodes()),
                        XLibCommandSupport.formatTrackIds(UpgradeApi.visibleTracks(data))
                ),
                false
        );
        return data.unlockedNodes().size();
    }

    static int inspect(CommandSourceStack source, ServerPlayer target) {
        UpgradeProgressData data = UpgradeApi.get(target);
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " | unlocked_nodes=" + XLibCommandSupport.joinIds(data.unlockedNodes())), false);
        source.sendSuccess(() -> Component.literal("points=" + XLibCommandSupport.formatNumericMap(data.pointBalances())), false);
        source.sendSuccess(() -> Component.literal("counters=" + XLibCommandSupport.formatNumericMap(data.counters())), false);
        source.sendSuccess(() -> Component.literal("visible_tracks=" + XLibCommandSupport.formatTrackIds(UpgradeApi.visibleTracks(data))), false);
        return 1;
    }
}
