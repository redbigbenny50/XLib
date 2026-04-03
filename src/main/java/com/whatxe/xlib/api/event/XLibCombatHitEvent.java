package com.whatxe.xlib.api.event;

import com.whatxe.xlib.combat.CombatHitKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

public final class XLibCombatHitEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation abilityId;
    private final LivingEntity target;
    private CombatHitKind kind;

    public XLibCombatHitEvent(ServerPlayer player, ResourceLocation abilityId, LivingEntity target, CombatHitKind kind) {
        this.player = player;
        this.abilityId = abilityId;
        this.target = target;
        this.kind = kind;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public ResourceLocation abilityId() {
        return this.abilityId;
    }

    public LivingEntity target() {
        return this.target;
    }

    public CombatHitKind kind() {
        return this.kind;
    }

    public void setKind(CombatHitKind kind) {
        this.kind = kind;
    }
}
