package com.whatxe.xlib.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public final class XLibIncomingDamageEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final DamageSource source;
    private final @Nullable LivingEntity attacker;
    private float amount;

    public XLibIncomingDamageEvent(ServerPlayer player, DamageSource source, @Nullable LivingEntity attacker, float amount) {
        this.player = player;
        this.source = source;
        this.attacker = attacker;
        this.amount = amount;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public DamageSource source() {
        return this.source;
    }

    public @Nullable LivingEntity attacker() {
        return this.attacker;
    }

    public float amount() {
        return this.amount;
    }

    public void setAmount(float amount) {
        this.amount = Math.max(0.0F, amount);
    }
}
