package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class AbilityDetectorApiTest {
    private static final ResourceLocation DETECTOR_ID = id("detector/parry");
    private static final ResourceLocation STALE_DETECTOR_ID = id("detector/stale");

    @Test
    void matchingEventsOpenAndTickDetectorWindows() {
        unregisterFixtures();
        try {
            AbilityDetectorApi.registerDetector(AbilityDetectorDefinition.builder(DETECTOR_ID, 3)
                    .event(ReactiveEventType.HURT)
                    .build());

            AbilityData data = ReactiveTriggerApi.dispatch(null, AbilityData.empty(), ReactiveRuntimeEvent.hurt());

            assertEquals(3, data.detectorWindowFor(DETECTOR_ID));
            assertTrue(AbilityRequirements.detectorActive(DETECTOR_ID).validate(null, data).isEmpty());

            AbilityData ticked = AbilityDetectorApi.tick(data);

            assertEquals(2, ticked.detectorWindowFor(DETECTOR_ID));
            assertEquals(Set.of(DETECTOR_ID), AbilityDetectorApi.activeDetectors(ticked));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void sanitizeRemovesDetectorWindowsForUnknownRegistrations() {
        unregisterFixtures();
        try {
            AbilityDetectorApi.registerDetector(AbilityDetectorDefinition.builder(DETECTOR_ID, 4)
                    .event(ReactiveEventType.JUMP)
                    .build());

            AbilityData data = AbilityData.empty()
                    .withDetectorWindow(DETECTOR_ID, 4)
                    .withDetectorWindow(STALE_DETECTOR_ID, 2);

            AbilityData sanitized = AbilityApi.sanitizeData(data);

            assertEquals(4, sanitized.detectorWindowFor(DETECTOR_ID));
            assertFalse(sanitized.activeDetectors().contains(STALE_DETECTOR_ID));
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        AbilityDetectorApi.unregisterDetector(DETECTOR_ID);
        AbilityDetectorApi.unregisterDetector(STALE_DETECTOR_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
