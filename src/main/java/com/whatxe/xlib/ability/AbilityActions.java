package com.whatxe.xlib.ability;

import com.whatxe.xlib.combat.CombatGeometry;
import com.whatxe.xlib.combat.CombatHitKind;
import com.whatxe.xlib.combat.CombatHitResolution;
import com.whatxe.xlib.combat.CombatReactionApi;
import com.whatxe.xlib.combat.CombatTargetingApi;
import com.whatxe.xlib.combat.CombatTargetingProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class AbilityActions {
    public enum MissBehavior {
        FAIL,
        CONSUME
    }

    private AbilityActions() {}

    public static AbilityDefinition.AbilityAction meleeStrike(
            ResourceLocation abilityId,
            CombatTargetingProfile profile,
            float damage
    ) {
        return meleeStrike(abilityId, profile, damage, MissBehavior.CONSUME);
    }

    public static AbilityDefinition.AbilityAction meleeStrike(
            ResourceLocation abilityId,
            CombatTargetingProfile profile,
            float damage,
            MissBehavior missBehavior
    ) {
        return (player, data) -> applyResolvedHit(
                data,
                CombatTargetingApi.resolveSingleTarget(player, abilityId, profile),
                missBehavior,
                target -> {
                    if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                        AbilityCombatTracker.recordAbilityHit(player, target, abilityId);
                    }
                }
        );
    }

    public static AbilityDefinition.AbilityAction dashBehindAndStrike(
            ResourceLocation abilityId,
            CombatTargetingProfile profile,
            double behindDistance,
            float damage
    ) {
        return dashBehindAndStrike(abilityId, profile, behindDistance, damage, MissBehavior.CONSUME);
    }

    public static AbilityDefinition.AbilityAction dashBehindAndStrike(
            ResourceLocation abilityId,
            CombatTargetingProfile profile,
            double behindDistance,
            float damage,
            MissBehavior missBehavior
    ) {
        return (player, data) -> applyResolvedHit(
                data,
                CombatTargetingApi.resolveSingleTarget(player, abilityId, profile),
                missBehavior,
                target -> {
                    CombatGeometry.teleportBehind(player, target, behindDistance);
                    if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                        AbilityCombatTracker.recordAbilityHit(player, target, abilityId);
                    }
                }
        );
    }

    public static AbilityDefinition.AbilityAction launchTarget(
            ResourceLocation abilityId,
            CombatTargetingProfile profile,
            float damage,
            double verticalBoost
    ) {
        return launchTarget(abilityId, profile, damage, verticalBoost, MissBehavior.CONSUME);
    }

    public static AbilityDefinition.AbilityAction launchTarget(
            ResourceLocation abilityId,
            CombatTargetingProfile profile,
            float damage,
            double verticalBoost,
            MissBehavior missBehavior
    ) {
        return (player, data) -> applyResolvedHit(
                data,
                CombatTargetingApi.resolveSingleTarget(player, abilityId, profile),
                missBehavior,
                target -> {
                    if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                        AbilityCombatTracker.recordAbilityHit(player, target, abilityId);
                    }
                    CombatGeometry.launch(target, verticalBoost);
                }
        );
    }

    public static AbilityDefinition.AbilityAction counterStrike(
            ResourceLocation abilityId,
            int recentHitWindowTicks,
            float minimumDamage,
            double reflectedDamageMultiplier
    ) {
        return counterStrike(abilityId, recentHitWindowTicks, minimumDamage, reflectedDamageMultiplier, MissBehavior.FAIL);
    }

    public static AbilityDefinition.AbilityAction counterStrike(
            ResourceLocation abilityId,
            int recentHitWindowTicks,
            float minimumDamage,
            double reflectedDamageMultiplier,
            MissBehavior missBehavior
    ) {
        return (player, data) -> {
            if (!CombatReactionApi.recentlyHurtWithin(player, recentHitWindowTicks)) {
                return AbilityUseResult.fail(
                        data,
                        Component.translatable("message.xlib.combat.counter_window", recentHitWindowTicks)
                );
            }

            LivingEntity attacker = CombatReactionApi.lastAttacker(player).orElse(null);
            if (attacker == null || !attacker.isAlive()) {
                return AbilityUseResult.fail(
                        data,
                        Component.translatable("message.xlib.combat.counter_window", recentHitWindowTicks)
                );
            }

            float damage = Math.max(minimumDamage, (float) (CombatReactionApi.lastDamage(player) * reflectedDamageMultiplier));
            return applyResolvedHit(
                    data,
                    CombatTargetingApi.resolveFixedTarget(player, abilityId, attacker),
                    missBehavior,
                    target -> {
                        if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                            AbilityCombatTracker.recordAbilityHit(player, target, abilityId);
                        }
                    }
            );
        };
    }

    private static AbilityUseResult applyResolvedHit(
            AbilityData data,
            CombatHitResolution resolution,
            MissBehavior missBehavior,
            HitEffect effect
    ) {
        if (!resolution.landed()) {
            return switch (missBehavior) {
                case FAIL -> AbilityUseResult.fail(data, feedback(resolution.kind()));
                case CONSUME -> AbilityUseResult.success(data, feedback(resolution.kind()));
            };
        }

        effect.apply(resolution.target());
        return AbilityUseResult.success(data);
    }

    private static Component feedback(CombatHitKind kind) {
        return switch (kind) {
            case HIT -> Component.empty();
            case MISS -> Component.translatable("message.xlib.combat_result_miss");
            case DODGED -> Component.translatable("message.xlib.combat_result_dodged");
            case BLOCKED -> Component.translatable("message.xlib.combat_result_blocked");
            case PARRIED -> Component.translatable("message.xlib.combat_result_parried");
        };
    }

    @FunctionalInterface
    private interface HitEffect {
        void apply(LivingEntity target);
    }
}
