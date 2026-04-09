package com.whatxe.xlib.menu;

import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.progression.UpgradeProgressData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class ProgressionMenuAccessApi {
    private static final Map<ResourceLocation, ProgressionMenuAccessPolicy> POLICIES = new LinkedHashMap<>();

    private ProgressionMenuAccessApi() {}

    public static void registerPolicy(ResourceLocation sourceId, ProgressionMenuAccessPolicy policy) {
        POLICIES.put(Objects.requireNonNull(sourceId, "sourceId"), Objects.requireNonNull(policy, "policy"));
    }

    public static void clearPolicy(ResourceLocation sourceId) {
        POLICIES.remove(Objects.requireNonNull(sourceId, "sourceId"));
    }

    public static void clearPolicies() {
        POLICIES.clear();
    }

    public static Map<ResourceLocation, ProgressionMenuAccessPolicy> policies() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(POLICIES));
    }

    public static MenuAccessDecision decision(@Nullable Player player) {
        return decision(player, player == null ? UpgradeProgressData.empty() : ModAttachments.getProgression(player));
    }

    public static MenuAccessDecision decision(@Nullable Player player, UpgradeProgressData data) {
        MenuAccessDecision lockedDecision = null;
        for (ProgressionMenuAccessPolicy policy : POLICIES.values()) {
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
