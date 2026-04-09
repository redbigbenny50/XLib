package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class ReactiveTriggerApi {
    private static final Map<ResourceLocation, ReactiveTriggerDefinition> TRIGGERS = new LinkedHashMap<>();

    private ReactiveTriggerApi() {}

    public static void bootstrap() {}

    public static ReactiveTriggerDefinition registerTrigger(ReactiveTriggerDefinition trigger) {
        XLibRegistryGuard.ensureMutable("reactive_triggers");
        ReactiveTriggerDefinition previous = TRIGGERS.putIfAbsent(trigger.id(), trigger);
        if (previous != null) {
            throw new IllegalStateException("Duplicate reactive trigger registration: " + trigger.id());
        }
        return trigger;
    }

    public static Optional<ReactiveTriggerDefinition> unregisterTrigger(ResourceLocation triggerId) {
        XLibRegistryGuard.ensureMutable("reactive_triggers");
        return Optional.ofNullable(TRIGGERS.remove(triggerId));
    }

    public static Optional<ReactiveTriggerDefinition> findTrigger(ResourceLocation triggerId) {
        return Optional.ofNullable(TRIGGERS.get(triggerId));
    }

    public static Collection<ReactiveTriggerDefinition> allTriggers() {
        return List.copyOf(TRIGGERS.values());
    }

    public static AbilityData dispatch(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event) {
        AbilityData updatedData = AbilityDetectorApi.dispatch(player, data, event);
        for (ReactiveTriggerDefinition trigger : TRIGGERS.values()) {
            if (!trigger.matches(player, updatedData, event)) {
                continue;
            }
            updatedData = trigger.action().apply(player, updatedData, event);
            for (ResourceLocation detectorId : trigger.consumedDetectors()) {
                updatedData = updatedData.withDetectorWindow(detectorId, 0);
            }
        }
        return updatedData;
    }
}
