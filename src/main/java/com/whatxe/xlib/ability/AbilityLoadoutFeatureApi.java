package com.whatxe.xlib.ability;

import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class AbilityLoadoutFeatureApi {
    private static final Map<ResourceLocation, AbilityLoadoutFeature> FEATURES = new LinkedHashMap<>();

    private AbilityLoadoutFeatureApi() {}

    public static void bootstrap() {}

    public static void registerFeature(ResourceLocation sourceId, AbilityLoadoutFeature feature) {
        FEATURES.put(Objects.requireNonNull(sourceId, "sourceId"), Objects.requireNonNull(feature, "feature"));
    }

    public static void clearFeature(ResourceLocation sourceId) {
        FEATURES.remove(Objects.requireNonNull(sourceId, "sourceId"));
    }

    public static void clearFeatures() {
        FEATURES.clear();
    }

    public static Map<ResourceLocation, AbilityLoadoutFeature> features() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(FEATURES));
    }

    public static boolean shouldRegisterQuickSwitchKeybind() {
        return FEATURES.values().stream().anyMatch(AbilityLoadoutFeature::exposesQuickSwitchKeybind);
    }

    public static AbilityLoadoutFeatureDecision decision(@Nullable Player player) {
        return decision(player, player == null ? AbilityData.empty() : ModAttachments.get(player));
    }

    public static AbilityLoadoutFeatureDecision decision(@Nullable Player player, AbilityData data) {
        AbilityLoadoutFeatureDecision mergedDecision = AbilityLoadoutFeatureDecision.disabled();
        for (AbilityLoadoutFeature feature : FEATURES.values()) {
            AbilityLoadoutFeatureDecision decision = feature.policy().evaluate(player, data);
            if (decision == null) {
                continue;
            }
            AbilityLoadoutFeatureDecision sanitizedDecision = new AbilityLoadoutFeatureDecision(
                    decision.managementEnabled(),
                    feature.exposesQuickSwitchKeybind() && decision.quickSwitchEnabled()
            );
            mergedDecision = mergedDecision.merge(sanitizedDecision);
        }
        return mergedDecision;
    }
}
