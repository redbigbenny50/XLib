package com.whatxe.xlib.combat;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class CombatReactionData {
    private static final CombatReactionData EMPTY = new CombatReactionData(-1L, null, 0.0F);

    private final long lastHitGameTime;
    private final @Nullable UUID lastAttacker;
    private final float lastDamage;

    private CombatReactionData(long lastHitGameTime, @Nullable UUID lastAttacker, float lastDamage) {
        this.lastHitGameTime = lastHitGameTime;
        this.lastAttacker = lastAttacker;
        this.lastDamage = lastDamage;
    }

    public static CombatReactionData empty() {
        return EMPTY;
    }

    public long lastHitGameTime() {
        return this.lastHitGameTime;
    }

    public @Nullable UUID lastAttacker() {
        return this.lastAttacker;
    }

    public float lastDamage() {
        return this.lastDamage;
    }

    public CombatReactionData withRecentHit(long gameTime, @Nullable UUID attackerId, float damage) {
        return new CombatReactionData(gameTime, attackerId, Math.max(0.0F, damage));
    }

    public boolean recentHitWithin(long currentGameTime, int ticks) {
        return ticks >= 0
                && this.lastHitGameTime >= 0L
                && currentGameTime >= this.lastHitGameTime
                && currentGameTime - this.lastHitGameTime <= ticks;
    }

    public boolean isEmpty() {
        return this.lastHitGameTime < 0L && this.lastAttacker == null && this.lastDamage <= 0.0F;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CombatReactionData that)) {
            return false;
        }
        return this.lastHitGameTime == that.lastHitGameTime
                && Float.compare(this.lastDamage, that.lastDamage) == 0
                && Objects.equals(this.lastAttacker, that.lastAttacker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lastHitGameTime, this.lastAttacker, this.lastDamage);
    }
}
