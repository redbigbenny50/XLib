package com.whatxe.xlib.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ReactiveTriggerApiTest {
    private static final ResourceLocation DETECTOR_ID = id("detector/counter_window");
    private static final ResourceLocation TRIGGER_ID = id("trigger/counter");
    private static final ResourceLocation FOLLOWUP_ID = id("ability/followup");

    @Test
    void triggersCanConsumeDetectorWindowsAndApplyResponses() {
        unregisterFixtures();
        try {
            AbilityDetectorApi.registerDetector(AbilityDetectorDefinition.builder(DETECTOR_ID, 5)
                    .event(ReactiveEventType.HURT)
                    .build());
            ReactiveTriggerApi.registerTrigger(ReactiveTriggerDefinition.builder(TRIGGER_ID)
                    .event(ReactiveEventType.HURT)
                    .requireDetector(DETECTOR_ID)
                    .consumeRequiredDetectors()
                    .action((player, data, event) -> data.withComboWindow(FOLLOWUP_ID, 6))
                    .build());

            AbilityData data = ReactiveTriggerApi.dispatch(null, AbilityData.empty(), ReactiveRuntimeEvent.hurt());

            assertEquals(6, data.comboWindowFor(FOLLOWUP_ID));
            assertEquals(0, data.detectorWindowFor(DETECTOR_ID));
            assertEquals(Set.of(FOLLOWUP_ID), data.comboWindows().keySet());
        } finally {
            unregisterFixtures();
        }
    }

    private static void unregisterFixtures() {
        ReactiveTriggerApi.unregisterTrigger(TRIGGER_ID);
        AbilityDetectorApi.unregisterDetector(DETECTOR_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }
}
