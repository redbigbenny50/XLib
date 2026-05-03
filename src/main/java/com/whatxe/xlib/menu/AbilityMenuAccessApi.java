package com.whatxe.xlib.menu;

import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class AbilityMenuAccessApi {
    private static final Map<ResourceLocation, AbilityMenuAccessPolicy> POLICIES = new LinkedHashMap<>();

    private AbilityMenuAccessApi() {}

    public static void registerPolicy(ResourceLocation sourceId, AbilityMenuAccessPolicy policy) {
        POLICIES.put(Objects.requireNonNull(sourceId, "sourceId"), Objects.requireNonNull(policy, "policy"));
    }

    public static void clearPolicy(ResourceLocation sourceId) {
        POLICIES.remove(Objects.requireNonNull(sourceId, "sourceId"));
    }

    public static void clearPolicies() {
        POLICIES.clear();
    }

    public static Map<ResourceLocation, AbilityMenuAccessPolicy> policies() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(POLICIES));
    }

    public static MenuAccessDecision decision(@Nullable Player player) {
        return decision(player, player == null ? AbilityData.empty() : ModAttachments.get(player));
    }

    public static MenuAccessDecision decision(@Nullable Player player, AbilityData data) {
        MenuAccessDecision lockedDecision = null;
        for (AbilityMenuAccessPolicy policy : POLICIES.values()) {
            MenuAccessDecision decision = policy.evaluate(player, data);
            if (decision.isHidden()) {
                return decision;
            }
            if (lockedDecision == null && decision.isLocked()) {
                lockedDecision = decision;
            }
        }
        return lockedDecision != null ? lockedDecision : MenuAccessDecision.available();
    }
}
