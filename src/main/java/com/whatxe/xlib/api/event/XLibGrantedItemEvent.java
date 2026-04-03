package com.whatxe.xlib.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public sealed class XLibGrantedItemEvent extends Event permits XLibGrantedItemEvent.Reclaimed, XLibGrantedItemEvent.Removed {
    public enum Reason {
        STORAGE_POLICY,
        REVOKED,
        MISSING_DEFINITION
    }

    private final ServerPlayer player;
    private final ResourceLocation grantedItemId;
    private final ItemStack stack;
    private final Reason reason;

    protected XLibGrantedItemEvent(ServerPlayer player, ResourceLocation grantedItemId, ItemStack stack, Reason reason) {
        this.player = player;
        this.grantedItemId = grantedItemId;
        this.stack = stack.copy();
        this.reason = reason;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public ResourceLocation grantedItemId() {
        return this.grantedItemId;
    }

    public ItemStack stack() {
        return this.stack.copy();
    }

    public Reason reason() {
        return this.reason;
    }

    public static final class Reclaimed extends XLibGrantedItemEvent {
        public Reclaimed(ServerPlayer player, ResourceLocation grantedItemId, ItemStack stack, Reason reason) {
            super(player, grantedItemId, stack, reason);
        }
    }

    public static final class Removed extends XLibGrantedItemEvent {
        public Removed(ServerPlayer player, ResourceLocation grantedItemId, ItemStack stack, Reason reason) {
            super(player, grantedItemId, stack, reason);
        }
    }
}
