package com.whatxe.xlib.combat;

import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class CombatReactionApi {
    private CombatReactionApi() {}

    public static void bootstrap() {}

    public static CombatReactionData get(LivingEntity entity) {
        return ModAttachments.getReaction(entity);
    }

    public static void set(LivingEntity entity, CombatReactionData data) {
        ModAttachments.setReaction(entity, data);
    }

    public static void clear(LivingEntity entity) {
        set(entity, CombatReactionData.empty());
    }

    public static void recordIncomingHit(LivingEntity target, @Nullable LivingEntity attacker, float damage) {
        if (damage <= 0.0F) {
            return;
        }
        set(target, get(target).withRecentHit(
                target.level().getGameTime(),
                attacker != null ? attacker.getUUID() : null,
                damage
        ));
    }

    public static boolean recentlyHurtWithin(LivingEntity entity, int ticks) {
        return get(entity).recentHitWithin(entity.level().getGameTime(), ticks);
    }

    public static boolean recentHitWithin(LivingEntity entity, int ticks) {
        return recentlyHurtWithin(entity, ticks);
    }

    public static float lastDamage(LivingEntity entity) {
        return get(entity).lastDamage();
    }

    public static Optional<LivingEntity> lastAttacker(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        if (get(entity).lastAttacker() == null) {
            return Optional.empty();
        }
        Entity attacker = serverLevel.getEntity(get(entity).lastAttacker());
        return attacker instanceof LivingEntity living ? Optional.of(living) : Optional.empty();
    }
}
