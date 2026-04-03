package com.whatxe.xlib.api.event;

import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import com.whatxe.xlib.progression.UpgradeRewardBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public sealed class XLibUpgradeRewardProjectionEvent extends Event
        permits XLibUpgradeRewardProjectionEvent.Projected, XLibUpgradeRewardProjectionEvent.Cleared {
    private final ServerPlayer player;
    private final UpgradeNodeDefinition node;
    private final ResourceLocation sourceId;
    private final UpgradeRewardBundle rewards;

    protected XLibUpgradeRewardProjectionEvent(
            ServerPlayer player,
            UpgradeNodeDefinition node,
            ResourceLocation sourceId,
            UpgradeRewardBundle rewards
    ) {
        this.player = player;
        this.node = node;
        this.sourceId = sourceId;
        this.rewards = rewards;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public UpgradeNodeDefinition node() {
        return this.node;
    }

    public ResourceLocation sourceId() {
        return this.sourceId;
    }

    public UpgradeRewardBundle rewards() {
        return this.rewards;
    }

    public static final class Projected extends XLibUpgradeRewardProjectionEvent {
        public Projected(ServerPlayer player, UpgradeNodeDefinition node, ResourceLocation sourceId, UpgradeRewardBundle rewards) {
            super(player, node, sourceId, rewards);
        }
    }

    public static final class Cleared extends XLibUpgradeRewardProjectionEvent {
        public Cleared(ServerPlayer player, UpgradeNodeDefinition node, ResourceLocation sourceId, UpgradeRewardBundle rewards) {
            super(player, node, sourceId, rewards);
        }
    }
}
