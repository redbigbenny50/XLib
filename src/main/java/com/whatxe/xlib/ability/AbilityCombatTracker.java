package com.whatxe.xlib.ability;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class AbilityCombatTracker {
    private static final int DEFAULT_TTL_TICKS = 100;
    private static final Map<UUID, RecentAbilityHit> RECENT_HITS = new ConcurrentHashMap<>();

    private AbilityCombatTracker() {}

    public static void recordAbilityHit(ServerPlayer player, LivingEntity target, ResourceLocation abilityId) {
        recordAbilityHit(player, target, abilityId, DEFAULT_TTL_TICKS);
    }

    public static void recordAbilityHit(ServerPlayer player, LivingEntity target, ResourceLocation abilityId, int ttlTicks) {
        if (ttlTicks <= 0) {
            return;
        }

        RECENT_HITS.put(
                target.getUUID(),
                new RecentAbilityHit(player.getUUID(), abilityId, target.level().getGameTime() + ttlTicks)
        );
    }

    public static Optional<ResourceLocation> recentKillingAbility(ServerPlayer player, LivingEntity target) {
        RecentAbilityHit hit = RECENT_HITS.get(target.getUUID());
        if (hit == null) {
            return Optional.empty();
        }
        if (!hit.playerId().equals(player.getUUID()) || hit.expiresAtGameTime() < target.level().getGameTime()) {
            RECENT_HITS.remove(target.getUUID(), hit);
            return Optional.empty();
        }
        return Optional.of(hit.abilityId());
    }

    public static void clearTarget(LivingEntity target) {
        RECENT_HITS.remove(target.getUUID());
    }

    private record RecentAbilityHit(UUID playerId, ResourceLocation abilityId, long expiresAtGameTime) {}
}
