package com.whatxe.xlib.ability;

import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class AbilityDetectorApi {
    private static final Map<ResourceLocation, AbilityDetectorDefinition> DETECTORS = new LinkedHashMap<>();

    private AbilityDetectorApi() {}

    public static void bootstrap() {}

    public static AbilityDetectorDefinition registerDetector(AbilityDetectorDefinition detector) {
        XLibRegistryGuard.ensureMutable("ability_detectors");
        AbilityDetectorDefinition previous = DETECTORS.putIfAbsent(detector.id(), detector);
        if (previous != null) {
            throw new IllegalStateException("Duplicate detector registration: " + detector.id());
        }
        return detector;
    }

    public static Optional<AbilityDetectorDefinition> unregisterDetector(ResourceLocation detectorId) {
        XLibRegistryGuard.ensureMutable("ability_detectors");
        return Optional.ofNullable(DETECTORS.remove(detectorId));
    }

    public static Optional<AbilityDetectorDefinition> findDetector(ResourceLocation detectorId) {
        return Optional.ofNullable(DETECTORS.get(detectorId));
    }

    public static Collection<AbilityDetectorDefinition> allDetectors() {
        return List.copyOf(DETECTORS.values());
    }

    public static Set<ResourceLocation> activeDetectors(AbilityData data) {
        return data.activeDetectors().stream()
                .filter(DETECTORS::containsKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean hasActiveDetector(AbilityData data, ResourceLocation detectorId) {
        return data.detectorWindowFor(detectorId) > 0 && DETECTORS.containsKey(detectorId);
    }

    public static AbilityData openWindow(AbilityData data, ResourceLocation detectorId) {
        AbilityDetectorDefinition detector = DETECTORS.get(detectorId);
        return detector == null ? data : data.withDetectorWindow(detectorId, detector.durationTicks());
    }

    public static AbilityData tick(AbilityData data) {
        return data.tickDetectorWindows();
    }

    public static AbilityData dispatch(@Nullable ServerPlayer player, AbilityData data, ReactiveRuntimeEvent event) {
        AbilityData updatedData = data;
        for (AbilityDetectorDefinition detector : DETECTORS.values()) {
            if (detector.matches(player, updatedData, event)) {
                updatedData = updatedData.withDetectorWindow(detector.id(), detector.durationTicks());
            }
        }
        return updatedData;
    }
}
