package com.whatxe.xlib.combat;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record CombatHitResolution(CombatHitKind kind, @Nullable LivingEntity target) {
    public static CombatHitResolution miss() {
        return new CombatHitResolution(CombatHitKind.MISS, null);
    }

    public boolean landed() {
        return this.kind == CombatHitKind.HIT && this.target != null;
    }
}
