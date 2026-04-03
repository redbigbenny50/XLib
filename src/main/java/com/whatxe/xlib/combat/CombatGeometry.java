package com.whatxe.xlib.combat;

import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

public final class CombatGeometry {
    private CombatGeometry() {}

    public static List<LivingEntity> candidateTargets(ServerPlayer player, double range, double verticalRange) {
        AABB searchBox = player.getBoundingBox().inflate(range, verticalRange, range);
        return player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                target -> target.isAlive() && target != player && !target.isSpectator()
        ).stream().sorted(Comparator.comparingDouble(player::distanceToSqr)).toList();
    }

    public static boolean isWithinRange(ServerPlayer player, LivingEntity target, double range) {
        return player.distanceToSqr(target) <= range * range;
    }

    public static boolean isWithinCone(ServerPlayer player, LivingEntity target, double range, double angleDegrees) {
        if (!isWithinRange(player, target, range)) {
            return false;
        }
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(player.getEyePosition())
                .normalize();
        double threshold = Math.cos(Math.toRadians(angleDegrees) / 2.0D);
        return look.dot(toTarget) >= threshold;
    }

    public static Vec3 positionBehind(LivingEntity target, double distance) {
        Vec3 offset = target.getLookAngle().normalize().scale(distance);
        return target.position().subtract(offset);
    }

    public static void teleportBehind(ServerPlayer player, LivingEntity target, double distance) {
        Vec3 behind = positionBehind(target, distance);
        player.teleportTo(behind.x, behind.y, behind.z);
    }

    public static void applyKnockback(LivingEntity target, Vec3 origin, double horizontalStrength, double verticalBoost) {
        Vec3 horizontal = target.position().subtract(origin);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 velocity = horizontal.normalize().scale(horizontalStrength).add(0.0D, verticalBoost, 0.0D);
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
    }

    public static void launch(LivingEntity target, double verticalBoost) {
        Vec3 velocity = target.getDeltaMovement().add(0.0D, verticalBoost, 0.0D);
        target.setDeltaMovement(velocity);
        target.hurtMarked = true;
    }
}
