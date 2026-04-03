package com.whatxe.xlib.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public final class XLibOutgoingDamageEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final LivingEntity target;
    private final DamageSource source;
    private float amount;

    public XLibOutgoingDamageEvent(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
        this.player = player;
        this.target = target;
        this.source = source;
        this.amount = amount;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public LivingEntity target() {
        return this.target;
    }

    public DamageSource source() {
        return this.source;
    }

    public float amount() {
        return this.amount;
    }

    public void setAmount(float amount) {
        this.amount = Math.max(0.0F, amount);
    }
}
