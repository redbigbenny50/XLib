package com.whatxe.xlib.combat;

import com.whatxe.xlib.api.event.XLibCombatHitEvent;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;

public final class CombatTargetingApi {
    private CombatTargetingApi() {}

    public static Optional<LivingEntity> findPrimaryTarget(ServerPlayer player, CombatTargetingProfile profile) {
        return findTargets(player, profile).stream().findFirst();
    }

    public static List<LivingEntity> findTargets(ServerPlayer player, CombatTargetingProfile profile) {
        List<LivingEntity> candidates = CombatGeometry.candidateTargets(player, profile.range(), profile.verticalRange());
        return switch (profile.mode()) {
            case DIRECT -> candidates.stream()
                    .filter(target -> CombatGeometry.isWithinCone(player, target, profile.range(), 20.0D))
                    .filter(target -> !profile.requireLineOfSight() || player.hasLineOfSight(target))
                    .limit(profile.maxTargets())
                    .toList();
            case CONE -> candidates.stream()
                    .filter(target -> CombatGeometry.isWithinCone(player, target, profile.range(), profile.angleDegrees()))
                    .filter(target -> !profile.requireLineOfSight() || player.hasLineOfSight(target))
                    .limit(profile.maxTargets())
                    .toList();
            case RADIUS -> candidates.stream()
                    .filter(target -> CombatGeometry.isWithinRange(player, target, profile.range() + profile.radius()))
                    .filter(target -> player.distanceTo(target) <= profile.radius() || CombatGeometry.isWithinCone(player, target, profile.range(), 180.0D))
                    .filter(target -> !profile.requireLineOfSight() || player.hasLineOfSight(target))
                    .limit(profile.maxTargets())
                    .toList();
        };
    }

    public static CombatHitResolution resolveSingleTarget(
            ServerPlayer player,
            ResourceLocation abilityId,
            CombatTargetingProfile profile
    ) {
        LivingEntity target = findPrimaryTarget(player, profile).orElse(null);
        return resolveFixedTarget(player, abilityId, target);
    }

    public static CombatHitResolution resolveFixedTarget(
            ServerPlayer player,
            ResourceLocation abilityId,
            LivingEntity target
    ) {
        if (target == null) {
            return CombatHitResolution.miss();
        }

        XLibCombatHitEvent event = new XLibCombatHitEvent(player, abilityId, target, CombatHitKind.HIT);
        NeoForge.EVENT_BUS.post(event);
        return new CombatHitResolution(event.kind(), target);
    }
}
